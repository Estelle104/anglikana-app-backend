# Hiérarchie ecclésiastique (Diocèse → Région → District → Paroisse → Église)

## Vue d'ensemble

CRUD des cinq niveaux de la hiérarchie et lecture des enfants directs de chaque entité. Chaque niveau
est rattaché à son parent par une clé étrangère obligatoire, et peut avoir un responsable (membre du
clergé, [module-clerge.md](module-clerge.md)). Les limites géographiques (`carte_qgis`) sont saisies
ici, mais leur usage spatial est décrit dans [module-geographie.md](module-geographie.md). Les
comptages sont dans [module-tableau-bord.md](module-tableau-bord.md).

## Structure / composants

**Modèle** (`entity/`, tables de `database/002-table.sql`) :

| Entité | Table | Champs propres | Parent (obligatoire) | Géométrie |
|---|---|---|---|---|
| `Diocese` | `diocese` | `nom`, `presentation` (TEXT), `coordonnees` (texte libre) | — | `carteQgis` Polygon |
| `Region` | `region` | `nom` | `diocese` | `carteQgis` Polygon |
| `District` | `district` | `nom` | `region` | `carteQgis` Polygon |
| `Paroisse` | `paroisse` | `nom` | `district` | `carteQgis` Polygon |
| `Eglise` | `eglise` | `nom`, `adresse`, `historique`, `lienFacebook` | `paroisse` | `localisation` Point |

Toutes ont un champ `responsable` (`@ManyToOne` LAZY vers `Clerge`, facultatif) et un champ `etat`
(`Short`, 1 = actif par défaut, -1 = supprimé, non exposé dans les DTO). En base, la suppression
physique d'un parent se propage encore aux enfants (`ON DELETE CASCADE` sur `diocese_id`,
`region_id`, `district_id` et `paroisse_id`), mais l'API ne fait plus de suppression physique.

**Suppression logique** (sprint 3) :

- chaque entité porte `@SQLRestriction("etat = 1")`. Tous les `findAll`, `findById`, `existsById` et
  requêtes dérivées ignorent les lignes à -1 ;
- `@SQLDelete(sql = "UPDATE <table> SET etat = -1 WHERE id = ?")` fait du `deleteById` une mise à jour ;
- les requêtes natives filtrent `etat = 1` table par table ([module-tableau-bord.md](module-tableau-bord.md),
  [module-geographie.md](module-geographie.md)).

**DTO** : les relations sont exposées par identifiant (`dioceseId`, `regionId`, `districtId`,
`paroisseId`, `responsableId`). `carteQgis` est exposée en GeoJSON (`JsonNode`, produit par
`GeometrieMapper`). `EgliseDTO` expose la localisation sous la forme `latitude` / `longitude` (`Double`).

**Endpoints** : le même squelette pour les cinq contrôleurs.

| Verbe | Route | Accès | Réponses |
|---|---|---|---|
| `GET` | `/api/<niveau>s` | public | `200` liste des lignes actives, non paginée |
| `GET` | `/api/<niveau>s/{id}` | public | `200` / `404` (introuvable ou supprimée) |
| `POST` | `/api/<niveau>s` | diocèse : niveau 5 ; autres : responsable du parent (`<parent>Id` du corps) ou d'un de ses ancêtres | `201` DTO / `400` texte si `carteQgis` est invalide (sauf Église, sans `carteQgis`) / `403` (`<parent>Id` absent compris) / `404` parent introuvable (sauf diocèse) |
| `PUT` | `/api/<niveau>s/{id}` | responsable de l'entité ou d'un de ses ancêtres ; un changement de parent exige aussi d'être responsable du nouveau parent | `200` / `400` (sauf Église) / `403` / `404` entité ou nouveau parent introuvable |
| `DELETE` | `/api/<niveau>s/{id}` | idem (`PUT`) | `204` (passage à `etat = -1`) / `403` / `404` / `409` texte tant qu'une fille directe est active |

Les règles d'accès (annotations, codes `403`/`404`) sont décrites dans
[securite-authentification.md](securite-authentification.md).

**Refus de suppression** (`HierarchieService.refusSuppression(niveau, id)`) : seules les **filles
directes actives** comptent (les petites-filles ne sont pas regardées) :

- diocèse → régions ;
- région → districts ;
- district → paroisses ;
- paroisse → églises ;
- église → ressources.

Requêtes `existsBy<Parent>Id` des repositories enfants. Le message est par exemple « Suppression
impossible : la région contient encore au moins un district actif. »

Routes des enfants (servies par `HierarchieService`, `404` si le parent n'existe pas, tri par nom) :

| Route | Méthode de service | Requête |
|---|---|---|
| `GET /api/dioceses/{id}/regions` | `getRegionsDuDiocese` | `RegionRepository.findByDioceseIdOrderByNomAsc` |
| `GET /api/dioceses/{id}/districts` | `getDistrictsDuDiocese` | `DistrictRepository.findByRegionDioceseIdOrderByNomAsc` (via les régions) |
| `GET /api/regions/{id}/districts` | `getDistrictsDeLaRegion` | `findByRegionIdOrderByNomAsc` |
| `GET /api/districts/{id}/paroisses` | `getParoissesDuDistrict` | `ParoisseRepository.findByDistrictIdOrderByNomAsc` |
| `GET /api/paroisses/{id}/eglises` | `getEglisesDeLaParoisse` | `EgliseRepository.findByParoisseIdOrderByNomAsc` |
| `GET /api/eglises/{id}/ressources` | `getRessourcesDeLEglise` | `RessourceRepository.findByEgliseIdOrderByNomAsc` ([module-ressource.md](module-ressource.md)) |

**Écriture** : chaque contrôleur a une méthode privée `appliquerDTO(entite, dto)`, qui :

- recopie les champs simples ;
- convertit `carteQgis` par `GeometrieMapper.depuisGeoJson`. Cette méthode lève une
  `IllegalArgumentException`, que le contrôleur traduit en `400` avec son message ;
- résout `parentId` et `responsableId` par `findById` ;
- pour une église, construit le `Point` depuis `latitude` / `longitude`, avec la `GeometryFactory`
  SRID 4326 du contrôleur. Le point vaut `null` si l'une des deux coordonnées manque.

**`HierarchieService`** (`@Transactional(readOnly = true)` au niveau de la classe) : les routes
d'enfants ci-dessus, `refusSuppression`, `estResponsableActif` ([module-clerge.md](module-clerge.md)),
`getTypesRessource` et `getTypeRessource` ([module-ressource.md](module-ressource.md)).

**`dto/NiveauHierarchique`** (enum) : `DIOCESE`, `REGION`, `DISTRICT`, `PAROISSE`, `EGLISE`, chacun
avec un code JSON (`diocese`…) et un libellé (`Diocèse`…). Méthodes :

- `depuisCode` : insensible à la casse, renvoie un `Optional` ;
- `inferieurs`, `enfant`, `estSuperieurA` ;
- `estGeographique` : vrai pour diocèse, région et district ;
- `enfantGeographique`.

L'enum sert à la géographie et au tableau de bord.

**Mappers** : `RegionMapper`, `DistrictMapper`, `ParoisseMapper` et `EgliseMapper` (statiques). Pour
le diocèse, la conversion est la méthode privée `DioceseController.toDTO`.

## Décisions techniques et raisons

- **Routes d'enfants dans un service** : le service vérifie l'existence du parent (`existsById`) avant
  de lister. Cela distingue « parent absent » (`404`) de « parent sans enfant » (`200 []`).
- **Districts d'un diocèse via les régions** : un district n'a pas de `diocese_id` ; le chemin est
  `district.region.diocese` (requête dérivée `findByRegionDioceseId…`).
- **JTS : X = longitude, Y = latitude** : `EgliseMapper` le rappelle en commentaire ; la conversion
  inverse est faite dans `EgliseController`.
- **Même règle de géométrie pour le CRUD et l'import QGIS** : `GeometrieMapper` est le point unique de
  validation ([module-geographie.md](module-geographie.md)).
- **Suppression logique plutôt que physique** (sprint 3) : l'historique est conservé. Le refus tant
  qu'une fille directe est active garantit qu'aucune ligne active ne pointe vers un parent supprimé.
  Les associations `@ManyToOne`, que `@SQLRestriction` ne filtre pas, restent donc cohérentes.

## État actuel connu

- Les CRUD des cinq niveaux et les six routes d'enfants sont fonctionnels. Couverture des tests
  ([tests.md](tests.md)) :
  - `Sprint2ControllerTest` : `GET /api/dioceses/{id}/regions` et la saisie de `carteQgis` sur
    `/api/districts` ;
  - `Sprint2PostgisIntegrationTest` (base réelle) : toutes les routes d'enfants sauf
    `getParoissesDuDistrict` ;
  - les CRUD de Diocèse, Région, Paroisse et Église ne sont pas testés.
- La carte d'une paroisse se saisit par le CRUD, mais la paroisse n'est pas un niveau géographique
  pour `/api/geographie` (`estGeographique` = faux).
- Droits et suppression logique testés par `Sprint3ControllerTest` et `Sprint3PostgisIntegrationTest`.
  Cas couverts : paroisse modifiée par le Doyen de son district et refusée hors district ; région qui
  change de diocèse ; refus `409` ; exclusion des lignes à -1.

## Écarts connus avec le code / dette identifiée

- **Parent absent ou inconnu** : depuis le sprint 3, le contrôle d'accès répond avant
  `appliquerDTO`. Un parent absent donne `403`, un parent inconnu ou supprimé `404`, à la création
  comme lors d'un changement de parent. L'ancienne erreur 500 (`NOT NULL` violé) n'est donc plus
  atteinte par l'API, mais `appliquerDTO` met toujours le parent à `null` sans vérifier. Un
  responsable inconnu ou supprimé est mis à `null` sans erreur.
- **Pas de validation de forme** : `nom` est obligatoire en base mais n'est pas vérifié en Java
  (`@Valid` absent).
- **Listes non paginées** : `GET /api/<niveau>s` renvoie toute la table.
- **Aucune restauration** : aucune route ne remet une ligne à `etat = 1`, il faut le faire en SQL.
- `DioceseController` n'a pas de mapper dédié ; deux styles de lecture d'`Optional` coexistent
  ([conventions-code.md §15](conventions-code.md)).

## Dernière mise à jour

2026-11-20 — Sprint 3 : accès par responsable (annotations), suppression logique (`etat`), refus `409` tant qu'une fille directe est active, contrôle du changement de parent.

2026-11-20 — Création du fichier (système documentaire initial).
