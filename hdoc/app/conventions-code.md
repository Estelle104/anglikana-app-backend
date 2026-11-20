# Conventions de code — anglikana-app-backend

> Conventions **observées** dans le code existant (`backoffice/src`, `database/`, `backoffice/migrations/`),
> relevées le 2026-11-20 sur la branche `features/sprint-3-RBAC`.
> Ce document décrit ce que fait le code, pas un idéal : les écarts entre fichiers sont listés en fin de document.
> Voir aussi : [stack-technique.md](stack-technique.md), [INDEX.md](INDEX.md) (fichier de référence de la documentation `hdoc/app`).

---

## 1. Langue

| Élément | Langue | Exemples |
|---|---|---|
| Vocabulaire métier (classes, champs, variables) | **Français** | `Paroisse`, `Clerge`, `nomUtilisateur`, `motDePasse`, `carteQgis` |
| Méthodes métier et helpers | **Français** | `appliquerDTO`, `rechercher`, `importerLimites`, `depuisCode`, `estGeographique`, `versGeoJson` |
| Méthodes CRUD des contrôleurs | **Anglais** | `getAll`, `getById`, `create`, `update`, `delete` |
| Préfixes techniques | Anglais + nom français | `getRegionsDuDiocese`, `getEglisesDeLaParoisse`, `toDTO` |
| Code JWT/sécurité (hérité d'exemples Spring) | Anglais | `generateToken`, `extractUsername`, `buildToken` |
| Javadoc, commentaires, messages d'erreur, noms de tests | **Français**, accents compris | `"Clergé introuvable pour l'id fourni"` |

Les noms métier gardent la grammaire française : `getDistrictsDeLaRegion`, `getRessourcesDeLEglise`, `regionsDUnDiocese`.
Les accents sont retirés des identifiants Java (`Eglise`, `Clerge`, `Diocese`) mais conservés dans les textes.

---

## 2. Architecture et packages

Package racine : `com.anglikana.backoffice`, découpage **par couche technique** :

```
config/       SecurityConfig, JwtAuthenticationFilter, MethodSecurityConfig, OpenApiConfig
controller/   @RestController, un par entité (+ GeographieController, TableauBordController, MoiController)
dto/          Objets exposés en JSON (+ enum NiveauHierarchique)
entity/       Entités JPA
mapper/       Conversion entité → DTO (classes statiques)
repository/   Spring Data JPA + repositories SQL natif (EntityManager)
security/     Annotations d'accès, utilisateur connecté, contrôle par niveau et par périmètre
service/      Logique métier, lecture hiérarchique, statistiques, JWT
```

Flux : `Controller → (Service) → Repository → Entity`, puis `Mapper.toDTO` pour la réponse.

- Les **CRUD simples** passent directement du contrôleur au repository (pas de service) : voir [ParoisseController.java](../../backoffice/src/main/java/com/anglikana/backoffice/controller/ParoisseController.java).
- La **logique transverse** (hiérarchie, géographie, tableau de bord) est dans un `@Service`.
- Les entités ne sortent jamais d'un contrôleur : on renvoie toujours un DTO.

---

## 3. Injection de dépendances

**Injection par constructeur explicite**, champs `private final`. Pas de `@Autowired` sur les champs, pas de `@RequiredArgsConstructor` dans le code de production.

```java
private final ParoisseRepository paroisseRepository;
private final DistrictRepository districtRepository;

public ParoisseController(ParoisseRepository paroisseRepository,
                           DistrictRepository districtRepository) {
    this.paroisseRepository = paroisseRepository;
    this.districtRepository = districtRepository;
}
```

Paramètres du constructeur alignés les uns sous les autres, un par ligne.
Exception : `JwtService` lit sa configuration par `@Value("${jwt.secret}")` sur champ.

---

## 4. Entités JPA (`entity/`)

Modèle type : [Paroisse.java](../../backoffice/src/main/java/com/anglikana/backoffice/entity/Paroisse.java).

- **Lombok** sur une seule ligne : `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`.
- Imports groupés : `jakarta.persistence.*`, `lombok.*`.
- `@Table(name = "...")` toujours explicite, nom de table en minuscules singulier (`paroisse`, `utilisateur`).
- Clé : `Integer id` avec `@GeneratedValue(strategy = GenerationType.IDENTITY)` (colonnes `SERIAL`).
- `@Column(name = "snake_case")` dès que le nom Java est en camelCase ; contraintes reportées (`nullable`, `length`, `unique`).
- Relations : `@ManyToOne(fetch = FetchType.LAZY)` systématique, `@JoinColumn(name = "xxx_id")`. Le responsable d'une entité s'appelle `responsable` (→ `Clerge`, colonne `responsable_id`).
- Valeurs par défaut : `@Builder.Default` (ex. `Boolean actif = true`).
- Géométrie : types JTS (`org.locationtech.jts.geom.Polygon`) avec `columnDefinition = "geometry(Polygon,4326)"`.
- **Suppression logique** (sprint 3, hiérarchie, ressource, clergé) : `@SQLRestriction("etat = 1")` et
  `@SQLDelete(sql = "UPDATE <table> SET etat = -1 WHERE id = ?")` sous `@Table`, champ
  `@Builder.Default @ColumnDefault("1") @Column(nullable = false) Short etat = 1` en dernier. `etat`
  n'est jamais exposé dans un DTO.
- Javadoc `/** ... */` d'une ligne sur les champs non évidents.

---

## 5. DTO (`dto/`)

Modèle type : [ParoisseDTO.java](../../backoffice/src/main/java/com/anglikana/backoffice/dto/ParoisseDTO.java).

- **Pas de Lombok** : getters/setters écrits à la main.
- Style récent (sprint 2) : getter et setter **sur une ligne chacun**, groupés par champ :
  ```java
  public String getNom() { return nom; }
  public void setNom(String nom) { this.nom = nom; }
  ```
- Suffixe `DTO` pour les réponses (`ParoisseDTO`, `TableauBordDTO`), `Request` / `Response` pour l'authentification et les entrées spécifiques (`LoginRequest`, `ImportLimitesRequest`).
- Les relations sont exposées par **identifiant** : `districtId`, `responsableId` (pas d'objet imbriqué).
- Géométrie exposée en `tools.jackson.databind.JsonNode` (GeoJSON, Jackson 3).
- Les types imbriqués propres au DTO sont des `public static class` internes (`ImportLimitesRequest.Feature`).
- Les enums partagés avec le frontend sont dans `dto/` et sérialisés par code via `@JsonValue` (voir [NiveauHierarchique.java](../../backoffice/src/main/java/com/anglikana/backoffice/dto/NiveauHierarchique.java)).

---

## 6. Mappers (`mapper/`)

Modèle type : [ParoisseMapper.java](../../backoffice/src/main/java/com/anglikana/backoffice/mapper/ParoisseMapper.java).

- Classe `public final` avec constructeur `private` vide : **utilitaire statique**, pas de bean Spring, pas de MapStruct.
- Méthode `public static XxxDTO toDTO(Xxx entite)`.
- Relations nulles gérées par ternaire : `paroisse.getDistrict() != null ? paroisse.getDistrict().getId() : null`.
- Le sens **DTO → entité** n'est pas dans le mapper : il est fait dans le contrôleur par une méthode privée `appliquerDTO(entite, dto)`, qui résout les identifiants via les repositories.
- Conversions de format : préfixes `versXxx` / `depuisXxx` (`GeometrieMapper.versGeoJson`, `GeometrieMapper.depuisGeoJson`).
- Utilisés en référence de méthode : `.map(ParoisseMapper::toDTO)`.

---

## 7. Contrôleurs (`controller/`)

### Routes
- `@RequestMapping("/api/<entite-au-pluriel>")` en minuscules, mots séparés par `-` : `/api/paroisses`, `/api/tableau-bord`, `/api/types-ressource`.
- Sous-ressources hiérarchiques : `/api/paroisses/{id}/eglises`, `/api/dioceses/{id}/regions`.
- Paramètres « niveau » passés en texte (`diocese`, `region`…) et convertis par `NiveauHierarchique.depuisCode`.

### Squelette CRUD

| Opération | Signature | Réponses |
|---|---|---|
| Liste | `List<XxxDTO> getAll()` | 200 |
| Détail | `ResponseEntity<XxxDTO> getById(@PathVariable Integer id)` | 200 / 404 |
| Création | `ResponseEntity<?> create(@RequestBody XxxDTO dto)` | 201 / 400 |
| Modification | `ResponseEntity<?> update(@PathVariable Integer id, @RequestBody XxxDTO dto)` | 200 / 400 / 404 |
| Suppression | `ResponseEntity<?> delete(@PathVariable Integer id)` | 204 / 404 |

- Variables : `xxxOptionnel` pour un `Optional`, `saved` / `updated` pour le résultat de `save`.
- Codes HTTP écrits en numérique : `ResponseEntity.status(201)`, `status(401)`, `status(409)`.
- Lorsque le service renvoie un `Optional`, enchaînement fonctionnel :
  ```java
  return service.getZone(niveau, id)
          .<ResponseEntity<?>>map(ResponseEntity::ok)
          .orElse(ResponseEntity.notFound().build());
  ```
- Les validations récurrentes sont factorisées en méthodes privées en bas de classe (`niveauGeographique`, `niveauGeographiqueInvalide`).
- Limites métier en constantes `private static final int` en MAJUSCULES (`LIMITE_RECHERCHE_MAX`).

### Sécurité (depuis le sprint 3)
- Plus de `@PreAuthorize` ni de rôle ADMIN. Chaque méthode protégée porte les annotations de `security/`
  (règles complètes dans [securite-authentification.md](securite-authentification.md)) :
  - créer un enfant : `@NeedAuthorisation(entity = EntiteProtegee.<PARENT>, id = "#dto.<parent>Id")` + `@CascadeAuthorised` ;
  - modifier ou supprimer : `@NeedAuthorisation(entity = EntiteProtegee.<ENTITE>, id = "#id")` + `@CascadeAuthorised` ;
  - opération hors hiérarchie : `@LevelRequired(5)` ;
  - changement de parent dans un `update` : `autorisationService.verifier(EntiteProtegee.<PARENT>, dto.get<Parent>Id(), true)`
    si l'identifiant diffère du parent actuel.
- `DELETE` d'une entité de la hiérarchie : `404` si absente, puis `409` texte si
  `hierarchieService.refusSuppression(...)` renvoie un motif, sinon `deleteById` (logique) et `204`.
- Lectures `GET /api/**` publiques (règle globale dans [SecurityConfig.java](../../backoffice/src/main/java/com/anglikana/backoffice/config/SecurityConfig.java)),
  sauf `/api/tableau-bord/**` et `/api/moi`, déclarées `authenticated` avant cette règle.
- JWT sans état ; le claim `role` porte le nom du rôle (`Doyen`…). Le principal est un
  `UtilisateurConnecte`, injecté par `@AuthenticationPrincipal` (avec `@Parameter(hidden = true)`).

### Documentation Swagger (depuis le 2026-11-20)
> Convention **imposée** par `CLAUDE.md` §3.1 pour tout endpoint créé ou modifié, et pas seulement observée.

Chaque contrôleur et chaque endpoint sont annotés avec `io.swagger.v3.oas.annotations.*`, textes en français :
- `@Tag(name, description)` sur la classe, avec un nom de domaine au pluriel et accentué (« Diocèses », « Églises », « Tableau de bord »).
- `@Operation(summary, description)` sur chaque méthode. Le `summary` commence par un verbe à l'infinitif (« Lister les… », « Consulter un… »). La `description` précise l'accès et les règles non évidentes. Formules employées :
  - « Lecture publique, éléments supprimés (etat = -1) exclus. » ;
  - « Réservé au niveau 5 (Évêque). » ;
  - « Réservé au responsable de/du <entité> ou de l'un de ses ancêtres. » ;
  - à la création : « Réservé au responsable de/du <parent> <parent>Id ou de l'un de ses ancêtres. »
- `403` décrit la cause réelle : « Jeton absent ou niveau 5 requis », « Jeton absent ou hors du
  périmètre de … » (ajouter « <parent>Id absent » à la création). `404` couvre aussi le parent
  introuvable produit par `@NeedAuthorisation`, et `409` le refus de suppression (texte).
- `@ApiResponse` : un par code **réellement renvoyé par le code Java**, jamais un code théorique.
  - Avec un retour `ResponseEntity<?>`, le succès précise son schéma : `content = @Content(schema = @Schema(implementation = XxxDTO.class))`, ou `array = @ArraySchema(...)` pour une liste.
  - Les erreurs en texte brut (`400`, `401`, `409`) utilisent `@Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))`.
  - Les réponses sans corps (`204`, `403`, `404`) utilisent `content = @Content`.
- `@SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)` sur chaque méthode portant `@LevelRequired` ou `@NeedAuthorisation`, et sur toute route qui exige un jeton en pratique.
- `@Parameter(description = ...)` sur les `@PathVariable` et `@RequestParam`. Les descriptions répétées dans une classe sont des constantes `private static final String DESCRIPTION_…` (`GeographieController`, `TableauBordController`).
- Aucune annotation `@Schema` sur les DTO : les schémas sont déduits des getters.

---

## 8. Gestion des erreurs

- **Un seul `@RestControllerAdvice`** (`security/AutorisationExceptionHandler`), pour une seule exception
  personnalisée : `EntiteIntrouvableException` → `404` sans corps. Elle est levée par le contrôle d'accès
  quand l'entité ciblée n'existe pas. Les autres erreurs suivent les règles ci-dessous.
- Refus d'accès : `AccessDeniedException` (ou décision négative du manager) → `403` par Spring Security.
- Erreur de saisie → `throw new IllegalArgumentException("message en français")` dans le mapper ou le service.
- Le contrôleur l'attrape localement et renvoie le message brut en texte :
  ```java
  try {
      appliquerDTO(paroisse, dto);
  } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
  }
  ```
- Absence de ressource → `Optional.empty()` dans le service, transformé en 404 par le contrôleur. Les services documentent : « renvoie un Optional vide lorsque l'entité parente n'existe pas ».
- Les messages d'erreur sont des phrases complètes destinées à l'utilisateur, avec la valeur fautive et la valeur attendue :
  `"Niveau géographique inconnu : " + code + " (attendu : diocese, region ou district)"`.
- Pas de Bean Validation (`@Valid`, `@NotNull`) bien que la dépendance soit présente : la validation est manuelle.
- Pas de logging (aucun `Logger` / `@Slf4j`).

---

## 9. Services (`service/`)

Modèle type : [HierarchieService.java](../../backoffice/src/main/java/com/anglikana/backoffice/service/HierarchieService.java).

- `@Service` + `@Transactional(readOnly = true)` au niveau classe **ou** méthode ; `@Transactional` simple pour les écritures (`importerLimites`).
- Retour `Optional<List<XxxDTO>>` quand le parent peut ne pas exister (vérifié par `existsById` d'abord).
- Les services renvoient déjà des DTO (le mapping est fait dans le service).
- Streams : `.stream().map(XxxMapper::toDTO).toList()`.
- Javadoc de classe expliquant la règle métier (ex. la chaîne Diocèse → Région → District → Paroisse → Église → Ressources), `@throws` documenté quand une exception est levée.
- Méthodes de calcul pures en `static` package-private pour être testées sans base (`TableauBordService.versStatistiques`).

---

## 10. Repositories (`repository/`)

- Spring Data : `@Repository public interface XxxRepository extends JpaRepository<Xxx, Integer>`.
- Requêtes dérivées, tri explicite dans le nom : `findByDistrictIdOrderByNomAsc`, `findAllByOrderByNomAsc`, `existsByNomUtilisateur`.
- Requêtes complexes (statistiques, PostGIS) : **classe** `@Repository` avec `EntityManager` et **SQL natif** dans des *text blocks* `"""` en constantes `private static final String` (voir [HierarchieStatistiqueRepository.java](../../backoffice/src/main/java/com/anglikana/backoffice/repository/HierarchieStatistiqueRepository.java)).
- Lignes de résultat SQL : `public record` internes (`LigneStatistique`, `LigneZone`, `Responsabilite`) — seul endroit où les records sont utilisés.
- Noms de colonnes injectés dans le SQL uniquement depuis une liste blanche indexée par l'enum ; la Javadoc le précise.
- `switch` moderne (`case X ->`, `default -> throw ...`).

---

## 11. Mise en forme

- Indentation **4 espaces** en Java (le `pom.xml` et `BackofficeApplication.java` générés par Spring Initializr utilisent des tabulations).
- Accolade ouvrante en fin de ligne ; `if/else` toujours avec accolades.
- Imports : projet `com.anglikana...` d'abord, ligne vide, librairies tierces (`org...`, `jakarta...`), ligne vide, `java.*`. Wildcards acceptés pour `org.springframework.web.bind.annotation.*`, `jakarta.persistence.*`, `lombok.*`.
- Chaînage de streams : coupure après le receveur, indentation de 8 espaces.
- Séparateurs de sections dans les longs fichiers de test : `// ------------------------------------------------------------------ Titre`.
- Pas de ligne vide finale imposée (beaucoup de fichiers se terminent sans saut de ligne).

---

## 12. Tests (`src/test`)

- JUnit 5, Mockito, AssertJ (`assertThat`), MockMvc.
- **Noms de méthodes = phrase française en camelCase, sans préfixe `test`** : `uneEntiteNeCompteQueSesDescendants`, `zoneEnPlusieursPartiesRefusee`, `regionsDUnDioceseInexistant`.
- Classes de test et méthodes en visibilité package (pas de `public`).
- `@BeforeEach void preparer()` pour l'initialisation.
- Trois niveaux :
  - **unitaire** : `mock(...)` à la main et instanciation directe du service ([TableauBordServiceTest.java](../../backoffice/src/test/java/com/anglikana/backoffice/service/TableauBordServiceTest.java)) ;
  - **web** : `@WebMvcTest(controllers = {...})` + `@Import({SecurityConfig.class, MethodSecurityConfig.class, AutorisationService.class})` + `@MockitoBean` (dont `PerimetreRepository`), et `@WithUtilisateurConnecte(clergeId, niveau)` pour un utilisateur connecté (`@WithMockUser` ne porte ni clergé ni niveau) ;
  - **intégration PostGIS** : `@DataJpaTest` sur la vraie base (`Replace.NONE`, `ddl-auto=none`), transaction annulée en fin de test, jeu de données construit par une classe dédiée (`JeuDonneesHierarchie`).
- Champs injectés sur une ligne : `@Autowired private MockMvc mockMvc;`, `@MockitoBean private XxxService xxxService;`.
- Tests regroupés par sprint (`sprint2/`, `sprint3/`) ou par couche (`service/`, `mapper/`, `security/`).
- Javadoc d'une ligne sur la classe de test pour dire ce qu'elle couvre.

---

## 13. SQL (`database/`, `backoffice/migrations/`)

- Scripts d'init numérotés par centaine : `001-init.sql`, `002-table.sql`, `300-index.sql`, `400-data.sql`.
- Migrations façon Flyway (appliquées à la main) : `V<n>__sprint<n>_<sujet>.sql` + `V<n>__sprint<n>_<sujet>_rollback.sql` (V2 géographie, V3 RBAC).
- Mots-clés SQL en MAJUSCULES, tables et colonnes en `snake_case` minuscules singulier.
- Clé `id SERIAL PRIMARY KEY`, clés étrangères `<table>_id`, contraintes nommées `fk_<table>_<cible>`, `ck_<table>_<colonne>` (CHECK), `uk_<table>_<colonne>` (UNIQUE).
- Requêtes natives : filtrer `etat = 1` sur **chaque** table lue ou jointe, car `@SQLRestriction` ne s'applique pas au SQL natif.
- En-tête de bloc :
  ```sql
  -- =========================================================
  -- TABLE : DIOCESE
  -- =========================================================
  ```
- Migrations **idempotentes** (`IF NOT EXISTS`, blocs `DO $$ ... $$`) et commentées en français (pourquoi, effet, annulation).
- Géométries en SRID 4326 (WGS 84).

---

## 14. Git

- Branches : `main`, `dev`, `features/sprint-<n>-<sujet>` (ex. `features/sprint-3-RBAC`) ; anciennes branches `sprint1-crud`, `sprint1-Auth`.
- Intégration par Pull Request GitHub (`Merge pull request #n from ...`).
- Messages de commit **sans convention stable** : quelques `feat: ...` en anglais, beaucoup de messages d'un mot (`m`, `o`, `paroisse`, `doc`).

---

## 15. Écarts et incohérences relevés

À trancher si l'équipe veut une convention unique :

| Sujet | Variante A | Variante B |
|---|---|---|
| Fin de stream | `.collect(Collectors.toList())` (contrôleurs, `getAll`) | `.toList()` (services) |
| Lecture d'un `Optional` | `isPresent()` / `get()` / `else` (CRUD sprint 1) | `.map(...).orElse(...)` (sprint 2) |
| Format des getters DTO | multi-lignes avec constructeurs (`LoginRequest`, `LoginResponse`) | une ligne par accesseur (DTO sprint 2) |
| CORS | `@CrossOrigin` sur `AuthController` | configuration globale `corsConfigurationSource()` |
| Langue des méthodes | `generateToken`, `extractUsername` | `appliquerDTO`, `rechercher` |
| Langue des annotations de sécurité | `@LevelRequired`, `@NeedAuthorisation`, `@CascadeAuthorised` (anglais, choix de l'équipe au sprint 3) | classes françaises du même package (`AutorisationService`, `EntiteProtegee`) |
| Nom des tests | `expiredTokenShouldBeRejectedGracefully`, `testerBCrypt` | phrase française camelCase |
| Indentation | tabulations (fichiers générés) | 4 espaces |

Autres points notables :
- Secrets en clair dans `application.properties` (`jwt.secret`, mot de passe PostgreSQL) alors qu'un `.env` existe.
- `spring.jpa.hibernate.ddl-auto=update` en configuration principale, alors que le schéma est géré par scripts SQL.
- `TestController` (`/api/test/...`) est un contrôleur de démonstration du sprint 1.
