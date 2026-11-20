# Géographie (limites QGIS, recherche, églises d'une zone)

## Vue d'ensemble

Exploitation spatiale des limites des zones et de la localisation des églises, avec PostGIS. Seuls
trois niveaux sont **géographiques** : diocèse, région et district
(`NiveauHierarchique.estGeographique`). Ce module couvre :

- l'export des limites d'un niveau en GeoJSON ;
- l'import d'une couche QGIS ;
- la recherche de zones ;
- la fiche d'une zone (polygone, sous-zones, églises situées dedans, églises incohérentes).

La saisie unitaire de `carteQgis` par le CRUD est décrite dans [module-hierarchie.md](module-hierarchie.md).
Les deux chemins partagent la même validation (`GeometrieMapper`).

## Structure / composants

**`GeographieController`** (`/api/geographie`). Le paramètre `{niveau}` vaut `diocese`, `region` ou
`district` ; tout autre code renvoie `400` « Niveau géographique inconnu ».

| Verbe | Route | Accès | Réponses |
|---|---|---|---|
| `GET` | `/recherche?q=&niveau=&limite=50` | public | `200 List<ResultatRechercheDTO>` ; `400` si `limite` hors de [1, 200], `q` de plus de 150 caractères, ou `niveau` inconnu |
| `GET` | `/{niveau}/limites` | public | `200 ZoneFeatureCollectionDTO` (zones dont la limite est importée) |
| `PUT` | `/{niveau}/limites` | niveau 5 (`@LevelRequired(5)`) | `200 ImportLimitesResultatDTO` ; `400` si le corps n'est pas une FeatureCollection ; `403` sans jeton ou sous le niveau 5 |
| `GET` | `/{niveau}/{id}` | public | `200 ZoneGeographiqueDTO` / `404` |
| `GET` | `/{niveau}/{id}/eglises` | public | `200 List<EgliseLocaliseeDTO>` / `404` |

Constantes : `LIMITE_RECHERCHE_MAX = 200`, `LONGUEUR_TERME_MAX = 150`.

**Suppression logique** (sprint 3) : toutes les requêtes de `GeographieRepository` filtrent
`etat = 1` sur chaque table lue ou jointe (limites, zone, sous-zones, églises dans, hors ou rattachées
à la zone, recherche). `majCarteQgis` ne met à jour qu'une zone active : une zone supprimée est
comptée dans `idsInconnus` ([module-hierarchie.md](module-hierarchie.md)).

**`GeographieService`** :

- `getLimites(niveau)` : renvoie la FeatureCollection des zones ayant une `carte_qgis`.
- `rechercher(niveau, terme, limite)` :
  - recherche par nom, insensible à la casse et aux accents, ou par identifiant exact ;
  - `niveau = null` cherche dans les trois niveaux ;
  - chaque résultat porte un `contexte` lisible, par exemple « Région X · Diocèse Y ».
- `getZone(niveau, id)` construit :
  - la `feature` de la zone ;
  - `limiteDisponible` ;
  - les `sousZones` (régions d'un diocèse, districts d'une région, ayant une limite) ;
  - les `eglises` : situées **dans** le polygone si une limite existe, sinon rattachées
    hiérarchiquement ;
  - les `eglisesHorsZone` : rattachées hiérarchiquement mais situées hors du polygone.
- `getEglisesDeLaZone` : même règle que `eglises`.
- `importerLimites(niveau, request)` (`@Transactional`), pour chaque feature :
  1. lit `properties.id` (nombre ou texte numérique) ;
  2. valide la géométrie par `GeometrieMapper.depuisGeoJson` ;
  3. met à jour la zone (`majCarteQgis`).

  Résultat : `zonesMisesAJour`, `idsInconnus` (identifiants absents de la base) et `erreurs` (une
  ligne par feature refusée, sans bloquer les autres).

**`GeographieRepository`** (SQL natif, `EntityManager`) :

| Méthode | SQL clé |
|---|---|
| `limites`, `zone`, `sousZones` | `ST_AsGeoJSON(carte_qgis, 6)` |
| `eglisesDansZone` | `JOIN eglise e ON ST_Covers(z.carte_qgis, e.localisation)` |
| `eglisesHorsZone` | jointures hiérarchiques + `NOT ST_Covers(...)` |
| `eglisesRattachees` | jointures hiérarchiques, églises localisées |
| `rechercher` | `UNION ALL` des 3 niveaux, `lower(unaccent(nom)) LIKE '%' \|\| lower(unaccent(:motif)) \|\| '%'` ou `CAST(id AS varchar) = :terme`, `LIMIT :limite` |
| `majCarteQgis` | `UPDATE … SET carte_qgis = ST_SetSRID(ST_GeomFromGeoJSON(:geojson), 4326)` |

Records internes : `LigneZone(id, nom, geojson)`, `LigneRecherche(niveau, id, nom, regionNom, dioceseNom)`.
Listes blanches : `table(niveau)` et `colonneRattachement(niveau)`. `echapperLike` neutralise
`%`, `_` et `\`.

**`mapper/GeometrieMapper`** (Polygon JTS ↔ GeoJSON) :

- `versGeoJson(Polygon)` : `{"type":"Polygon","coordinates":[...]}`, ou `null`.
- `depuisGeoJson(JsonNode)` : une valeur absente, `null` ou une chaîne vide donne `null` (aucune
  carte). Sinon :
  - type `Polygon` accepté ;
  - `MultiPolygon` d'une seule partie accepté et converti ;
  - `MultiPolygon` de plusieurs parties refusé ;
  - anneaux d'au moins 4 positions, fermés ;
  - positions `[longitude, latitude]` numériques ;
  - emprise comprise dans les bornes WGS 84 ;
  - polygone invalide corrigé par `GeometryFixer` (équivalent de `ST_MakeValid`) s'il reste d'un
    seul tenant, refusé sinon.

  Chaque refus lève une `IllegalArgumentException` avec un message en français.
- `SRID_GPS = 4326` (constante publique). `EgliseController` n'utilise pas cette constante : il
  déclare sa propre constante privée `SRID_GPS = 4326` pour sa `GeometryFactory`.

**DTO** :

- `ZoneFeatureCollectionDTO` (`type` = `FeatureCollection`) ;
- `ZoneFeatureDTO` (`type` = `Feature`, `properties` = `ReferenceEntiteDTO`, `geometry` en
  `@JsonRawValue`) ;
- `ZoneGeographiqueDTO`, `EgliseLocaliseeDTO` (avec `paroisseNom`), `ResultatRechercheDTO` ;
- `ImportLimitesRequest` (classe interne `Feature`), `ImportLimitesResultatDTO`.

**Base** : migration `V2__sprint2_geographie_statistiques.sql` et index GIST de `300-index.sql`
([generalite.md](generalite.md)).

## Décisions techniques et raisons

- **`ST_Covers` plutôt que `ST_Contains`** : une église située exactement sur la frontière appartient
  à la zone, et la requête exploite les index GIST (Javadoc de `GeographieRepository`).
- **Une zone est d'un seul tenant** (Polygon, trous autorisés). Le MultiPolygon d'une partie est
  accepté car c'est la forme habituelle des exports QGIS.
- **Contrôle des bornes WGS 84** : il rejette une couche exportée dans un autre système, par exemple
  Laborde (EPSG:29702).
- **GeoJSON produit par PostGIS et transmis tel quel** (`@JsonRawValue`), avec 6 décimales (≈ 10 cm),
  pour des réponses plus légères.
- **Import tolérant** : une feature refusée est signalée sans annuler les autres.
- **Sans limite importée, la zone renvoie ses églises rattachées** : la carte reste utilisable avant
  l'import QGIS.
- **SQL dynamique limité aux listes blanches** : les noms de tables et de colonnes injectés viennent
  de `table` et `colonneRattachement` ; les valeurs passent par des paramètres nommés.

## État actuel connu

- Les cinq endpoints sont fonctionnels.
- `GeometrieMapper` est couvert par `GeometrieMapperTest` (10 cas).
- Le contrôleur est couvert par `Sprint2ControllerTest` : paramètres, `403` sans authentification ou
  sous le niveau 5.
- L'exclusion des lignes supprimées (recherche, églises rattachées) est couverte par
  `Sprint3PostgisIntegrationTest`.
- Le SQL spatial est couvert par `Sprint2PostgisIntegrationTest` : frontière incluse, hors zone,
  import, recherche sans accents ([tests.md](tests.md)).

## Écarts connus avec le code / dette identifiée

- La paroisse a une `carte_qgis` (saisie par le CRUD) mais n'est pas un niveau géographique : elle
  n'est ni importable, ni affichable par `/api/geographie`.
- `localisation` des ressources n'est pas exploitée ici ([module-ressource.md](module-ressource.md)).
- Le corps de `PUT /{niveau}/limites` n'a pas de taille maximale côté application.
- La constante SRID 4326 est déclarée deux fois (`GeometrieMapper.SRID_GPS` et une constante privée
  de `EgliseController`).

## Dernière mise à jour

2026-11-20 — Sprint 3 : import des limites réservé au niveau 5 ; filtre `etat = 1` dans toutes les requêtes spatiales et la recherche.

2026-11-20 — Correction : `EgliseController` a sa propre constante `SRID_GPS` (il n'utilise pas celle de `GeometrieMapper`).

2026-11-20 — Création du fichier (système documentaire initial).
