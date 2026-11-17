-- =========================================================
-- SPRINT 2 : cartes QGIS stockées en polygones PostGIS et index de consultation
--
-- carte_qgis passe de VARCHAR (chemin d'un fichier .qgz) à GEOMETRY(Polygon, 4326) :
-- la colonne contient désormais directement la limite de la zone.
--   - diocese, district, paroisse : colonne convertie ;
--   - region : colonne ajoutée (la table n'en avait pas).
-- Les anciennes valeurs (chemins de fichiers .qgz) ne sont pas convertibles en polygones :
-- elles sont remplacées par NULL, et les cartes sont à importer depuis QGIS.
--
-- Script idempotent : une colonne déjà convertie n'est jamais retouchée
-- (une seconde exécution ne peut pas effacer les polygones importés).
-- Annulation : V2__sprint2_geographie_statistiques_rollback.sql
-- =========================================================

-- Recherche insensible aux accents (« Diocese » trouve « Diocèse »)
CREATE EXTENSION IF NOT EXISTS unaccent;

-- ---------------------------------------------------------
-- Conversion de carte_qgis en polygone
-- SRID 4326 (WGS 84), identique à eglise.localisation : aucune transformation
-- n'est nécessaire pour les requêtes spatiales.
-- Polygon : chaque zone est d'un seul tenant (une seule surface, trous autorisés).
-- ---------------------------------------------------------
DO $$
DECLARE
    nom_table TEXT;
BEGIN
    FOREACH nom_table IN ARRAY ARRAY['diocese', 'district', 'paroisse'] LOOP
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = current_schema()
                     AND table_name = nom_table
                     AND column_name = 'carte_qgis'
                     AND data_type = 'character varying') THEN
            EXECUTE format(
                'ALTER TABLE %I ALTER COLUMN carte_qgis TYPE GEOMETRY(Polygon, 4326) USING NULL',
                nom_table);
        END IF;
    END LOOP;
END $$;

ALTER TABLE region ADD COLUMN IF NOT EXISTS carte_qgis GEOMETRY(Polygon, 4326);

-- Index spatiaux : églises contenues dans une zone (ST_Covers)
CREATE INDEX IF NOT EXISTS idx_diocese_carte_qgis   ON diocese  USING GIST (carte_qgis);
CREATE INDEX IF NOT EXISTS idx_region_carte_qgis    ON region   USING GIST (carte_qgis);
CREATE INDEX IF NOT EXISTS idx_district_carte_qgis  ON district USING GIST (carte_qgis);
CREATE INDEX IF NOT EXISTS idx_paroisse_carte_qgis  ON paroisse USING GIST (carte_qgis);
CREATE INDEX IF NOT EXISTS idx_eglise_localisation  ON eglise   USING GIST (localisation);

-- ---------------------------------------------------------
-- Index des clés étrangères hiérarchiques.
-- PostgreSQL n'indexe pas automatiquement les clés étrangères ; ces colonnes
-- servent à toutes les jointures de navigation et d'agrégation du tableau de bord.
-- ---------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_region_diocese    ON region    (diocese_id);
CREATE INDEX IF NOT EXISTS idx_district_region   ON district  (region_id);
CREATE INDEX IF NOT EXISTS idx_paroisse_district ON paroisse  (district_id);
CREATE INDEX IF NOT EXISTS idx_eglise_paroisse   ON eglise    (paroisse_id);
CREATE INDEX IF NOT EXISTS idx_ressource_eglise  ON ressource (eglise_id);
