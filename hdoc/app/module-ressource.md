# Ressources et types de ressource

## Vue d'ensemble

Une **ressource** est une infrastructure anglicane rattachée à une église : école, dispensaire, toby,
institut… (cahier des charges). Elle a un **type** (`type_ressource`) et, au choix, un responsable
du clergé. Ce fichier couvre le CRUD des ressources et la lecture des types. La liste des ressources
d'une église (`GET /api/eglises/{id}/ressources`) est décrite dans
[module-hierarchie.md](module-hierarchie.md). La fiche détaillée d'une église est décrite dans
[module-tableau-bord.md](module-tableau-bord.md).

## Structure / composants

**Entités** :

| Entité | Table | Champs |
|---|---|---|
| `Ressource` | `ressource` | `nom` (obligatoire, 150), `adresse`, `localisation` (Point 4326), `type` (obligatoire), `eglise` (obligatoire, `ON DELETE CASCADE`), `responsable` (facultatif), `etat` (suppression logique, comme la hiérarchie : [module-hierarchie.md](module-hierarchie.md)) |
| `TypeRessource` | `type_ressource` | `nom` (obligatoire, 100) |

**DTO** :

- `RessourceDTO { id, nom, adresse, typeId, egliseId, responsableId }` : pas de localisation ;
- `TypeRessourceDTO { id, nom }` ;
- `RessourceDetailleeDTO { ressource, typeNom, responsable }`, utilisé uniquement par le tableau de bord.

Mappers : `RessourceMapper`, `TypeRessourceMapper`.

**`RessourceController`** (`/api/ressources`) :

| Verbe | Route | Accès | Réponses |
|---|---|---|---|
| `GET` | `/api/ressources` | public | `200` liste des ressources actives |
| `GET` | `/api/ressources/{id}` | public | `200` / `404` (introuvable ou supprimée) |
| `POST` | `/api/ressources` | responsable de l'église `egliseId` ou d'un de ses ancêtres | `201` `RessourceDTO` / `403` (`egliseId` absent compris) / `404` église introuvable |
| `PUT` | `/api/ressources/{id}` | responsable de la ressource ou d'un de ses ancêtres ; un changement d'église exige aussi d'être responsable de la nouvelle | `200` / `403` / `404` |
| `DELETE` | `/api/ressources/{id}` | idem (`PUT`) | `204` (passage à `etat = -1`) / `403` / `404` |

Règles d'accès : [securite-authentification.md](securite-authentification.md). Une église ne peut être
supprimée que lorsque toutes ses ressources le sont.

`appliquerDTO` résout `typeId`, `egliseId` et `responsableId` par `findById`. Un identifiant absent ou
inconnu donne `null`.

**`TypeRessourceController`** (`/api/types-ressource`, lecture seule, via `HierarchieService`) :

| Verbe | Route | Accès | Réponses |
|---|---|---|---|
| `GET` | `/api/types-ressource` | public | `200` liste triée par nom (`findAllByOrderByNomAsc`) |
| `GET` | `/api/types-ressource/{id}` | public | `200` / `404` |

**Repository** : `RessourceRepository` fournit :

- `findByEgliseIdOrderByNomAsc` ;
- `findDetailleesByEgliseId`, en JPQL `join fetch r.type left join fetch r.responsable`, qui charge
  type et responsable en une seule requête.

## Décisions techniques et raisons

- **`join fetch` pour la fiche d'une église** : il évite une requête par ressource (N+1) lors de la
  lecture du type et du responsable (commentaire de `RessourceRepository`).
- **Types en lecture seule par l'API** : le référentiel des types n'a pas d'écriture exposée.

## État actuel connu

CRUD fonctionnel. `findDetailleesByEgliseId` est testé par
`Sprint2PostgisIntegrationTest.tableauBordDeLEgliseContientSesRessourcesDetaillees`. Le CRUD n'est pas
testé.

## Écarts connus avec le code / dette identifiée

- **`localisation` n'est ni exposée ni saisissable** : la colonne existe (Point 4326) mais
  `RessourceDTO` ne la contient pas, et `appliquerDTO` ne l'écrit jamais.
- **Types de ressource non gérables par l'API** : `400-data.sql` fournit `École` et `Dispensaire`,
  mais aucune route ne permet d'en ajouter. Un nouveau type demande une insertion SQL.
- **Type absent ou inconnu ⇒ erreur 500** : la contrainte `NOT NULL` est violée et l'erreur n'est pas
  traduite. Contrairement aux contrôleurs de la hiérarchie, `RessourceController` n'a aucun
  `try/catch`. Une église absente (`403`) ou inconnue (`404`) est désormais refusée par le contrôle
  d'accès, avant l'écriture.

## Dernière mise à jour

2026-11-22 — `400-data.sql` fournit 2 types de ressource et 1 ressource (église 1) : l'écart « aucun type à l'initialisation » est reformulé.

2026-11-20 — Sprint 3 : accès par responsable (église ou ancêtre), suppression logique (`etat`).

2026-11-20 — Création du fichier (système documentaire initial).
