# Généralités du projet `anglikana-app-backend`

## Vue d'ensemble

Backend du **back-office** de l'application A.C.A (*Anglican Church Application*). L'application
recense les églises et infrastructures anglicanes de Madagascar. Le back-office est l'application web
de l'administrateur : il gère la hiérarchie ecclésiastique, le clergé, les ressources, les limites
géographiques QGIS et un tableau de bord. L'application mobile (hors ligne) et le frontend Angular sont
dans d'autres dépôts.

Contexte métier et planning : `hdoc/source/Cahier_des_charges_ACA.md` et `hdoc/source/ACA_Project.md`
(sprints 0 à 3 ; la branche courante est `features/sprint-3-RBAC`).

Versions, outils et commandes : [stack-technique.md](stack-technique.md), qui fait foi et n'est pas
dupliqué ici.

## Structure / composants

**Identité** (`backoffice/pom.xml`) :

| Propriété | Valeur |
|---|---|
| groupId / artifactId | `com.anglikana` / `backoffice` |
| Version | `0.0.1-SNAPSHOT` |
| Package racine | `com.anglikana.backoffice` (détail : [structure-backoffice.md](structure-backoffice.md)) |
| Point d'entrée | `BackofficeApplication.java` |
| Port | `8084` |

Les champs `name`, `description`, `url`, `licenses`, `developers` et `scm` du `pom.xml` sont vides
(valeurs par défaut de Spring Initializr).

**Domaine métier couvert** :

- Hiérarchie Diocèse → Région → District → Paroisse → Église ([module-hierarchie.md](module-hierarchie.md)) ;
- Clergé et ses responsabilités ([module-clerge.md](module-clerge.md)) ;
- Ressources (infrastructures rattachées à une église) et leurs types ([module-ressource.md](module-ressource.md)) ;
- Limites géographiques QGIS, recherche, églises d'une zone ([module-geographie.md](module-geographie.md)) ;
- Tableau de bord et statistiques ([module-tableau-bord.md](module-tableau-bord.md)) ;
- Authentification JWT et rôles ([securite-authentification.md](securite-authentification.md)) ;
- Tables prévues sans API : contributions, photos, horaires de culte ([entites-sans-api.md](entites-sans-api.md)).

**Configuration** (`backoffice/src/main/resources/application.properties`, fichier unique, sans profil) :

| Domaine | Propriété | Valeur / source |
|---|---|---|
| Serveur | `server.port` | `8084` |
| Datasource | `spring.datasource.url` | `jdbc:postgresql://localhost:5432/anglican_db_mg` (lancement hors Docker) |
| Datasource | `spring.datasource.username` / `password` | écrits en clair (voir « Écarts connus ») |
| JPA | `spring.jpa.hibernate.ddl-auto` | `update` |
| JWT | `jwt.secret` | clé HMAC base64, écrite en clair (voir « Écarts connus ») |
| JWT | `jwt.expiration` | `86400000` ms (24 h) |

En Docker, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` et
`JWT_SECRET` (fournis par `docker-compose.yml` depuis `.env`) écrasent ces valeurs grâce au
*relaxed binding* de Spring Boot.

**Variables d'environnement** (`.env`, non versionné ; modèle `.env.example`) : `POSTGRES_DB`,
`POSTGRES_USER`, `POSTGRES_PASSWORD`, `JWT_SECRET` (clé base64 d'au moins 256 bits).

**Docker** (`docker-compose.yml`, `backoffice/Dockerfile`) : deux conteneurs, `eem-database`
(PostGIS, port hôte 5434) et `eem-backend` (`mvn spring-boot:run`, code monté, DevTools). Détail des
images, ports, volumes et commandes : [stack-technique.md §9](stack-technique.md).

**Scripts SQL de `database/`** : ils sont montés sur `/docker-entrypoint-initdb.d`, donc exécutés par
ordre alphabétique, **uniquement à la création du volume** `eem-postgres-data`.

| Fichier | Plage | Contenu |
|---|---|---|
| `001-init.sql` | 0xx — initialisation | Extensions `postgis` (types `GEOMETRY`) et `unaccent` (recherche sans accents) |
| `002-table.sql` | 0xx — tables | `role`, `clerge`, `diocese`, `region`, `district`, `paroisse`, `eglise`, `horaire_culte`, `type_ressource`, `ressource`, `utilisateur`, `contribution`, `photo` ; contraintes `fk_<table>_<cible>` en bas de chaque `CREATE TABLE` ; colonne `etat` (`ck_<table>_etat`) sur `clerge` et les tables de la hiérarchie et des ressources ; `role.niveau` NOT NULL, UNIQUE, entre 1 et 5 |
| `300-index.sql` | 3xx — index | Index polymorphes (`photo`, `contribution`), index des clés étrangères hiérarchiques, index GIST sur `carte_qgis` et `eglise.localisation`, index partiels `idx_<table>_responsable_actif` (`WHERE etat = 1`) |
| `400-data.sql` | 4xx — données | Jeu de démonstration : 5 rôles (Katekista 1 … Évêque 5, id = niveau), 8 membres du clergé, 2 diocèses, 2 régions, 2 districts, 2 paroisses, 2 églises localisées, horaires de culte, 2 types de ressource, 1 ressource, 7 comptes de test ; recalage des séquences (`setval`) |

**Responsables et comptes de test de `400-data.sql`** (un compte par responsable, mot de passe commun
donné en commentaire dans le fichier) :

| Clergé | Rôle (niveau) | Responsable de | Compte |
|---|---|---|---|
| 1 | Évêque (5) | diocèse 1 | `eveque.antananarivo` |
| 2 | Évêque (5) | diocèse 2, région 2 | `eveque.toamasina` |
| 6 | Archidiacre (4) | région 1 | `archidiacre.analamanga` |
| 3 | Doyen (3) | district 1, église 1, ressource 1 | `doyen.tana` |
| 4 | Doyen (3) | district 2, paroisse 2 | `doyen.toamasina` |
| 7 | Presbytère (2) | paroisse 1 | `presbytere.saintjoseph` |
| 5 | Katekista (1) | église 2 | `katekista.saintetherese` |
| 8 | Katekista (1) | rien | aucun (pour tester la suppression d'un clergé et `/register`) |

La ressource 1 (église 1) rend `DELETE /api/eglises/1` impossible (`409`) tant qu'elle est active. Ces
données ne sont chargées que sur une base neuve : `docker compose down -v` puis `up` (le `-v` efface
le volume, donc toutes les données existantes).

**Migrations pour une base existante** (`backoffice/migrations/`, appliquées à la main) :

| Fichier | Effet |
|---|---|
| `V2__sprint2_geographie_statistiques.sql` | `carte_qgis` passe de `VARCHAR` (chemin d'un `.qgz`) à `GEOMETRY(Polygon, 4326)` pour `diocese`, `district` et `paroisse` (anciennes valeurs remplacées par `NULL`) ; ajout de `region.carte_qgis` ; index GIST et index des clés étrangères ; extension `unaccent`. Idempotent |
| `V2__sprint2_geographie_statistiques_rollback.sql` | Annulation : suppression des index, retour de `carte_qgis` en `VARCHAR(255)` vide. Les polygones importés sont perdus |
| `V3__sprint3_rbac.sql` | Colonne `etat` (1 / -1, défaut 1, `CHECK`) sur les 7 tables et index partiels des responsables actifs. Remplacement de Admin / Curé / Diacre par les 5 rôles de niveau, chaque clergé recevant le rôle de sa plus haute responsabilité active. `role.niveau` NOT NULL, UNIQUE, entre 1 et 5. Idempotent : les rôles ne sont remplacés que si `Admin` existe |
| `V3__sprint3_rbac_rollback.sql` | Annulation : retrait de `etat` (les lignes supprimées redeviennent visibles), retour à Admin / Curé / Diacre par correspondance de niveau (approximatif) |

À appliquer dans l'ordre V2 puis V3. Testé le 2026-11-20 sur une base neuve et sur une base à l'ancien
schéma : migration, réexécution, rollback, puis nouvelle migration.

## Décisions techniques et raisons

- **Schéma géré par des scripts SQL**, pas par Hibernate : `database/` construit une base neuve et
  `backoffice/migrations/` fait évoluer une base existante. Toute modification de schéma touche donc
  ces deux endroits ([stack-technique.md, « Règles pour Claude »](stack-technique.md)).
- **SRID 4326 (WGS 84) partout** : limites et localisations sont dans le même système, donc les
  requêtes spatiales ne font aucune transformation (commentaire de la migration V2).
- **Port hôte 5434** pour la base : 5432 est pris par le PostgreSQL local et 5433 par une autre stack
  (commentaire de `docker-compose.yml`).
- **Healthcheck `pg_isready -h localhost`** : la base n'est annoncée prête qu'une fois les scripts
  `database/` exécutés, donc le backend ne démarre pas sur une base vide.
- **Valeurs locales dans `application.properties`** et valeurs Docker par variables d'environnement :
  le lancement hors Docker reste possible sans `.env`.

## État actuel connu

- Sprints 1 (CRUD, authentification) et 2 (géographie, tableau de bord) livrés. Sprint 3, côté back-office :
  droits par niveau et par périmètre de responsable, et suppression logique
  ([securite-authentification.md](securite-authentification.md)). Une base existante doit recevoir
  la migration V3.
- Documentation d'API Swagger/OpenAPI (springdoc, depuis le 2026-11-20) : Swagger UI sur
  `http://localhost:8084/swagger-ui.html` et spec JSON sur `/v3/api-docs`, en accès public. Les 57
  endpoints sont annotés ; pour tester une écriture, utiliser le bouton « Authorize » avec un jeton de
  `POST /api/auth/login`. Configuration : `config/OpenApiConfig.java`
  ([securite-authentification.md](securite-authentification.md) pour l'accès,
  [conventions-code.md](conventions-code.md) pour la convention d'annotation).
- Aucun profil Spring (`dev`/`prod`).

## Écarts connus avec le code / dette identifiée

- **Secrets en clair** dans `application.properties` (mot de passe PostgreSQL, `jwt.secret`), alors
  qu'un `.env` existe pour Docker. Le même secret est recopié dans `JwtServiceTest`.
- **`ddl-auto=update`** alors que le schéma est géré par les scripts SQL : Hibernate peut modifier la
  base en dehors de `database/` et `migrations/`. Exemple : sur une base sans V3, il ajoute lui-même la
  colonne `etat` (défaut 1, grâce à `@ColumnDefault`), mais ni les contraintes ni les nouveaux rôles.
  V3 reste donc nécessaire.
- Dans `400-data.sql`, `contribution` et `photo` n'ont aucune donnée.
- **Comptes de test au mot de passe connu dans `400-data.sql`** : ils sont créés sur toute base neuve,
  y compris en production. Aucun profil ne les sépare des données de démonstration.
- Les autres écarts de style sont listés dans [conventions-code.md §15](conventions-code.md).
- Swagger UI et `/v3/api-docs` sont actifs sans condition (aucun profil) ; springdoc conseille de les
  désactiver en production (`springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false`).

## Dernière mise à jour

2026-11-22 — `400-data.sql` : clergés 6 à 8, ressources, 7 comptes de test (tableau des responsables).

2026-11-20 — Migration V3 (RBAC, `etat`), scripts `database/` mis à jour, état du sprint 3, 57 endpoints.

2026-11-20 — Swagger/OpenAPI disponible (springdoc) : URL et accès dans « État actuel connu ».

2026-11-20 — Création du fichier (système documentaire initial).
