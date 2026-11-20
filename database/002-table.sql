-- =========================================================
-- TABLE : ROLE
-- =========================================================
-- niveau : 5 = Évêque (diocèse) ... 1 = Katekista (église). Il porte les droits applicatifs.
CREATE TABLE role (
    id          SERIAL PRIMARY KEY,
    nom         VARCHAR(100) NOT NULL,
    niveau      INT NOT NULL,
    description TEXT,
    CONSTRAINT uk_role_niveau UNIQUE (niveau),
    CONSTRAINT ck_role_niveau CHECK (niveau BETWEEN 1 AND 5)
);

-- =========================================================
-- TABLE : CLERGE
-- =========================================================
CREATE TABLE clerge (
    id             SERIAL PRIMARY KEY,
    nom            VARCHAR(100) NOT NULL,
    prenom         VARCHAR(100),
    date_naissance DATE,
    hierarchie     VARCHAR(100),
    telephone      VARCHAR(30),
    email          VARCHAR(150),
    role_id        INT,
    etat           SMALLINT NOT NULL DEFAULT 1,  -- 1 = actif, -1 = supprimé (suppression logique)
    CONSTRAINT fk_clerge_role
        FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT ck_clerge_etat CHECK (etat IN (1, -1))
);

-- =========================================================
-- TABLE : DIOCESE
-- =========================================================
CREATE TABLE diocese (
    id              SERIAL PRIMARY KEY,
    nom             VARCHAR(150) NOT NULL,
    carte_qgis      GEOMETRY(Polygon, 4326),  -- limite de la zone, importée depuis QGIS
    presentation    TEXT,
    coordonnees     VARCHAR(100),
    responsable_id  INT,
    etat            SMALLINT NOT NULL DEFAULT 1,  -- 1 = actif, -1 = supprimé (suppression logique)
    CONSTRAINT fk_diocese_responsable
        FOREIGN KEY (responsable_id) REFERENCES clerge(id),
    CONSTRAINT ck_diocese_etat CHECK (etat IN (1, -1))
);

-- =========================================================
-- TABLE : REGION
-- =========================================================
CREATE TABLE region (
    id              SERIAL PRIMARY KEY,
    nom             VARCHAR(150) NOT NULL,
    carte_qgis      GEOMETRY(Polygon, 4326),  -- limite de la zone, importée depuis QGIS
    diocese_id      INT NOT NULL,
    responsable_id  INT,
    etat            SMALLINT NOT NULL DEFAULT 1,  -- 1 = actif, -1 = supprimé (suppression logique)
    CONSTRAINT fk_region_diocese
        FOREIGN KEY (diocese_id) REFERENCES diocese(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_region_responsable
        FOREIGN KEY (responsable_id) REFERENCES clerge(id),
    CONSTRAINT ck_region_etat CHECK (etat IN (1, -1))
);

-- =========================================================
-- TABLE : DISTRICT
-- =========================================================
CREATE TABLE district (
    id              SERIAL PRIMARY KEY,
    nom             VARCHAR(150) NOT NULL,
    carte_qgis      GEOMETRY(Polygon, 4326),  -- limite de la zone, importée depuis QGIS
    region_id       INT NOT NULL,
    responsable_id  INT,
    etat            SMALLINT NOT NULL DEFAULT 1,  -- 1 = actif, -1 = supprimé (suppression logique)
    CONSTRAINT fk_district_region
        FOREIGN KEY (region_id) REFERENCES region(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_district_responsable
        FOREIGN KEY (responsable_id) REFERENCES clerge(id),
    CONSTRAINT ck_district_etat CHECK (etat IN (1, -1))
);

-- =========================================================
-- TABLE : PAROISSE
-- =========================================================
CREATE TABLE paroisse (
    id              SERIAL PRIMARY KEY,
    nom             VARCHAR(150) NOT NULL,
    carte_qgis      GEOMETRY(Polygon, 4326),  -- limite de la zone, importée depuis QGIS
    district_id     INT NOT NULL,
    responsable_id  INT,
    etat            SMALLINT NOT NULL DEFAULT 1,  -- 1 = actif, -1 = supprimé (suppression logique)
    CONSTRAINT fk_paroisse_district
        FOREIGN KEY (district_id) REFERENCES district(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_paroisse_responsable
        FOREIGN KEY (responsable_id) REFERENCES clerge(id),
    CONSTRAINT ck_paroisse_etat CHECK (etat IN (1, -1))
);

-- =========================================================
-- TABLE : EGLISE
-- =========================================================
CREATE TABLE eglise (
    id              SERIAL PRIMARY KEY,
    nom             VARCHAR(150) NOT NULL,
    adresse         VARCHAR(255),
    localisation    GEOMETRY(Point, 4326),
    historique      TEXT,
    lien_facebook   VARCHAR(255),
    paroisse_id     INT NOT NULL,
    responsable_id  INT,
    etat            SMALLINT NOT NULL DEFAULT 1,  -- 1 = actif, -1 = supprimé (suppression logique)
    CONSTRAINT fk_eglise_paroisse
        FOREIGN KEY (paroisse_id) REFERENCES paroisse(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_eglise_responsable
        FOREIGN KEY (responsable_id) REFERENCES clerge(id),
    CONSTRAINT ck_eglise_etat CHECK (etat IN (1, -1))
);

-- =========================================================
-- TABLE : HORAIRE_CULTE
-- =========================================================
CREATE TABLE horaire_culte (
    id          SERIAL PRIMARY KEY,
    jour        VARCHAR(20),
    libelle     VARCHAR(150),
    debut       TIME,
    fin         TIME,
    eglise_id   INT NOT NULL,
    CONSTRAINT fk_horaire_eglise
        FOREIGN KEY (eglise_id) REFERENCES eglise(id)
        ON DELETE CASCADE
);

-- =========================================================
-- TABLE : TYPE_RESSOURCE
-- =========================================================
CREATE TABLE type_ressource (
    id   SERIAL PRIMARY KEY,
    nom  VARCHAR(100) NOT NULL
);

-- =========================================================
-- TABLE : RESSOURCE
-- =========================================================
CREATE TABLE ressource (
    id              SERIAL PRIMARY KEY,
    nom             VARCHAR(150) NOT NULL,
    adresse         VARCHAR(255),
    localisation    GEOMETRY(Point, 4326),
    type_id         INT NOT NULL,
    eglise_id       INT NOT NULL,
    responsable_id  INT,
    etat            SMALLINT NOT NULL DEFAULT 1,  -- 1 = actif, -1 = supprimé (suppression logique)
    CONSTRAINT fk_ressource_type
        FOREIGN KEY (type_id) REFERENCES type_ressource(id),
    CONSTRAINT fk_ressource_eglise
        FOREIGN KEY (eglise_id) REFERENCES eglise(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ressource_responsable
        FOREIGN KEY (responsable_id) REFERENCES clerge(id),
    CONSTRAINT ck_ressource_etat CHECK (etat IN (1, -1))
);

-- =========================================================
-- TABLE : UTILISATEUR
-- =========================================================
CREATE TABLE utilisateur (
    id              SERIAL PRIMARY KEY,
    nom_utilisateur VARCHAR(100) NOT NULL UNIQUE,
    mot_de_passe    VARCHAR(255) NOT NULL,
    actif           BOOLEAN DEFAULT TRUE,
    clerge_id       INT UNIQUE,
    CONSTRAINT fk_utilisateur_clerge
        FOREIGN KEY (clerge_id) REFERENCES clerge(id)
);

-- =========================================================
-- TABLE : CONTRIBUTION
-- =========================================================
CREATE TABLE contribution (
    id                  SERIAL PRIMARY KEY,
    type_entite         VARCHAR(50) NOT NULL,
    entite_id           INT NOT NULL,
    action              VARCHAR(50),
    date_contribution   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut              VARCHAR(50),
    commentaire         TEXT,
    date_validation     TIMESTAMP,
    utilisateur_id      INT NOT NULL,
    validateur_id       INT,
    CONSTRAINT fk_contribution_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id),
    CONSTRAINT fk_contribution_validateur
        FOREIGN KEY (validateur_id) REFERENCES utilisateur(id)
);

-- =========================================================
-- TABLE : PHOTO
-- (polymorphe : type_entite + entite_id -> eglise, ressource ou clerge)
-- =========================================================
CREATE TABLE photo (
    id           SERIAL PRIMARY KEY,
    url          VARCHAR(255) NOT NULL,
    description  VARCHAR(255),
    date_ajout   DATE DEFAULT CURRENT_DATE,
    type_entite  VARCHAR(50) NOT NULL,   -- 'eglise', 'ressource', 'clerge'
    entite_id    INT NOT NULL
);
