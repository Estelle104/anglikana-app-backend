# Planning du projet A.C.A

## Sprint 0 (03 août -- 07 août 2026)

**Objectif :** Mise en place du projet.

1.  **Initialisation du projet**
    -   Angular
    -   Spring Boot
    -   Flutter
2.  **Conception de la base de données**

**N.B. :** PostgreSQL obligatoire, utilisation de Supabase au plus tard.

## Sprint 1 (10 août -- 14 août 2026)

### 3. Back Office (React + Spring Boot + PostgreSQL)

**Objectif :** Gestion des données principales.

-   L'administrateur doit s'authentifier pour que la page soit
    sécurisée, car les données sont confidentielles.
-   CRUD pour la gestion des hiérarchies (clergé, diocèses).

#### a. Authentification (JSON Web Token)

1.  Login
2.  Logout
3.  Création de compte

#### b. CRUD

1.  Diocèses
2.  Région
3.  District
4.  Paroisse
5.  Fiangonana
6.  Clergé
7.  Administrateur
8.  Bien / ressources

**N.B. :** Dans l'interface d'insertion d'églises, on peut cliquer sur
une carte pour la saisie des coordonnées, ou bien effectuer une saisie
manuelle à l'aide de deux champs : latitude et longitude.

### 4. Front Office (Dart / Flutter + SQLite)

**Objectif :** Carte mobile.

#### a. Carte interactive

1.  Affichage de la carte.

2.  Barre de recherche d'église et de ressource.

    **N.B. :** Zoom après validation de la recherche.

3.  Filtres :

    -   Diocèse
    -   Région
    -   District
    -   Paroisse
    -   Église (si on filtre par l'église, on affiche les ressources ;
        sinon, les églises uniquement).

## Sprint 2 (17 août -- 21 août 2026)

### 5. Back Office (React + Spring Boot + PostgreSQL)

**Objectif :** Consultation, vérification et visualisation des données
selon la hiérarchie ecclésiastique.

Le Back Office doit permettre à l'administrateur de consulter les
données enregistrées et de vérifier leur cohérence à travers un tableau
de bord hiérarchique.

#### a. Tableau de bord

1.  Nombre d'églises dans chaque diocèse, région, district et paroisse.
2.  Nombre de membres du clergé dans chaque diocèse, région, district et
    paroisse.

**N.B. :** Il y a d'abord une liste des diocèses. Quand on clique sur un
diocèse, il y a deux options :

-   Voir le tableau de bord (3.a).
-   Voir les districts + nombre de districts.
-   ... (récursif).

**Hiérarchie ecclésiastique :**

``` text
Diocèse
   ↓
Régions
   ↓
Districts
   ↓
Paroisses
   ↓
Églises
   ↓
Ressources
```

### 6. Front Office (Dart / Flutter + SQLite)

**Objectif :** Consultation cartographique des données géographiques et
navigation dans la hiérarchie ecclésiastique en mode hors ligne.

L'application mobile doit permettre à l'utilisateur de rechercher et
consulter les différentes entités géographiques à partir d'une carte
interactive.

#### a. Carte interactive

1.  Rechercher un diocèse, une région ou un district.
    -   Affichage de la carte QGIS après validation de la recherche.
    -   Il faut que les églises soient dans le polygone du QGIS.

## Sprint 3 (16 novembre -- 20 novembre 2026)

**Objectif :** Gestion des responsabilités et consultation détaillée des
données ecclésiastiques.

### 7. Back Office (React + Spring Boot + PostgreSQL)

#### a. Gestion des rôles

1.  **Administrateurs possibles (par ordre) :**
    -   Évêque (responsable diocèse)
    -   Archidiacre (responsable région)
    -   Doyen (responsable district)
    -   Presbytère (responsable paroisse)
    -   Katekista (responsable église)
2.  **Attribution :**
    -   L'Évêque gère son propre diocèse et tout ce qui se trouve dans
        ce diocèse.
    -   ...

### 8. Front Office (Dart / Flutter + SQLite)

#### a. Carte interactive

1.  **Affichage des détails de chaque église :**

    -   Nom
    -   Localisation
    -   Photo
    -   Historique
    -   Horaires des cultes (plusieurs)
    -   Responsable actuel
    -   Page Facebook

    **N.B. :** Affichage des détails après avoir cliqué sur une église.

2.  **Affichage des détails des membres du clergé :**

    -   Nom
    -   Photo
    -   Téléphone
    -   Email

    **N.B. :** Affichage des détails après avoir cliqué sur un membre du
    clergé dans le détail de l'église.
