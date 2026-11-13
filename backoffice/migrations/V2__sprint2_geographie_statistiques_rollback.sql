-- =========================================================
-- ANNULATION du script V2__sprint2_geographie_statistiques.sql
-- ATTENTION : la suppression des colonnes « limite » efface les polygones importés.
-- Exporter les limites au préalable si nécessaire :
--   GET /api/geographie/{niveau}/limites
-- =========================================================

DROP INDEX IF EXISTS idx_ressource_eglise;
DROP INDEX IF EXISTS idx_eglise_paroisse;
DROP INDEX IF EXISTS idx_paroisse_district;
DROP INDEX IF EXISTS idx_district_region;
DROP INDEX IF EXISTS idx_region_diocese;

DROP INDEX IF EXISTS idx_eglise_localisation;
DROP INDEX IF EXISTS idx_district_limite;
DROP INDEX IF EXISTS idx_region_limite;
DROP INDEX IF EXISTS idx_diocese_limite;

ALTER TABLE district DROP COLUMN IF EXISTS limite;
ALTER TABLE region   DROP COLUMN IF EXISTS limite;
ALTER TABLE diocese  DROP COLUMN IF EXISTS limite;

-- L'extension unaccent est conservée : elle peut être utilisée ailleurs.
