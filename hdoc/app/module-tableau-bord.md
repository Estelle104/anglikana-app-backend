# Tableau de bord et statistiques

## Vue d'ensemble

Le tableau de bord (`/api/tableau-bord`) est réservé au back-office depuis le sprint 3 : jeton requis,
et lecture limitée au périmètre du responsable ([securite-authentification.md](securite-authentification.md)).
Il comprend :

- un **aperçu** des diocèses dont le compte est responsable ;
- le **tableau de bord** d'une entité : ancêtres, enfants, clergé affecté, et la fiche détaillée
  d'une église ;
- les **statistiques** d'une entité.

Tous les comptages sont calculés par PostgreSQL, dans une CTE unique qui aplatit la hiérarchie
(`HierarchieStatistiqueRepository`). L'enum `NiveauHierarchique` est décrite dans
[module-hierarchie.md](module-hierarchie.md).

## Structure / composants

**`TableauBordController`** (`/api/tableau-bord`, tous niveaux : `diocese`, `region`, `district`,
`paroisse`, `eglise`) :

| Verbe | Route | Accès | Réponses |
|---|---|---|---|
| `GET` | `/api/tableau-bord` | niveau 5 | `200 ApercuHierarchieDTO`, limité aux diocèses dont le clergé du compte est responsable (`AutorisationService.dioceses`) / `403` |
| `GET` | `/api/tableau-bord/{niveau}/{id}?niveauEnfants=` | responsable de l'entité ou d'un de ses ancêtres | `200 TableauBordDTO` / `400` si `niveau = ressource` ou `niveauEnfants` inconnu / `403` (dont niveau inconnu) / `404` |
| `GET` | `/api/tableau-bord/{niveau}/{id}/statistiques` | idem | `200 StatistiquesEntiteDTO` / `400` si `niveau = ressource` / `403` / `404` |

Un niveau inconnu donne `403` et non plus `400`, car le contrôle d'accès
(`@NeedAuthorisation(entityExpression = "#niveau", …)`) refuse un code d'entité qu'il ne connaît pas
avant d'appeler le contrôleur.

**`TableauBordService`** :

- `getApercu(dioceseIds)` : totaux des diocèses demandés (`TotauxHierarchieDTO`, clergé compté une
  fois) et un `NoeudHierarchiqueDTO` par diocèse. Une liste vide donne des totaux à 0.
- `getTableauBord(niveau, id, niveauEnfantsDemande)`, qui renvoie :
  - `ancetres` : la chaîne du diocèse jusqu'au parent ;
  - `entite` : le nœud de l'entité, avec ses statistiques et son responsable ;
  - `vuesEnfants` : au plus les **2** niveaux inférieurs (`NOMBRE_VUES_ENFANTS`) ;
  - `niveauEnfants` : le niveau demandé s'il fait partie des vues, sinon le niveau directement
    inférieur ;
  - `enfants` : triés par nom, ordre `Collator` français ;
  - `clerges` : `ClergeAffecteDTO`, membres responsables dans le périmètre, regroupés avec la liste de
    leurs entités ;
  - pour une église uniquement : `eglise` (`EgliseDTO`) et `ressources` (`RessourceDetailleeDTO`).
- `getStatistiques(niveau, id)` : statistiques de l'entité.
- `versStatistiques` (statique, package-private) : ne garde que les comptages des niveaux
  **strictement inférieurs** à l'entité. Les ressources et le clergé sont toujours conservés.

**`HierarchieStatistiqueRepository`** (SQL natif) :

- CTE `PERIMETRE` (package-private, réutilisée par `PerimetreRepository`) : une ligne par entité
  **active** (diocèse → ressource), qui porte `niveau`, `rang`, `id`, `nom`, `responsable_id` et les
  identifiants de tous ses ancêtres (`diocese_id` … `eglise_id`). Chaque table lue ou jointe est
  filtrée sur `etat = 1`.
- Bloc `COMPTAGES` :
  - `COUNT(*) FILTER (WHERE p.niveau = …)` par niveau ;
  - clergé : `COUNT(DISTINCT responsable_id)`, ressources exclues.
- `statistiquesParEntite(niveauGroupe, niveauPerimetre, idPerimetre)` : une ligne par entité du
  niveau de groupement, éventuellement restreinte au périmètre d'une entité.
- `totaux(dioceseIds)` : comptages des diocèses donnés (`p.diocese_id IN (:ids)`), sans requête si la
  liste est vide. L'ancien `totaux()` global est supprimé.
- `responsabilites(niveau, id)` : couples (clergé, nom d'entité) du périmètre, ressources exclues,
  triés par `rang` puis par nom.
- `chaine(niveau, id)` : liste `ReferenceEntiteDTO` du diocèse jusqu'à l'entité ; vide si l'entité
  n'existe pas, ce qui sert de test d'existence.
- Records : `LigneStatistique(id, nom, responsableId, dioceses … clerges)`,
  `Responsabilite(clergeId, entiteNom)`.
- Liste blanche des colonnes : `colonne(niveau)`.

**DTO** :

- `ApercuHierarchieDTO { totaux, dioceses }` ;
- `TableauBordDTO` (voir ci-dessus) ;
- `NoeudHierarchiqueDTO extends ReferenceEntiteDTO { statistiques, responsable }` ;
- `StatistiquesEntiteDTO { regions, districts, paroisses, eglises, ressources, clerges }` ;
- `TotauxHierarchieDTO extends StatistiquesEntiteDTO { dioceses }` ;
- `ReferenceEntiteDTO { niveau, id, nom }`.

## Décisions techniques et raisons

- **Une CTE qui aplatit la hiérarchie** : chaque entité n'apparaît qu'une fois. Les jointures ne
  peuvent donc pas compter deux fois la même entité, et un simple `COUNT(*) FILTER` suffit (Javadoc
  du repository).
- **Règle de comptage du clergé** : membres responsables d'une entité de la hiérarchie (diocèse →
  église), comptés une seule fois. Les responsables de ressources seules sont exclus. Le frontend
  applique la même règle.
- **Une entité ne se compte pas elle-même** : le SQL renvoie des comptages bruts, et
  `versStatistiques` retire le niveau de l'entité et ceux du dessus.
- **Clergé chargé en une requête** : tous les identifiants sont collectés, puis `findAllById` charge
  tout le clergé d'un coup, ce qui évite le N+1.
- **Tri des enfants en Java avec `Collator` français** : ordre alphabétique correct avec accents.

## État actuel connu

Fonctionnalité couverte par trois suites ([tests.md](tests.md)) :

- `TableauBordServiceTest` : règles, sans base ;
- `Sprint2ControllerTest` : routes, codes `400`/`403`/`404`, niveaux en code, périmètre (hors
  périmètre, anonyme, aperçu limité aux diocèses du responsable) ;
- `Sprint2PostgisIntegrationTest` : pas de double comptage, régions vides, somme des enfants égale au
  parent, chaîne hiérarchique, tableau de bord de chaque niveau, aperçu restreint ;
- `Sprint3PostgisIntegrationTest` : statistiques sans les lignes supprimées.

## Écarts connus avec le code / dette identifiée

- Chaque appel exécute la CTE complète sur toute la base, filtrée ensuite. Aucun cache ni mesure de
  performance n'existe sur un volume réel.
- `getTableauBord` exécute plusieurs requêtes : chaîne, entité, enfants, responsabilités, clergé et,
  pour une église, l'église et ses ressources.

## Dernière mise à jour

2026-11-20 — Sprint 3 : tableau de bord réservé au périmètre du responsable (jeton requis), aperçu limité à ses diocèses (niveau 5), filtre `etat = 1` dans la CTE `PERIMETRE`.

2026-11-20 — Création du fichier (système documentaire initial).
