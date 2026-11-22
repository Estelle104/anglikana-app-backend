# Stack technique — anglikana-app-backend

> Backend du back-office ACA. Module unique : `backoffice/` (Maven, Spring Boot).
> Versions vérifiées le 2026-11-20 par `./mvnw dependency:list`, à partir de `backoffice/pom.xml`.
> Si le `pom.xml` change, mettre à jour ce fichier.
> Voir aussi : [INDEX.md](INDEX.md) (fichier de référence de la documentation `hdoc/app`).

## En bref

| Élément | Valeur |
|---|---|
| Langage | Java **21** |
| Framework | Spring Boot **4.1.0** (Spring Framework **7.0.8**) |
| Build | Maven **3.9.16** via le wrapper `./mvnw` (wrapper 3.3.4) |
| Base de données | PostgreSQL **16.15** + PostGIS + unaccent |
| ORM | Hibernate **7.4.1.Final** + Hibernate Spatial |
| Sécurité | Spring Security **7.1.0**, JWT sans état (JJWT **0.12.6**) |
| Serveur | Tomcat embarqué **11.0.22**, port **8084** |
| Package racine | `com.anglikana.backoffice` |

---

## 1. Langage et build

| Technologie | Version | Source |
|---|---|---|
| Java | 21 | `<java.version>` dans `pom.xml` |
| Spring Boot parent | 4.1.0 | `<parent>` dans `pom.xml` |
| Maven | 3.9.16 | `.mvn/wrapper/maven-wrapper.properties` |
| Lombok | 1.18.46 | géré par Spring Boot, déclaré comme annotation processor |

Commandes (depuis `backoffice/`) :

```bash
./mvnw spring-boot:run      # lancer l'application (http://localhost:8084)
./mvnw test                 # lancer les tests
./mvnw dependency:list      # voir les versions résolues
```

> ⚠️ Il faut un **JDK 21**. Le JDK 17 ne compile pas le projet (`release 21`).

## 2. Spring

| Composant | Version | Usage dans le projet |
|---|---|---|
| Spring Framework (core, webmvc) | 7.0.8 | API REST (`@RestController`) |
| Spring Data JPA | 4.1.0 | interfaces dans `repository/` |
| Spring Security | 7.1.0 | filtre JWT + RBAC par annotations de méthode (`@LevelRequired`, `@NeedAuthorisation`) |
| Spring Validation | (Hibernate Validator 9.1.0.Final) | validation des DTO (`jakarta.validation`) |
| Spring Boot DevTools | 4.1.0 | rechargement à chaud, en dev uniquement |

Starters déclarés : `data-jpa`, `security`, `oauth2-resource-server`, `validation`, `webmvc`, `devtools`.

## 3. Base de données et données géographiques

| Technologie | Version | Rôle |
|---|---|---|
| PostgreSQL | 16.15 | base `anglican_db_mg` sur `localhost:5432` |
| PostGIS | extension PostgreSQL (`CREATE EXTENSION postgis`) | stockage des géométries |
| unaccent | extension PostgreSQL | recherche sans accents |
| Driver JDBC PostgreSQL | 42.7.11 | |
| Hibernate ORM | 7.4.1.Final | `spring.jpa.hibernate.ddl-auto=update` |
| Hibernate Spatial | 7.4.1.Final | mapping des types PostGIS |
| JTS Core | 1.19.0 | types Java `Polygon`, `Point` (`org.locationtech.jts.geom`) |
| Jakarta Persistence API | 3.2.0 | annotations `jakarta.persistence.*` |
| QGIS | outil externe (logiciel de bureau) | dessin des limites géographiques importées dans l'API |

**Conventions géographiques :**
- SRID **4326** (WGS 84) partout.
- Limites de zones : colonne `carte_qgis`, type `geometry(Polygon,4326)`, mappée en `org.locationtech.jts.geom.Polygon`.
- Localisation des églises : `geometry(Point,4326)`.
- Import des limites : `PUT /api/geographie/{niveau}/limites`.
- Conversion géométrie ↔ DTO : `mapper/GeometrieMapper.java`.

**Scripts SQL** (aucun Flyway ni Liquibase) :
- `database/` : initialisation d'une base neuve, exécutée par Docker (`docker-entrypoint-initdb.d`) par ordre alphabétique. Numérotation : `0xx` = init et tables, `3xx` = index, `4xx` = données.
  - `001-init.sql` : extensions `postgis` et `unaccent`
  - `002-table.sql` : tables
  - `300-index.sql` : index (relations polymorphes, hiérarchie, GIST)
  - `400-data.sql` : données de démo
- `backoffice/migrations/V<n>__*.sql` : migration d'une base existante, à lancer à la main, avec son script de rollback (ex. sprint 2)

## 4. Sécurité

| Technologie | Version | Rôle |
|---|---|---|
| Spring Security | 7.1.0 | configuration dans `config/SecurityConfig.java` |
| JJWT (`jjwt-api`, `-impl`, `-jackson`) | 0.12.6 | génération et vérification des tokens (`service/JwtService.java`) |
| Nimbus JOSE JWT | 10.9 | apporté par `oauth2-resource-server` |

**Règles actuelles :**
- Sans état (pas de session), CSRF désactivé, CORS configuré.
- `POST /api/auth/login`, `/register` et `/logout` : accès public.
- `GET /api/tableau-bord/**` et `/api/moi` : authentification exigée.
- Autres `GET /api/**` : accès public.
- `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` : accès public (Swagger).
- Écritures (POST/PUT/DELETE) : authentification exigée. RBAC par niveau et par périmètre : annotations `@LevelRequired`, `@NeedAuthorisation` et `@CascadeAuthorised` (`security/`), appliquées par `config/MethodSecurityConfig` (`@EnableMethodSecurity` reste actif).
- Le token est lu par `config/JwtAuthenticationFilter.java` ; les utilisateurs sont chargés par `service/CustomUserDetailsService.java`.
- Durée de vie du token : `jwt.expiration=86400000` (24 h).

## 5. Sérialisation et serveur

| Technologie | Version | Remarque |
|---|---|---|
| Jackson 3 (`tools.jackson.core`) | 3.1.4 | JSON par défaut avec Spring Boot 4 |
| Jackson 2 (`com.fasterxml.jackson`) | 2.21.4 | uniquement au runtime, pour `jjwt-jackson` |
| Tomcat embarqué | 11.0.22 | |
| springdoc-openapi (`springdoc-openapi-starter-webmvc-ui`) | 3.1.1 | Swagger UI (`/swagger-ui.html`) et spec OpenAPI (`/v3/api-docs`), configurés par `config/OpenApiConfig.java`. Version explicite dans le `pom.xml`, car springdoc n'est pas géré par le parent Spring Boot ; la 3.1.1 est construite sur `spring-boot-starter-parent` 4.1.0 |

> Avec Spring Boot 4, il faut importer Jackson depuis **`tools.jackson.*`** (Jackson 3), et non depuis `com.fasterxml.jackson.databind`.
> Exception : les annotations (`com.fasterxml.jackson.annotation.*`) gardent leur ancien package.

## 6. Tests

| Technologie | Version |
|---|---|
| JUnit Jupiter | 6.0.3 |
| Mockito | 5.23.0 |
| Starters de test | `data-jpa-test`, `security-test`, `validation-test`, `webmvc-test` |

## 7. Architecture du code

```
backoffice/src/main/java/com/anglikana/backoffice/
├── config/       # SecurityConfig, JwtAuthenticationFilter
├── controller/   # API REST (/api/...)
├── dto/          # objets d'entrée et de sortie de l'API
├── entity/       # entités JPA (Diocese, Region, District, Paroisse, Eglise, Clerge, Utilisateur, Role…)
├── mapper/       # conversion entité ↔ DTO (écrite à la main, sans MapStruct)
├── repository/   # interfaces Spring Data JPA
└── service/      # logique métier, JwtService
```

Hiérarchie métier : Diocèse → Région → District → Paroisse → Église.

## 8. Configuration

Fichier : `backoffice/src/main/resources/application.properties`

| Clé | Valeur |
|---|---|
| `server.port` | 8084 |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/anglican_db_mg` |
| `spring.jpa.hibernate.ddl-auto` | `update` |
| `jwt.secret` / `spring.datasource.password` | ⚠️ écrits en clair dans le dépôt : à déplacer dans des variables d'environnement |
| `jwt.expiration` | 86400000 ms (24 h) |

## 9. Docker

Fichiers : `docker-compose.yml` (racine), `backoffice/Dockerfile`, `database/`, `.env` (non versionné, modèle : `.env.example`).

| Conteneur | Image | Port hôte → conteneur | Rôle |
|---|---|---|---|
| `eem-database` | `postgis/postgis:16-3.5-alpine` | 5434 → 5432 | PostgreSQL 16 + PostGIS 3.5, volume `eem-postgres-data` |
| `eem-backend` | build `maven:3.9-eclipse-temurin-21` | 8084 → 8084 | `mvn spring-boot:run`, code `./backoffice` monté sur `/app` |

- Réseau `eem-network` (bridge). Le backend joint la base via `jdbc:postgresql://eem-database:5432/${POSTGRES_DB}`.
- Le backend attend que la base soit *healthy* (`pg_isready`), donc que les scripts `database/` aient fini de s'exécuter.
- Les variables `SPRING_DATASOURCE_*` et `JWT_SECRET` écrasent `application.properties` (relaxed binding de Spring Boot), qui reste valable pour un lancement hors Docker.
- Le choix du port 5434 évite les conflits avec le PostgreSQL local (5432) et avec la stack CNTEMAD (5433).

```bash
cp .env.example .env              # première fois : renseigner les valeurs
docker compose up --build -d      # démarrer
docker compose logs -f eem-backend
docker compose down               # arrêter (données conservées)
docker compose down -v            # arrêter et supprimer la base (réexécute database/ au prochain up)
docker exec -it eem-database psql -U postgres -d anglican_db_mg
```

> Les scripts de `database/` ne s'exécutent **que si le volume est vide**. Après une modification, il faut soit lancer `down -v`, soit appliquer une migration.

---

## Règles pour Claude

- Utiliser les **API de Spring Boot 4 / Spring Framework 7 / Spring Security 7**. Pas de `WebSecurityConfigurerAdapter`, pas de `javax.*` (seulement `jakarta.*`). Configurer Security avec un bean `SecurityFilterChain` et des lambdas.
- Utiliser **Hibernate 7** : `jakarta.persistence` 3.2, types JTS pour les géométries (pas de type `geolatte`).
- Écrire le JSON avec **Jackson 3** (`tools.jackson.*`).
- Pour le JWT, utiliser l'API **JJWT 0.12** : `Jwts.builder()…signWith(key)`, `Jwts.parser().verifyWith(key).build().parseSignedClaims(...)`. Les méthodes `setSubject` et `parseClaimsJws` sont obsolètes.
- Ne pas indiquer de version pour les dépendances gérées par le parent Spring Boot.
- Toute nouvelle géométrie : SRID 4326 et `columnDefinition = "geometry(<Type>,4326)"`.
- Toute modification du schéma se fait à deux endroits :
  1. dans `database/`, en modifiant ou en ajoutant un fichier numéroté selon sa catégorie, pour les bases neuves ;
  2. dans `backoffice/migrations/V<n>__<description>.sql`, avec son script de rollback, pour les bases existantes.
- Toute nouvelle variable d'environnement : l'ajouter dans `docker-compose.yml`, dans `.env.example` (sans valeur secrète) et dans `.env`.
- Protéger toute nouvelle route d'écriture avec `@NeedAuthorisation` (+ `@CascadeAuthorised`) ou `@LevelRequired` ([securite-authentification.md](securite-authentification.md)).
- Ne jamais copier les secrets de `application.properties` dans du code, de la documentation ou des messages.
