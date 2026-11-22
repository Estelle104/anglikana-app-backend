# Tests

## Vue d'ensemble

Le projet contient 13 fichiers de test (104 méthodes `@Test`), sous
`backoffice/src/test/java/com/anglikana/backoffice/`. Ils se répartissent en trois niveaux :

- unitaire, sans Spring ;
- couche web (`@WebMvcTest`) ;
- intégration sur la **vraie base PostgreSQL/PostGIS**.

Commande : `./mvnw test` depuis `backoffice/` ([stack-technique.md](stack-technique.md)). Le style des
tests (noms de méthodes en phrases françaises, `preparer()`, etc.) est décrit dans
[conventions-code.md §12](conventions-code.md).

## Structure / composants

| Fichier | Niveau | `@Test` | Couvre |
|---|---|---|---|
| `BackofficeApplicationTests` | `@SpringBootTest` | 1 | Démarrage du contexte complet (base requise) |
| `JwtServiceTest` | unitaire | 1 | Jeton expiré rejeté sans exception (`extractUsername` = `null`, `isTokenValid` = faux) |
| `PasswordEncoderTest` | unitaire | 1 | Affiche un hash BCrypt (aucune assertion) |
| `mapper/GeometrieMapperTest` | unitaire | 10 | Règles de `GeometrieMapper` : WGS 84, trous, MultiPolygon d'une ou plusieurs parties, correction, anneau mal formé, couche hors WGS 84 |
| `service/TableauBordServiceTest` | unitaire, mocks Mockito | 6 | `versStatistiques`, choix de `niveauEnfants`, tri et regroupement du clergé, entité inexistante |
| `sprint2/Sprint2ControllerTest` | `@WebMvcTest` + `@Import(SecurityConfig, MethodSecurityConfig, AutorisationService)` | 26 | Routes d'enfants, `carteQgis` en CRUD, tableau de bord (codes `400`/`403`/`404`, anonyme, hors périmètre, aperçu limité aux diocèses du responsable), géographie (paramètres, `403` sans authentification ou sous le niveau 5, import) |
| `sprint2/Sprint2PostgisIntegrationTest` | `@DataJpaTest` sur base réelle | 19 | Agrégations SQL, chaîne hiérarchique, tableau de bord de chaque niveau, aperçu restreint à des diocèses, routes d'enfants, `ST_Covers` (frontière incluse), hors zone, import QGIS, recherche sans accents |
| `sprint2/JeuDonneesHierarchie` | jeu de données | — | Arbre isolé : 1 diocèse, 2 régions, 2 districts, 2 paroisses, 4 églises placées dans, sur et hors d'un carré de référence, 2 ressources ; responsables A, B, C, X |
| `security/PerimetreAuthorizationManagerTest` | unitaire, mock de `PerimetreRepository` | 15 | `@LevelRequired`, responsable direct, cascade, hors périmètre, anonyme, compte désactivé, entité introuvable (`404`), `id` nul, entité calculée (`entityExpression`), cibles multiples |
| `security/WithUtilisateurConnecte` (+ `…Factory`) | annotation de test | — | Place un `UtilisateurConnecte` (`clergeId`, `niveau`) dans le contexte, à la place de `@WithMockUser` |
| `sprint3/Sprint3ControllerTest` | `@WebMvcTest` + `HierarchieService` réel | 17 | Doyen dans et hors de son district, changement de diocèse d'une région, parent absent (`403`) ou introuvable (`404`), opérations de niveau 5, `/register` public (`201` sans jeton, `409` nom pris), refus `409`, suppression logique, `/api/moi`, lectures publiques |
| `sprint3/Sprint3PostgisIntegrationTest` | `@DataJpaTest` sur base réelle | 8 | `chaineResponsables`, `racines`, suppression logique (`etat = -1`) ignorée par JPA, statistiques, géographie et périmètre ; seules les filles directes comptent ; clergé responsable puis supprimé |

**Configuration de `Sprint2PostgisIntegrationTest`** :

- `@AutoConfigureTestDatabase(replace = NONE)` : utilise la base de `application.properties` ;
- `spring.jpa.hibernate.ddl-auto=none` : le schéma n'est jamais modifié ;
- chaque test tourne dans une transaction annulée à la fin ;
- le jeu `JeuDonneesHierarchie` est inséré dans `preparer()`. L'aperçu est limité au diocèse du jeu,
  ce qui permet de vérifier des totaux exacts. `Sprint3PostgisIntegrationTest` suit la même
  configuration.

## Décisions techniques et raisons

- **Base réelle plutôt que H2** : les requêtes PostGIS (`ST_Covers`, `ST_AsGeoJSON`) et `unaccent`
  n'existent que sur PostgreSQL.
- **Transaction annulée et `ddl-auto=none`** : les tests peuvent tourner sur la base de développement
  sans la polluer ni toucher au schéma.
- **Règles métier pures testées sans base** : `TableauBordService.versStatistiques` est statique et
  package-private, donc testable directement.

## État actuel connu

- Prérequis de `BackofficeApplicationTests` et des deux suites `*PostgisIntegrationTest` : une base
  PostgreSQL/PostGIS accessible à l'URL de `application.properties` (`localhost:5432`), avec les
  migrations V2 et V3 appliquées ([generalite.md](generalite.md)).
- Le 2026-11-22, les tests passent sur une base neuve construite par `database/` (comptes de test
  compris). Le 2026-11-20, ils avaient aussi été joués après V2 et V3.
  Ils ont été lancés dans un conteneur JDK 21, avec `SPRING_DATASOURCE_URL` qui pointe sur
  `eem-database`.
- Couverture : les règles des sprints 1 à 3 (géographie, tableau de bord, droits, suppression logique)
  sont testées. Le CRUD complet de chaque niveau, `ClergeController` hors suppression, et `login` ne
  sont pas testés.

## Écarts connus avec le code / dette identifiée

- **Base Docker sur un autre port** : la base Docker est exposée sur le port hôte `5434`, alors que les
  tests visent `localhost:5432`. Sans PostgreSQL local, il faut surcharger `spring.datasource.url`.
- `PasswordEncoderTest` n'a aucune assertion et utilise `System.out.println`. Sa classe est `public`,
  contrairement aux autres.
- `JwtServiceTest` contient le secret JWT de `application.properties` en clair.
- Les tests sont regroupés par sprint (`sprint2/`, `sprint3/`) et non par domaine. Seuls les tests de
  sécurité sont dans un paquet de domaine (`security/`).

## Dernière mise à jour

2026-11-22 — Retrait d'`OutilControllerTest` (endpoint temporaire supprimé) ; 104 tests.

2026-11-22 — `OutilControllerTest`, tests du `register` public ; 107 tests.

2026-11-20 — Sprint 3 : `PerimetreAuthorizationManagerTest`, `Sprint3ControllerTest`, `Sprint3PostgisIntegrationTest`, `@WithUtilisateurConnecte` ; `Sprint2*` adaptés (fin d'ADMIN, aperçu restreint) ; prérequis V3.

2026-11-20 — Création du fichier (système documentaire initial).
