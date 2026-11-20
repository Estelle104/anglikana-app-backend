# Sécurité et authentification

## Vue d'ensemble

L'authentification est **sans état, par JWT** (Spring Security 7, JJWT 0.12). Un utilisateur se
connecte par `POST /api/auth/login` et reçoit un jeton à envoyer ensuite dans
`Authorization: Bearer <jeton>`.

Depuis le sprint 3 (branche `features/sprint-3-RBAC`), les droits viennent du **membre du clergé** lié au
compte. Il n'y a plus de rôle ADMIN :

- **niveau** du rôle du clergé (`role.niveau`) : 5 Évêque (diocèse), 4 Archidiacre (région), 3 Doyen
  (district), 2 Presbytère (paroisse), 1 Katekista (église), 0 sans rôle ;
- **périmètre** : les entités dont le clergé est le responsable (`responsable_id`) et toute leur
  descendance. Un responsable a le CRUD complet sur ce périmètre, son entité comprise.

Les contrôles sont portés par trois annotations de méthode : `@LevelRequired`, `@NeedAuthorisation` et
`@CascadeAuthorised`. Les lectures `GET /api/**` restent publiques (application mobile), sauf le tableau
de bord et `/api/moi`.

## Structure / composants

**`config/SecurityConfig`** (`@EnableWebSecurity`, `@EnableMethodSecurity`) :

| Élément | Comportement |
|---|---|
| CSRF | désactivé |
| Session | `SessionCreationPolicy.STATELESS` |
| `POST /api/auth/login`, `POST /api/auth/logout` | `permitAll` (`/register` n'est plus public) |
| `OPTIONS /**` | `permitAll` (requêtes préliminaires CORS) |
| `GET /api/tableau-bord`, `/api/tableau-bord/**`, `/api/moi` | `authenticated`, déclarées avant la règle suivante |
| `GET /api/**` | `permitAll` |
| `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | `permitAll` (Swagger, hors de `/api`) |
| Toute autre requête (dont `/error`) | `authenticated` |
| Filtre | `JwtAuthenticationFilter` ajouté avant `UsernamePasswordAuthenticationFilter` |
| `PasswordEncoder` | `BCryptPasswordEncoder` |
| `DaoAuthenticationProvider` | construit avec `CustomUserDetailsService` + BCrypt |
| CORS (`corsConfigurationSource`) | origines `http://localhost:*` et `http://127.0.0.1:*`, méthodes `GET POST PUT PATCH DELETE OPTIONS`, en-têtes `Authorization Content-Type Accept`, `allowCredentials=true` |

**`config/JwtAuthenticationFilter`** (`OncePerRequestFilter`) :

1. sans en-tête `Authorization: Bearer …`, la requête continue sans authentification ;
2. sinon, `JwtService.extractUsername(token)` lit le sujet ;
3. si un sujet est trouvé et que le contexte est vide, l'utilisateur est rechargé par
   `CustomUserDetailsService`, et le jeton est vérifié par `isTokenValid` ;
4. si le jeton est valide **et le compte actif** (`isEnabled`), un `UsernamePasswordAuthenticationToken`
   portant l'`UtilisateurConnecte` est placé dans le `SecurityContext`. Un compte désactivé reste
   anonyme, même avec un jeton encore valide.

Le compte est rechargé à chaque requête : un changement de responsable ou de rôle s'applique tout de
suite, sans nouveau jeton.

**`service/JwtService`** (`@Service`) :

| Méthode | Rôle |
|---|---|
| `generateToken(username, role)` | Jeton signé HMAC : `sub` = nom d'utilisateur, claim `role`, `iat`, `exp` = maintenant + `jwt.expiration` |
| `extractUsername` / `extractRole` | Lecture du claim ; `null` si le jeton est invalide ou expiré |
| `extractExpiration` | Expiration ; `new Date(0)` si le jeton est expiré, `null` s'il est invalide |
| `isTokenValid(token, username)` | Sujet identique et jeton non expiré |
| `isTokenExpired` | `true` si l'expiration est absente ou passée |

La clé est construite par `Keys.hmacShaKeyFor(secret.getBytes(UTF_8))` à partir de `jwt.secret`
(`@Value`).

**`service/CustomUserDetailsService`** (`UserDetailsService`, `@Transactional(readOnly = true)`) renvoie
un `security/UtilisateurConnecte` :

- charge l'`Utilisateur` par `UtilisateurRepository.findByNomUtilisateur`, ou lève
  `UsernameNotFoundException` ;
- charge le clergé actif et son rôle par `ClergeRepository.findAvecRoleById` (`left join fetch`) ;
- `niveau` = `role.niveau`, ou 0 sans clergé ou sans rôle ; `roleNom` = `role.nom` ;
- le compte est désactivé si `actif = false` ou si son clergé est supprimé (`etat = -1`).

**`security/UtilisateurConnecte`** (`UserDetails`) : `nomUtilisateur`, `motDePasse`, `actif`,
`clergeId`, `niveau`, `roleNom`. Autorité unique : `NIVEAU_<n>`.

**Annotations** (`security/`, sur les méthodes des contrôleurs) :

| Annotation | Règle |
|---|---|
| `@LevelRequired(n)` | `niveau >= n` |
| `@NeedAuthorisation(entity = …, id = "#…")` ou `(entityExpression = "#niveau", id = "#…")`, répétable | le clergé du compte est le `responsable_id` de l'entité ciblée. `entity` est une valeur de `EntiteProtegee` (`DIOCESE` … `EGLISE`, `RESSOURCE`) ; `entityExpression` et `id` sont des expressions SpEL sur les paramètres |
| `@CascadeAuthorised` | `@NeedAuthorisation` accepte aussi le responsable d'un **ancêtre** de l'entité |

Toutes les conditions d'une méthode doivent être remplies. Réponses :

- `403` : compte anonyme ou désactivé, niveau insuffisant, `id` nul, code d'entité inconnu, ou entité
  hors du périmètre ;
- `404` : entité ciblée introuvable ou supprimée. `EntiteIntrouvableException` est traduite par
  `AutorisationExceptionHandler` (`@RestControllerAdvice`).

**Mise en œuvre** :

- `config/MethodSecurityConfig` enregistre un `AuthorizationManagerBeforeMethodInterceptor`
  (`@Role(ROLE_INFRASTRUCTURE)`). Son pointcut porte sur les trois annotations, et il est ordonné juste
  après `@PreAuthorize` ;
- `security/PerimetreAuthorizationManager` lit les annotations et évalue le SpEL
  (`MethodBasedEvaluationContext`) ;
- `security/AutorisationService` porte les règles : `estResponsable`, `aLeNiveau`, `racines`,
  `dioceses`. `verifier(entite, id, cascade)` sert aux contrôles faits dans le code, pour le
  rattachement à un nouveau parent ;
- `repository/PerimetreRepository` (SQL natif, lignes actives seulement) :
  - `chaineResponsables(entite, id)` : responsables de l'entité puis de ses ancêtres. Vide si
    l'entité est introuvable ;
  - `racines(clergeId)` : entités dont le clergé est responsable, sans celles qui sont déjà sous une
    autre de ses racines (ressources exclues). Réutilise la CTE `PERIMETRE`
    ([module-tableau-bord.md](module-tableau-bord.md)).

**`controller/MoiController`** : `GET /api/moi` (`@LevelRequired(0)`, tout compte connecté). Il renvoie
`ProfilDTO { nomUtilisateur, clerge, role, niveau, racines }` : c'est le point d'entrée du back-office
pour un responsable. `200` ; `403` sans jeton.

**`controller/AuthController`** (`/api/auth`) :

| Verbe | Route | Corps | Réponses |
|---|---|---|---|
| `POST` | `/login` | `LoginRequest { nomUtilisateur, motDePasse }` | `200 LoginResponse { token, nomUtilisateur, role, niveau }` (`role` = nom du rôle, ex. `Doyen`) ; `401` texte si identifiants incorrects ou compte désactivé |
| `POST` | `/register` | `RegisterRequest { nomUtilisateur, motDePasse, clergeId }` | `@LevelRequired(5)` : `201` texte ; `403` sans jeton ou sous le niveau 5 ; `409` si le nom existe déjà ; `400` si le clergé est introuvable ou supprimé |
| `POST` | `/logout` | — | `200` texte : le serveur ne fait rien, le client supprime son jeton |

`register` encode le mot de passe en BCrypt, crée le compte avec `actif = true` et le rattache au clergé.

**`controller/TestController`** (`/api/test`) : routes de démonstration du sprint 1.
`GET /protected` renvoie le nom et l'autorité. `GET /admin-only` exige `@LevelRequired(5)`. La route
`/protected` est publique par la règle `GET /api/**`, mais exige un jeton **en pratique**. Sans jeton,
le paramètre `Authentication` vaut `null` et `authentication.getName()` lève une
`NullPointerException`. La requête est alors redirigée vers `/error`, qui n'est pas public, et la
réponse est `403` (constaté le 2026-11-20).

**Swagger** (`config/OpenApiConfig`) : déclare le schéma de sécurité `bearerAuth` (HTTP `bearer`,
format `JWT`), sans exigence globale. Chaque méthode protégée porte
`@SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)` : Swagger UI affiche un cadenas
sur ces seules routes, et le bouton « Authorize » envoie le jeton saisi dans `Authorization: Bearer`.

**Modèle de données** :

- `utilisateur` (`nom_utilisateur` unique, `mot_de_passe` BCrypt, `actif`, `clerge_id` unique) ;
- `utilisateur.clerge_id` → `clerge` (un compte au plus par membre du clergé) ;
- `clerge.role_id` → `role` (`nom`, `niveau` NOT NULL, UNIQUE, entre 1 et 5, `description`).

Rôles fournis par `400-data.sql` et par la migration V3 (id = niveau) : `Katekista` (1), `Presbytère`
(2), `Doyen` (3), `Archidiacre` (4), `Évêque` (5) ([entites-sans-api.md](entites-sans-api.md)).

**Contrôle d'accès par méthode** (aucun `@PreAuthorize` ne subsiste) :

| Opération | Annotations |
|---|---|
| Créer un enfant (région, district, paroisse, église, ressource) | `@NeedAuthorisation(entity = <parent>, id = "#dto.<parent>Id") @CascadeAuthorised` |
| Modifier ou supprimer une entité ou une ressource | `@NeedAuthorisation(entity = <entité>, id = "#id") @CascadeAuthorised` (diocèse : sans cascade, il n'a pas d'ancêtre) |
| Changer de parent lors d'un `PUT` | en plus, `AutorisationService.verifier(<parent>, <nouveauId>, true)` dans le contrôleur |
| Créer un diocèse, CRUD du clergé, `POST /api/auth/register`, `PUT /api/geographie/{niveau}/limites`, `GET /api/test/admin-only` | `@LevelRequired(5)` |
| `GET /api/tableau-bord/{niveau}/{id}` et `/statistiques` | `@NeedAuthorisation(entityExpression = "#niveau", id = "#id") @CascadeAuthorised` |
| `GET /api/tableau-bord` (aperçu) | `@LevelRequired(5)`, limité aux diocèses dont le compte est responsable |

Détail des routes et de leurs réponses dans les fichiers de chaque domaine.

## Décisions techniques et raisons

- **JWT sans état** : aucune session serveur, ce qui convient à un back-office Angular et à une
  application mobile.
- **Rôle porté par le clergé, pas par le compte** : un compte est toujours lié à une personne du
  clergé ; son rôle ecclésiastique détermine ses droits.
- **Périmètre déduit de `responsable_id`** (sprint 3) : aucune table d'affectation. Changer le
  responsable d'une entité change les droits, et le périmètre est l'union des sous-arbres dont le
  clergé est responsable.
- **Niveau 5 = le plus haut** : `role.niveau` sert à `@LevelRequired` pour les opérations hors
  hiérarchie (diocèse, clergé, comptes, import QGIS). Le rôle ADMIN est supprimé.
- **Annotations traitées par un `AuthorizationManager`** plutôt que par un aspect AOP : aucune
  dépendance ajoutée, et un refus produit le `403` standard de Spring Security. Le manager reçoit
  `AutorisationService` par `ObjectProvider`, car il est créé avant les repositories JPA.
- **`404` écrit par un `@RestControllerAdvice`** plutôt que par `ResponseStatusException` :
  `sendError` renverrait vers `/error`, qui perd le contexte de sécurité d'une requête sans état.
- **Lectures publiques maintenues** pour le mobile. Seules les vues du back-office (tableau de bord,
  `/api/moi`) exigent un jeton et suivent le périmètre.
- **Lectures publiques** : l'application mobile grand public consulte les données sans compte
  (cahier des charges).
- **Lecture du jeton sans exception** : `JwtService` renvoie `null` ou `false` au lieu de propager une
  `JwtException`. Un jeton expiré ou falsifié est donc traité comme une absence d'authentification
  (testé par `JwtServiceTest`).
- **Sans authentification, la réponse est `403`** : aucun point d'entrée d'authentification n'est
  configuré. `Sprint2ControllerTest.importDesLimitesRefuseSansAuthentification` attend ce `403`.

## État actuel connu

- Connexion, inscription (niveau 5) et déconnexion côté client sont fonctionnelles.
- RBAC du sprint 3 en place : niveaux 1 à 5 et périmètre par responsable. Il est testé par
  `PerimetreAuthorizationManagerTest`, `Sprint3ControllerTest` et `Sprint3PostgisIntegrationTest`
  ([tests.md](tests.md)), et vérifié de bout en bout le 2026-11-20 sur une base neuve.
- **Premier compte** : `/register` exigeant le niveau 5, le premier compte d'un Évêque se crée en SQL.
  Il faut insérer dans `utilisateur` un hash BCrypt et le `clerge_id` d'un clergé de rôle `Évêque`.
- Aucun refresh token, aucune révocation, aucune protection contre la force brute.

## Écarts connus avec le code / dette identifiée

- **`System.out.println` dans `JwtAuthenticationFilter`** : il affiche l'en-tête `Authorization`
  (donc le jeton) et le nom extrait à chaque requête.
- **CORS déclaré deux fois** : `@CrossOrigin` sur `AuthController` (origines fixes, ports 4200 et
  8084) en plus de la configuration globale de `SecurityConfig`.
- `jwt.secret` écrit en clair dans `application.properties` et recopié dans `JwtServiceTest`
  ([generalite.md](generalite.md)).
- **Rollback V3 approximatif** : `V3__sprint3_rbac_rollback.sql` fait les correspondances suivantes.
  L'ancien rôle exact d'un clergé n'est pas conservé :
  - Évêque → Admin ;
  - Archidiacre, Doyen et Presbytère → Curé ;
  - Katekista → Diacre.
- **Le niveau du rôle et le niveau des responsabilités ne sont pas liés** : un clergé de niveau 1
  responsable d'un diocèse gère tout ce diocèse. `@LevelRequired` ne s'applique qu'aux opérations hors
  hiérarchie.
- **Un compte peut se désigner responsable** : un responsable qui modifie son entité peut choisir
  n'importe quel `responsableId` actif, et donc transmettre ses droits sur cette entité.
- **Une exception non traitée sur une requête anonyme sort en `403`, pas en `500`** : l'erreur est
  redirigée vers `/error`, qui exige une authentification. Exemple : `GET /api/test/protected` sans
  jeton. Pour un appelant authentifié, la même exception sort en `500`.
- `PasswordEncoderTest` ne fait aucune assertion : il affiche un hash avec `System.out.println`.

## Dernière mise à jour

2026-11-20 — RBAC du sprint 3 : niveaux 1 à 5, périmètre par `responsable_id`, annotations `@LevelRequired` / `@NeedAuthorisation` / `@CascadeAuthorised`, `GET /api/moi`, `/register` réservé au niveau 5, fin du rôle ADMIN, compte désactivé si le clergé est supprimé.

2026-11-20 — Accès public à Swagger (`permitAll`) et schéma `bearerAuth`. Correction : `/api/test/protected` répond `403` sans jeton (et non 200, comme indiqué à tort).

2026-11-20 — Création du fichier (système documentaire initial).
