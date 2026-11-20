# Cahier des charges -- A.C.A

## 1. Informations générales

-   **Nom du projet :** Anglican Church Application
-   **Responsable :** MoraTek
-   **Propriétaire (tâche) :** Randy Robinson
-   **Date de début :** 01/08/2026
-   **Date de fin prévue :** 31/12/2027
-   **Priorité :** Moyenne

## 2. Contexte et objectifs

### Contexte

A.C.A nous montre où sont toutes les églises Anglicanes à Madagascar.
Tous les malgaches peuvent l'utiliser, même dans une zone suburbaine
sans réseau internet, car l'application est hors ligne sur mobile.

### Objectif principal

L'évangélisation sera plus facile.

### Objectifs secondaires

-   Connaître les coordonnées réelles des églises et infrastructures
    anglicanes à Madagascar, ainsi que leurs détails.

## 3. Périmètre

### Ce qui est inclus dans le projet

-   Gestion des diocèses, districts, paroisses, églises (fiangonana) et
    utilisateurs.
-   Gestion du clergé et de leurs affectations.
-   Gestion des infrastructures anglicanes (écoles, dispensaires, toby,
    instituts, etc.).
-   Carte interactive hors ligne (recherche de lieux, suggestion des
    fiangonana à proximité).
-   Intégration d'une carte QGIS liée aux diocèses / districts /
    paroisses.
-   Menu de contribution permettant l'ajout de contenus par les
    personnes autorisées.
-   Tableau de bord.

### Ce qui est exclu du projet

*Aucun élément n'est précisé dans le document.*

## 4. Livrables attendus

  -----------------------------------------------------------------------
  Livrable                Description             Date prévue
  ----------------------- ----------------------- -----------------------
  Application mobile      Application             21/12/2026
                          fonctionnelle côté      
                          utilisateur             

  Application web         Application côté        31/10/2026
                          administrateur          

  Base de données         Base de données         01/09/2026
                          contenant les           
                          coordonnées réelles des 
                          églises et des clergés  

  Documentation technique Documentation           24/12/2026
                          expliquant              
                          l'architecture, les     
                          technologies utilisées  
                          et l'installation.      

  Manuel utilisateur      Guide expliquant        24/12/2026
                          comment utiliser        
                          l'application.          

  Rapport de projet       Rapport présentant la   ---
                          réalisation et les      
                          résultats               
  -----------------------------------------------------------------------

## 5. Exigences / fonctionnalités (à terminer d'ici novembre)

-   Gérer plusieurs diocèses, avec la possibilité d'en ajouter de
    nouveaux si besoin.
-   Possibilité de représenter la structure comme un organigramme des
    églises (optionnel).
-   Inclure l'ensemble des entités et biens rattachés au diocèse :
    fiangonana (église), école, dispensaire, toby, institut, etc.

### Carte interactive

-   Recherche de lieux.
-   Suggestion / affichage des fiangonana à proximité, etc.
-   Barre de recherche et filtres (diocèse, district, paroisse,
    fiangonana, clergé, et autres types de biens/ressources).

### Carte de navigation type « Maps »

-   Mode en ligne possible, mais l'objectif principal est le mode hors
    ligne.
-   Intégration d'une carte QGIS et liaison avec diocèse / paroisse /
    district.
-   Sert à visualiser les limites et les zones/lieux à visiter ensuite.
-   Si possible : menus dédiés par type d'entité (voir section
    suivante).

### Gestion (CRUD)

CRUD (Create Read Update Delete) : églises, clergés, diocèses,
districts, paroisses, utilisateurs.

### Tableau de bord

-   Tableau de bord.

## 6. Menus / fiches par entité (champs attendus)

### Diocèse

  Champ                           Description
  ------------------------------- -------------------------------------------------
  Anarana (nom)                   Nom du diocèse.
  Carte QGIS                      Carte des limites géographiques du diocèse.
  Présentation                    Description / présentation générale du diocèse.
  Coordonnées                     Coordonnées de contact du diocèse.
  Eveka (évêque)                  Nom de l'évêque responsable.
  Liste des districts rattachés   Districts appartenant au diocèse.

### District

  Champ                 Description
  --------------------- ----------------------------------------------
  Anarana (nom)         Nom du district.
  Carte QGIS            Carte des limites géographiques du district.
  Responsable           Personne responsable du district.
  Liste des paroisses   Paroisses appartenant au district.

### Paroisse

  -----------------------------------------------------------------------
  Champ                               Description
  ----------------------------------- -----------------------------------
  Anarana (nom)                       Nom de la paroisse.

  Carte QGIS                          Carte des limites géographiques de
                                      la paroisse.

  Responsable                         Personne responsable de la
                                      paroisse.

  Fiangonana et autres biens/entités  Liste des églises et biens
  rattachés                           rattachés à la paroisse.
  -----------------------------------------------------------------------

### Fiangonana (Église)

  Champ                          Description
  ------------------------------ ------------------------------------------
  Anarana (nom)                  Nom de l'église.
  Localisation                   Position géographique (coordonnées GPS).
  Sary (photos)                  Photos de l'église.
  Historique                     Historique de l'église.
  Horaire de culte               Horaires des cultes.
  Responsable actuel             Nom du responsable actuel.
  Page Facebook (si existante)   Lien vers la page Facebook.
  « M'y rendre »                 Fonction d'itinéraire, comme sur Maps.

### Clergé

  -----------------------------------------------------------------------
  Champ                               Description
  ----------------------------------- -----------------------------------
  Anarana (nom)                       Nom du membre du clergé.

  Sary (photo)                        Photo du membre du clergé.

  Toerana iasana amin'izao (lieu      Lieu d'affectation actuel.
  d'affectation actuel)               

  Téléphone                           Numéro de téléphone.

  Email                               Adresse email.

  Rattachement à une paroisse /       Paroisse ou église de rattachement.
  fiangonana                          

  Gestion des affectations            En cas de changement, mise à jour
                                      simple via modification de
                                      l'affectation.
  -----------------------------------------------------------------------

## 7. Contribution / ajout de données (si possible)

-   Menu « Contribution » pour permettre l'ajout de contenus (lieux +
    photos) par les personnes autorisées, surtout le clergé.
-   Ajouts soumis à validation avant publication (pour éviter les
    erreurs).
-   Possibilité permanente d'ajouter : fiangonana, lieux, clergé, etc.

## 8. Visibilité (en ligne)

Quand la personne consulte en ligne, afficher clairement :

-   De quoi il s'agit (le projet).
-   Si le contenu est « validé sur place » / approuvé (exemple : « Kate
    tsy azo tafo »).

## 9. Contraintes et risques

### Contraintes

#### Stockage

-   Les photos des églises peuvent rapidement occuper plusieurs
    gigaoctets.
-   Il faudra optimiser la taille des images.

#### Précision des coordonnées GPS

-   Les coordonnées des églises doivent être exactes.
-   Certaines églises rurales ne sont pas référencées sur les cartes
    existantes.

#### Configuration minimale du téléphone

  -----------------------------------------------------------------------
  Élément                             Configuration minimale
  ----------------------------------- -----------------------------------
  Système d'exploitation              Android 8.0 (Oreo) ou version
                                      supérieure

  Processeur                          Quad-Core 1,8 GHz ou équivalent

  Mémoire vive (RAM)                  3 Go

  Stockage disponible                 500 Mo pour l'application + 1 à 2
                                      Go pour les cartes et les données
                                      hors ligne

  Écran                               5 pouces minimum, résolution HD
                                      (720 × 1280)

  GPS                                 GPS intégré

  Connexion Internet                  Non requise pour l'utilisation
                                      normale (uniquement pour les mises
                                      à jour éventuelles)
  -----------------------------------------------------------------------

### Risques identifiés

  -----------------------------------------------------------------------
  Risque            Impact            Probabilité       Mesures de
                                                        prévention
  ----------------- ----------------- ----------------- -----------------
  Données           Élevé             Élevée            Faire valider les
  incomplètes ou                                        données par
  incorrectes sur                                       chaque diocèse
  les églises                                           avant leur
                                                        intégration.

  Difficulté à      Élevé             Moyenne           Utiliser des
  obtenir les                                           relevés GPS sur
  coordonnées GPS                                       le terrain ou des
  des églises                                           outils de
  rurales                                               cartographie pour
                                                        compléter les
                                                        données.

  Absence de cartes Élevé             Moyenne           Créer les limites
  officielles des                                       géographiques à
  diocèses et                                           partir de
  districts                                             documents
                                                        officiels ou de
                                                        relevés validés
                                                        par l'Église.

  Volume important  Moyen             Moyenne           Compresser les
  des images et                                         images et
  documents                                             optimiser le
                                                        stockage.

  Difficulté        Moyen             Moyenne           Concevoir une
  d'utilisation par                                     interface simple,
  des utilisateurs                                      intuitive et
  peu familiarisés                                      fournir un guide
  avec                                                  d'utilisation.
  l'informatique                                        

  Erreurs de        Moyen             Moyenne           Vérifier les
  localisation sur                                      coordonnées GPS
  la carte                                              avant publication
                                                        et permettre leur
                                                        correction.
  -----------------------------------------------------------------------

## 10. Critères de succès

  -----------------------------------------------------------------------
  Critère de succès       Indicateur de réussite  État
  ----------------------- ----------------------- -----------------------
  Fonctionnement hors     Toutes les              
  ligne                   fonctionnalités         
                          principales sont        
                          accessibles sans        
                          connexion Internet.     

  Localisation des        Toutes les églises      
  églises                 enregistrées sont       
                          correctement            
                          positionnées sur la     
                          carte.                  

  Recherche efficace      Un utilisateur retrouve 
                          une église en moins de  
                          10 secondes par nom,    
                          diocèse, district ou    
                          paroisse.               

  Consultation des        Chaque église possède   
  informations            une fiche complète      
                          (nom, localisation,     
                          diocèse, district,      
                          paroisse, photos,       
                          clergé, etc.).          

  Affichage               Les cartes des          
  cartographique          diocèses, districts et  
                          emplacements des        
                          églises s'affichent     
                          correctement.           

  Facilité d'utilisation  Les utilisateurs        
                          peuvent consulter les   
                          informations sans       
                          formation approfondie.  

  Performance             L'application démarre   
                          en moins de 10 secondes 
                          et les recherches       
                          s'effectuent            
                          rapidement.             

  Fiabilité des données   Les informations        
                          affichées sont validées 
                          par les responsables de 
                          l'Église.               

  Sécurité                Seuls les               
                          administrateurs         
                          autorisés peuvent       
                          modifier les données.   

  Stabilité               L'application           
                          fonctionne sans         
                          plantage lors d'une     
                          utilisation normale.    

  Satisfaction des        Les utilisateurs jugent 
  utilisateurs            l'application utile,    
                          simple et fiable lors   
                          des tests.              

  Contribution            Le clergé et les        
  communautaire           personnes autorisées    
                          peuvent soumettre de    
                          nouvelles données pour  
                          validation.             
  -----------------------------------------------------------------------

## 11. Budget

### Budget total estimé

  ------------------------------------------------------------------------
  Poste de dépense      Description                      Coût estimé (MGA)
  --------------------- --------------------- ----------------------------
  Logiciel              Logiciel de                                      0
                        développement         

  Déplacements          Collecte des                               500 000
                        coordonnées GPS et    
                        des informations sur  
                        les églises           

  Connexion Internet    Téléchargement des                         125 000
                        cartes, documentation 
                        et mises à jour       

  Documentation         Impression du mémoire                       10 000
                        et des documents      
                        techniques            

  Tests                 Déplacements pour les                      200 000
                        essais dans les       
                        diocèses ou paroisses 

  Imprévus              Marge pour les                             100 000
                        dépenses non prévues  

  **Total estimé**                                         **935 000 MGA**
  ------------------------------------------------------------------------

## 12. Technologies

### Technologies de l'application mobile (Utilisateurs)

  -----------------------------------------------------------------------
  Composant                           Technologie proposée
  ----------------------------------- -----------------------------------
  Langage                             Dart

  Framework                           Flutter

  Cartographie                        MapLibre Flutter ou Flutter Map
                                      avec OpenStreetMap / QGIS

  Base de données locale              SQLite

  Géolocalisation                     Geolocator

  Stockage des images                 Local (dans l'application ou le
                                      stockage du téléphone)

  Fonctionnement                      Hors ligne (Offline First)
  -----------------------------------------------------------------------

### Technologies de l'application Web (Administrateurs)

  Composant                 Technologie
  ------------------------- -------------------------
  Front-end                 React.js
  Framework CSS             Tailwind CSS
  Cartographie              Leaflet + OpenStreetMap
  Gestion des formulaires   React Hook Form
  Authentification          JWT (JSON Web Token)

### Backend

  Composant   Technologie proposée
  ----------- -----------------------------
  Langage     Java
  Framework   Spring Boot
  API         REST API
  Sécurité    Spring Security + JWT
  ORM         Spring Data JPA / Hibernate

### Base de données

  Composant         Technologie proposée
  ----------------- ----------------------
  Base de données   PostgreSQL
  Extension SIG     PostGIS

### Serveur

  Composant               Technologie proposée
  ----------------------- ---------------------------------------
  Serveur d'application   Apache Tomcat (intégré à Spring Boot)

### Justification des technologies

-   **Flutter :** une seule base de code pour Android (et iOS si besoin
    plus tard), avec de bonnes performances.
-   **React :** interface d'administration moderne, rapide et facile à
    maintenir.
-   **Spring Boot :** backend robuste, sécurisé et largement utilisé
    dans les projets professionnels.
-   **PostgreSQL + PostGIS :** référence pour les applications SIG grâce
    à ses capacités de gestion des données géographiques.
-   **SQLite :** permet à l'application mobile de fonctionner
    entièrement hors ligne, tout en synchronisant les données lorsqu'une
    connexion Internet est disponible.
-   **QGIS :** utilisé pour créer et gérer les cartes des limites de
    diocèses, districts et paroisses, intégrées ensuite à la carte de
    navigation.
