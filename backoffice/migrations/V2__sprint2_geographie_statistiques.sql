-- =========================================================
-- SPRINT 2 : limites géographiques (QGIS) et index de consultation
-- Script idempotent et non destructif : aucune donnée existante n'est modifiée.
-- Annulation : V2__sprint2_geographie_statistiques_rollback.sql
-- =========================================================

-- Recherche insensible aux accents (« Diocese » trouve « Diocèse »)
CREATE EXTENSION IF NOT EXISTS unaccent;

-- ---------------------------------------------------------
-- Limites géographiques des zones (polygones exportés de QGIS)
-- SRID 4326 (WGS 84), identique à eglise.localisation : aucune transformation
-- n'est nécessaire pour les requêtes spatiales.
-- MultiPolygon : une zone peut être composée de plusieurs parties (îles...).
-- ---------------------------------------------------------
ALTER TABLE diocese  ADD COLUMN IF NOT EXISTS limite GEOMETRY(MultiPolygon, 4326);
ALTER TABLE region   ADD COLUMN IF NOT EXISTS limite GEOMETRY(MultiPolygon, 4326);
ALTER TABLE district ADD COLUMN IF NOT EXISTS limite GEOMETRY(MultiPolygon, 4326);

-- Index spatiaux : églises contenues dans une zone (ST_Covers)
CREATE INDEX IF NOT EXISTS idx_diocese_limite       ON diocese  USING GIST (limite);
CREATE INDEX IF NOT EXISTS idx_region_limite        ON region   USING GIST (limite);
CREATE INDEX IF NOT EXISTS idx_district_limite      ON district USING GIST (limite);
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
