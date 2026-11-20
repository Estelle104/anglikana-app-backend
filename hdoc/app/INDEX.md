# INDEX — hdoc/app

> Lire ce fichier en premier. N'ouvrir que le ou les fichiers de détail utiles à la tâche, jamais
> tout le dossier par prudence.

## Fichiers de détail

| Fichier | Domaine couvert | Code concerné |
|---|---|---|
| [generalite.md](generalite.md) | Identité du projet, domaine métier, configuration (`application.properties`, `.env`), Docker, rôle et ordre des scripts SQL `database/` et des migrations `backoffice/migrations/` | `backoffice/pom.xml`, `application.properties`, `docker-compose.yml`, `backoffice/Dockerfile`, `.env.example`, `database/*.sql`, `backoffice/migrations/*.sql` |
| [structure-backoffice.md](structure-backoffice.md) | Arborescence du package `com.anglikana.backoffice`, inventaire des classes par couche, flux d'une requête | `backoffice/src/main/java/com/anglikana/backoffice/**` |
| [securite-authentification.md](securite-authentification.md) | Spring Security (règles d'URL, CORS, sans état), JWT, chargement des utilisateurs, login/register/logout, rôle porté par le clergé, RBAC du sprint 3 (niveaux, périmètre, `@LevelRequired` / `@NeedAuthorisation` / `@CascadeAuthorised`) | `config/SecurityConfig`, `config/JwtAuthenticationFilter`, `config/MethodSecurityConfig`, `security/**`, `service/JwtService`, `service/CustomUserDetailsService`, `controller/AuthController`, `controller/TestController`, `controller/MoiController`, `entity/Utilisateur`, `UtilisateurRepository`, `repository/PerimetreRepository`, `dto/Login*`, `dto/RegisterRequest`, `dto/ProfilDTO` |
| [module-hierarchie.md](module-hierarchie.md) | CRUD Diocèse, Région, District, Paroisse et Église, routes des enfants, `HierarchieService`, enum `NiveauHierarchique` | `controller/{Diocese,Region,District,Paroisse,Eglise}Controller`, `service/HierarchieService`, entités, DTO, mappers et repositories correspondants, `dto/NiveauHierarchique` |
| [module-clerge.md](module-clerge.md) | CRUD du clergé, rôle de responsable des entités | `controller/ClergeController`, `entity/Clerge`, `dto/ClergeDTO`, `dto/ClergeAffecteDTO`, `mapper/ClergeMapper`, `ClergeRepository` |
| [module-ressource.md](module-ressource.md) | CRUD des ressources (infrastructures d'une église), lecture des types de ressource | `controller/RessourceController`, `controller/TypeRessourceController`, `entity/Ressource`, `entity/TypeRessource`, `dto/Ressource*`, `dto/TypeRessourceDTO`, mappers et repositories correspondants |
| [module-geographie.md](module-geographie.md) | Limites QGIS en GeoJSON, import d'une FeatureCollection, recherche sans accents, zone et églises dans ou hors zone (PostGIS), validation des polygones | `controller/GeographieController`, `service/GeographieService`, `repository/GeographieRepository`, `mapper/GeometrieMapper`, `dto/Zone*`, `dto/ImportLimites*`, `dto/EgliseLocaliseeDTO`, `dto/ResultatRechercheDTO`, `migrations/V2__*` |
| [module-tableau-bord.md](module-tableau-bord.md) | Aperçu, tableau de bord et statistiques d'une entité, CTE `perimetre`, règles de comptage (clergé, descendants) | `controller/TableauBordController`, `service/TableauBordService`, `repository/HierarchieStatistiqueRepository`, `dto/ApercuHierarchieDTO`, `dto/TableauBordDTO`, `dto/NoeudHierarchiqueDTO`, `dto/StatistiquesEntiteDTO`, `dto/TotauxHierarchieDTO`, `dto/ReferenceEntiteDTO` |
| [entites-sans-api.md](entites-sans-api.md) | Tables avec une entité mais sans contrôleur : `Role`, `HoraireCulte`, `Contribution`, `Photo` | `entity/{Role,HoraireCulte,Contribution,Photo}`, repositories correspondants |
| [tests.md](tests.md) | Suites de tests, niveaux (unitaire, web, intégration PostGIS), prérequis, couverture | `backoffice/src/test/**` |
| [CHANGELOG.md](CHANGELOG.md) | Journal des changements de la documentation (ajout en fin de fichier uniquement) | — |

## Fichiers de référence

Ces fichiers ont leur propre structure et ne suivent pas le plan commun à 6 sections. Les fichiers de
détail y renvoient au lieu de dupliquer leur contenu.

| Fichier | Contenu |
|---|---|
| [stack-technique.md](stack-technique.md) | Versions exactes (Java, Spring Boot, Hibernate, PostgreSQL/PostGIS, JJWT…), commandes Maven et Docker, règles techniques par version |
| [conventions-code.md](conventions-code.md) | Style de code observé (langue, injection, entités, DTO, mappers, contrôleurs, erreurs, tests, SQL, Git) et liste des incohérences de style (§15) |
| `../source/` | Cahier des charges (`Cahier_des_charges_ACA.md`) et planning par sprint (`ACA_Project.md`) |

## Règle de frontière entre domaines

- Une entité de la chaîne Diocèse → Église relève de [module-hierarchie.md](module-hierarchie.md)
  pour son CRUD et ses routes d'enfants.
- Sa **géométrie** (polygones, points, requêtes spatiales, import QGIS) relève de
  [module-geographie.md](module-geographie.md). La saisie de `carteQgis` par le CRUD reste dans
  `module-hierarchie.md`, qui renvoie à la géographie pour les règles de validation.
- Ses **comptages** et sa vue de synthèse relèvent de [module-tableau-bord.md](module-tableau-bord.md).
- Le **clergé** relève de [module-clerge.md](module-clerge.md). Ce qui donne des droits (rôle, compte
  utilisateur) relève de [securite-authentification.md](securite-authentification.md).
- Une entité de [entites-sans-api.md](entites-sans-api.md) qui reçoit un contrôleur rejoint le
  fichier de son domaine, ou un nouveau fichier créé sur validation.
- En cas de doute, signaler l'ambiguïté plutôt que trancher sans le dire.

## Protocole de lecture et d'écriture

Voir `CLAUDE.md` §2 (racine du projet) : lire cet INDEX, n'ouvrir que les fichiers utiles, modifier
une section précise, journaliser dans [CHANGELOG.md](CHANGELOG.md), et appliquer la validation
documentaire.
