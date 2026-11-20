# Clergé

## Vue d'ensemble

CRUD des membres du clergé (`/api/clerges`). Un membre du clergé a un rôle (`Role`) et peut être
responsable d'entités de la hiérarchie et de ressources (`responsable_id` dans `diocese`, `region`,
`district`, `paroisse`, `eglise` et `ressource`). Il peut aussi être lié à un compte utilisateur, et
son rôle détermine alors les droits de ce compte ([securite-authentification.md](securite-authentification.md)).

## Structure / composants

**Entité `Clerge`** (table `clerge`) : `nom` (obligatoire, 100), `prenom`, `dateNaissance`
(`LocalDate`), `hierarchie` (texte libre, ex. « Prêtre », « Diacre »), `telephone`, `email`, `role`
(`@ManyToOne` LAZY vers `Role`, facultatif), `etat` (1 = actif, -1 = supprimé ; `@SQLRestriction` et
`@SQLDelete`, comme la hiérarchie : [module-hierarchie.md](module-hierarchie.md)).

`ClergeRepository.findAvecRoleById` charge un clergé actif et son rôle en une requête, pour
l'utilisateur connecté ([securite-authentification.md](securite-authentification.md)).

**`ClergeDTO`** : mêmes champs ; le rôle est exposé par `roleId`. Conversion par
`ClergeMapper.toDTO`.

**`ClergeController`** (`/api/clerges`) :

| Verbe | Route | Accès | Réponses |
|---|---|---|---|
| `GET` | `/api/clerges` | public | `200` liste des membres actifs |
| `GET` | `/api/clerges/{id}` | public | `200` / `404` (introuvable ou supprimé) |
| `POST` | `/api/clerges` | niveau 5 | `201` `ClergeDTO` / `403` |
| `PUT` | `/api/clerges/{id}` | niveau 5 | `200` / `403` / `404` |
| `DELETE` | `/api/clerges/{id}` | niveau 5 | `204` (passage à `etat = -1`) / `403` / `404` / `409` texte s'il est encore responsable d'une entité ou d'une ressource active |

Le `409` vient de `HierarchieService.estResponsableActif` (`existsByResponsableId` sur les six
repositories). Un clergé supprimé ne donne plus aucun droit, et son compte ne peut plus se connecter.

`appliquerDTO` recopie les champs et résout `roleId` par `RoleRepository.findById`. Un rôle inconnu
est mis à `null` sans erreur.

**Usage par d'autres domaines** :

- `ClergeAffecteDTO { clerge, responsabilites }` : liste des entités dont un membre du clergé est
  responsable, construite par le tableau de bord ([module-tableau-bord.md](module-tableau-bord.md)) ;
- `RessourceDetailleeDTO.responsable` : responsable d'une ressource ([module-ressource.md](module-ressource.md)) ;
- `TableauBordService.chargerClerges` : chargement groupé par `findAllById`.

## Décisions techniques et raisons

- **`hierarchie` (texte) et `role` (table) sont distincts** : `hierarchie` est le titre
  ecclésiastique affiché, `role` porte les droits applicatifs.
- **Responsable facultatif partout** : une entité peut exister sans responsable (`responsable_id`
  nullable, sans `ON DELETE`).
- **Le rôle porte les droits** (sprint 3) : `roleId` fixe le niveau (5 Évêque … 1 Katekista). Le CRUD
  du clergé est donc réservé au niveau 5.

## État actuel connu

CRUD fonctionnel, sans test dédié. Les rôles ne sont pas exposés par l'API : aucun contrôleur pour
`Role`, les identifiants sont ceux de `400-data.sql` ([entites-sans-api.md](entites-sans-api.md)).

## Écarts connus avec le code / dette identifiée

- Pas de validation : `nom` obligatoire en base, mais non vérifié en Java ; format de l'`email` non
  contrôlé.
- Les données personnelles (téléphone, e-mail, date de naissance) sont exposées par un `GET` public.

## Dernière mise à jour

2026-11-20 — Sprint 3 : CRUD réservé au niveau 5, suppression logique (`etat`), refus `409` d'un responsable actif (corrige l'erreur 500), `findAvecRoleById`.

2026-11-20 — Création du fichier (système documentaire initial).
