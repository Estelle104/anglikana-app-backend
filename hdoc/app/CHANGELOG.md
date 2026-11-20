# CHANGELOG — hdoc/app

> Ajout en fin de fichier uniquement : une nouvelle entrée par changement, jamais de réécriture.

## 2026-11-20 — Création des conventions de code
Fichier créé : `conventions-code.md`. Relevé du style de code observé dans `backoffice/src`,
`database/` et `backoffice/migrations/`, avec la liste des incohérences (§15).

## 2026-11-20 — Mise en place du système documentaire
Structure inspirée de `note_backoffice/docs/analyse` (projet note_all) : un INDEX lu en premier, des
fichiers de détail par domaine au plan commun à 6 sections, et ce CHANGELOG.

Fichiers créés : `INDEX.md`, `CHANGELOG.md`, `generalite.md`, `structure-backoffice.md`,
`securite-authentification.md`, `module-hierarchie.md`, `module-clerge.md`, `module-ressource.md`,
`module-geographie.md`, `module-tableau-bord.md`, `entites-sans-api.md`, `tests.md`.

Fichiers existants conservés tels quels comme fichiers de référence (hors plan commun) :
`stack-technique.md`, `conventions-code.md`.

Raison : rendre la documentation adressable par domaine, pour lire et mettre à jour uniquement ce
qui concerne la tâche. Le protocole est défini dans `CLAUDE.md` §2.

## 2026-11-20 — Renvoi vers l'INDEX dans les fichiers de référence
Fichiers impactés : `stack-technique.md`, `conventions-code.md`. Ajout d'une ligne « Voir aussi :
INDEX.md » dans l'en-tête de chacun, pour les relier au système documentaire. Contenu inchangé par
ailleurs.

## 2026-11-20 — Swagger / OpenAPI (springdoc 3.1.1)
Fichiers impactés : `generalite.md`, `stack-technique.md`, `securite-authentification.md`,
`structure-backoffice.md`, `module-geographie.md`, `conventions-code.md`. Changements :
- ajout de la dépendance springdoc et de `config/OpenApiConfig` (schéma `bearerAuth`) ;
- accès public à `/swagger-ui/**`, `/swagger-ui.html` et `/v3/api-docs/**` ;
- annotation des 56 endpoints, et convention d'annotation des contrôleurs.

Corrections de la documentation existante :
- `/api/test/protected` répond `403` sans jeton (il n'est pas public en pratique) ;
- une exception non traitée sur une requête anonyme sort en `403` ;
- `EgliseController` déclare sa propre constante `SRID_GPS`.

## 2026-11-20 — Convention Swagger rendue obligatoire
Fichier impacté : `conventions-code.md` (§7, sous-section « Documentation Swagger »). Ajout d'un renvoi
vers `CLAUDE.md` §3.1 (version 1.1), qui impose désormais de documenter dans Swagger tout endpoint
créé ou modifié, dans la même tâche.

## 2026-11-20 — Sprint 3 : RBAC par niveau et périmètre, suppression logique
Fichiers impactés : `securite-authentification.md`, `module-hierarchie.md`, `module-clerge.md`,
`module-ressource.md`, `module-geographie.md`, `module-tableau-bord.md`, `entites-sans-api.md`,
`generalite.md`, `structure-backoffice.md`, `tests.md`, `conventions-code.md`. Changements :
- fin du rôle ADMIN : rôles Katekista (1) … Évêque (5) portés par `role.niveau` ; périmètre déduit de
  `responsable_id` (entité et descendance) ;
- annotations `@LevelRequired`, `@NeedAuthorisation`, `@CascadeAuthorised` (package `security/`),
  `config/MethodSecurityConfig`, `PerimetreRepository`, route `GET /api/moi` (57 endpoints) ;
- `/api/auth/register`, CRUD du clergé, création de diocèse et import QGIS réservés au niveau 5 ;
  tableau de bord réservé au périmètre du responsable ;
- suppression logique (`etat` = 1 / -1) sur la hiérarchie, les ressources et le clergé ; refus `409`
  tant qu'une fille directe est active, ou qu'un clergé est responsable actif ;
- migration `V3__sprint3_rbac.sql` et son rollback ; scripts `database/` mis à jour ;
- nouvelles suites de tests (104 tests).

Écarts retirés parce que le code les corrige : `/register` public, rôle mis en majuscules avec ses
accents, suppression en cascade sans confirmation, suppression d'un clergé référencé ⇒ 500. Écarts
reformulés : parent absent ou inconnu (hiérarchie, ressource). Écarts ajoutés : rollback V3
approximatif, niveau du rôle indépendant des responsabilités, transmission des droits par
`responsableId`, aucune restauration d'une ligne supprimée.

## 2026-11-20 — Sprint 3 : référence technique et INDEX
Fichiers impactés : `stack-technique.md` (§4 Sécurité : règles d'URL, RBAC par annotations, règle
« protéger toute nouvelle route d'écriture ») et `INDEX.md` (périmètre de code de
`securite-authentification.md` : `security/**`, `config/MethodSecurityConfig`,
`controller/MoiController`, `repository/PerimetreRepository`, `dto/ProfilDTO`).
