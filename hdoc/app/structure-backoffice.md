# Structure du package `com.anglikana.backoffice`

## Vue d'ensemble

Le code est rangé **par couche technique**, dans un module Maven unique (`backoffice/`). Ce fichier
donne l'inventaire des classes et leur rôle, couche par couche. Le style d'écriture observé
(nommage, injection, format des DTO, gestion des erreurs) est décrit dans
[conventions-code.md](conventions-code.md) et n'est pas répété ici. Le fonctionnement de chaque
domaine est dans son fichier `module-*.md` (voir [INDEX.md](INDEX.md)).

## Structure / composants

```
backoffice/src/main/java/com/anglikana/backoffice/
├── BackofficeApplication.java   point d'entrée Spring Boot
├── config/        sécurité HTTP, filtre JWT, description OpenAPI (Swagger)
├── controller/    contrôleurs REST, préfixe /api
├── dto/           objets JSON d'entrée/sortie + enum NiveauHierarchique
├── entity/        entités JPA (une par table)
├── mapper/        conversions entité → DTO, classes statiques
├── repository/    Spring Data JPA + 3 repositories SQL natif
├── security/      annotations d'accès, utilisateur connecté, contrôle par niveau et par périmètre
└── service/       logique transverse, JWT, chargement des utilisateurs
```

**`config/`**

| Classe | Rôle | Détail |
|---|---|---|
| `SecurityConfig` | Chaîne de filtres, règles d'URL, CORS, `PasswordEncoder` BCrypt, `AuthenticationManager` | [securite-authentification.md](securite-authentification.md) |
| `JwtAuthenticationFilter` | Lit `Authorization: Bearer`, alimente le `SecurityContext` | idem |
| `OpenApiConfig` | Bean `OpenAPI` (titre, description, version `v1`) et schéma de sécurité `bearerAuth` ; constante `BEARER_SECURITY_SCHEME` utilisée par les contrôleurs | [generalite.md](generalite.md), [conventions-code.md](conventions-code.md) |
| `MethodSecurityConfig` | Intercepteur de méthode des annotations `@LevelRequired`, `@NeedAuthorisation`, `@CascadeAuthorised` | [securite-authentification.md](securite-authentification.md) |

**`security/`** (10 classes, sprint 3, détail dans [securite-authentification.md](securite-authentification.md))

| Classe | Rôle |
|---|---|
| `LevelRequired`, `NeedAuthorisation` (+ conteneur `NeedAuthorisations`), `CascadeAuthorised` | Annotations de méthode |
| `EntiteProtegee` | Enum des entités contrôlées (`RESSOURCE` … `DIOCESE`), table et colonne parente en liste blanche |
| `UtilisateurConnecte` | `UserDetails` portant `clergeId`, `niveau`, `roleNom` |
| `PerimetreAuthorizationManager` | `AuthorizationManager<MethodInvocation>` : lecture des annotations, évaluation SpEL |
| `AutorisationService` | Règles (niveau, responsable, cascade, racines) ; `verifier` pour les contrôles faits dans le code |
| `EntiteIntrouvableException`, `AutorisationExceptionHandler` | Entité ciblée introuvable → `404` (`@RestControllerAdvice`) |

**`controller/`** (13 classes)

| Classe | Route | Fichier de domaine |
|---|---|---|
| `AuthController` | `/api/auth` | [securite-authentification.md](securite-authentification.md) |
| `TestController` | `/api/test` | idem |
| `MoiController` | `/api/moi` | idem |
| `DioceseController`, `RegionController`, `DistrictController`, `ParoisseController`, `EgliseController` | `/api/dioceses`, `/api/regions`, `/api/districts`, `/api/paroisses`, `/api/eglises` | [module-hierarchie.md](module-hierarchie.md) |
| `ClergeController` | `/api/clerges` | [module-clerge.md](module-clerge.md) |
| `RessourceController`, `TypeRessourceController` | `/api/ressources`, `/api/types-ressource` | [module-ressource.md](module-ressource.md) |
| `GeographieController` | `/api/geographie` | [module-geographie.md](module-geographie.md) |
| `TableauBordController` | `/api/tableau-bord` | [module-tableau-bord.md](module-tableau-bord.md) |

**`service/`** (5 classes)

| Classe | Rôle |
|---|---|
| `HierarchieService` | Lecture des enfants d'une entité (régions d'un diocèse, etc.) et des types de ressource ; refus de suppression (fille directe active, clergé responsable actif) |
| `GeographieService` | Limites, recherche, zone, import QGIS |
| `TableauBordService` | Aperçu, tableau de bord, statistiques |
| `JwtService` | Création et lecture des jetons JWT |
| `CustomUserDetailsService` | Chargement d'un `Utilisateur` pour Spring Security |

**`repository/`**

- 13 interfaces Spring Data `JpaRepository<Entite, Integer>`, une par entité : `Clerge`, `Contribution`,
  `Diocese`, `District`, `Eglise`, `HoraireCulte`, `Paroisse`, `Photo`, `Region`, `Ressource`, `Role`,
  `TypeRessource`, `Utilisateur`.
- 3 classes `@Repository` en SQL natif via `EntityManager` :
  - `GeographieRepository` : requêtes PostGIS ([module-geographie.md](module-geographie.md)) ;
  - `HierarchieStatistiqueRepository` : agrégations du tableau de bord ([module-tableau-bord.md](module-tableau-bord.md)) ;
  - `PerimetreRepository` : chaîne des responsables et racines d'un clergé ([securite-authentification.md](securite-authentification.md)).

**`entity/`** (13 classes) : `Diocese`, `Region`, `District`, `Paroisse`, `Eglise`, `Clerge`, `Role`,
`Utilisateur`, `Ressource`, `TypeRessource`, `HoraireCulte`, `Contribution`, `Photo`. Elles reflètent
les tables de `database/002-table.sql` (voir [generalite.md](generalite.md)).

**`mapper/`** (8 classes) : `ClergeMapper`, `RegionMapper`, `DistrictMapper`, `ParoisseMapper`,
`EgliseMapper`, `RessourceMapper` et `TypeRessourceMapper` (entité → DTO), plus `GeometrieMapper`
(Polygon JTS ↔ GeoJSON, [module-geographie.md](module-geographie.md)). Il n'y a pas de `DioceseMapper` :
la conversion est une méthode privée `toDTO` de `DioceseController`.

**`dto/`** (28 classes) : DTO par entité (`*DTO`), entrées d'authentification (`LoginRequest`,
`RegisterRequest`, `LoginResponse`), profil de l'utilisateur connecté (`ProfilDTO`), DTO géographiques (`Zone*`, `ImportLimites*`,
`EgliseLocaliseeDTO`, `ResultatRechercheDTO`), DTO du tableau de bord (`ApercuHierarchieDTO`,
`TableauBordDTO`, `NoeudHierarchiqueDTO`, `StatistiquesEntiteDTO`, `TotauxHierarchieDTO`,
`ReferenceEntiteDTO`, `ClergeAffecteDTO`, `RessourceDetailleeDTO`) et l'enum `NiveauHierarchique`.

**Flux d'une requête**

- CRUD simple : `Controller → Repository → Entity`, puis `Mapper.toDTO` pour la réponse.
- Lecture transverse : `Controller → Service → Repository`. Le service renvoie déjà des DTO, et un
  `Optional` vide quand l'entité parente n'existe pas.

## Décisions techniques et raisons

- **Découpage par couche, pas par domaine** : petit nombre d'entités, toutes liées à la même hiérarchie.
- **SQL natif pour la géographie et les statistiques** : PostGIS (`ST_Covers`, `ST_AsGeoJSON`) et les
  agrégations par CTE ne s'expriment pas en requêtes dérivées Spring Data.
- **`NiveauHierarchique` dans `dto/`** : l'enum est sérialisée vers le frontend par son code
  (`@JsonValue`) ; elle fait donc partie du contrat JSON.

## État actuel connu

Inventaire à jour au 2026-11-20 : 4 classes de configuration, 13 contrôleurs (tous annotés pour
Swagger), 5 services, 16 repositories, 13 entités, 8 mappers, 28 DTO, 10 classes `security/`. Il n'y a
ni package `exception`, ni configuration Jackson. Le seul `@RestControllerAdvice`
(`AutorisationExceptionHandler`) ne traite que `EntiteIntrouvableException`.

## Écarts connus avec le code / dette identifiée

- Les CRUD ne passent pas par un service : la conversion DTO → entité (`appliquerDTO`) est écrite dans
  chaque contrôleur.
- `DioceseController` est le seul contrôleur sans mapper dédié.
- `ContributionRepository`, `HoraireCulteRepository` et `PhotoRepository` n'ont aucun appelant
  ([entites-sans-api.md](entites-sans-api.md)).
- `TestController` est un contrôleur de démonstration du sprint 1.

## Dernière mise à jour

2026-11-22 — Suppression d'`OutilController` (temporaire) ; 13 contrôleurs.

2026-11-22 — Ajout de `OutilController` (temporaire, hachage BCrypt) ; 14 contrôleurs.

2026-11-20 — Sprint 3 : package `security/`, `config/MethodSecurityConfig`, `MoiController`, `PerimetreRepository`, `ProfilDTO` ; inventaire mis à jour.

2026-11-20 — Ajout de `config/OpenApiConfig` (Swagger) ; contrôleurs annotés.

2026-11-20 — Création du fichier (système documentaire initial).
