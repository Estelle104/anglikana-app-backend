# Entités sans API

## Vue d'ensemble

Ce fichier liste les tables qui ont une entité JPA et un repository, mais **aucun contrôleur**. Elles
préparent des fonctionnalités du cahier des charges pas encore développées : menu de contribution,
photos, horaires de culte. Le rôle (`Role`) est aussi là : il est utilisé par le clergé et la
sécurité, mais n'est pas exposé. Dès qu'une de ces entités reçoit un contrôleur, elle quitte ce
fichier pour un fichier de domaine (voir la règle de frontière dans [INDEX.md](INDEX.md)).

## Structure / composants

| Entité | Table | Champs | Repository | Appelants |
|---|---|---|---|---|
| `Role` | `role` | `nom` (obligatoire), `niveau` (NOT NULL, UNIQUE, 1 à 5 ; porte les droits, 5 = le plus haut), `description` | `RoleRepository` | `ClergeController` (résolution de `roleId`), `CustomUserDetailsService` (via `ClergeRepository.findAvecRoleById`) |
| `HoraireCulte` | `horaire_culte` | `jour` (texte), `libelle`, `debut` / `fin` (`LocalTime`), `eglise` (obligatoire, `ON DELETE CASCADE`) | `HoraireCulteRepository` | aucun |
| `Contribution` | `contribution` | `typeEntite` + `entiteId` (référence polymorphe), `action`, `statut`, `commentaire`, `dateContribution` (par défaut maintenant), `dateValidation`, `utilisateur` (obligatoire), `validateur` | `ContributionRepository` | aucun |
| `Photo` | `photo` | `url` (obligatoire), `description`, `dateAjout` (par défaut aujourd'hui), `typeEntite` (`eglise`, `ressource` ou `clerge`) + `entiteId` | `PhotoRepository` | aucun |

Index de `300-index.sql` : `idx_photo_entite` et `idx_contribution_entite` sur
(`type_entite`, `entite_id`).

Données de `400-data.sql` :

- `role` (id = niveau) : `Katekista` (1), `Presbytère` (2), `Doyen` (3), `Archidiacre` (4), `Évêque`
  (5). Usage : [securite-authentification.md](securite-authentification.md) ;
- `horaire_culte` : 4 horaires pour les 2 églises ;
- `contribution`, `photo` : aucune donnée.

## Décisions techniques et raisons

- **Références polymorphes** (`type_entite` + `entite_id`) pour `photo` et `contribution` : une même
  table sert à plusieurs entités cibles, sans une clé étrangère par cible. En contrepartie,
  l'intégrité de la référence n'est garantie par aucune contrainte (commentaire de `002-table.sql`).
- **`Contribution` a un circuit de validation** : un auteur (`utilisateur`), un `validateur` et un
  `statut` portent le menu de contribution du cahier des charges, réservé aux personnes autorisées.

## État actuel connu

- Aucune route n'expose les horaires, les contributions ou les photos.
- Les horaires de culte existent en base mais ne figurent pas dans `EgliseDTO`.

## Écarts connus avec le code / dette identifiée

- `Contribution.dateContribution` est un `LocalDateTime` et la colonne un `TIMESTAMP` sans fuseau.
- `HoraireCulte.jour` est un texte libre (`Dimanche`, `Vendredi`…), sans contrainte de valeur.
- `photo.url` ne précise pas le lieu de stockage des fichiers : aucun mécanisme d'envoi n'existe.

## Dernière mise à jour

2026-11-20 — Sprint 3 : rôles Katekista (1) … Évêque (5), `role.niveau` obligatoire et unique.

2026-11-20 — Création du fichier (système documentaire initial).
