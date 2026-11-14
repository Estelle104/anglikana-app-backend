-- =========================================================
-- ANNULATION du script V2__sprint2_geographie_statistiques.sql
-- carte_qgis redevient un VARCHAR(255), vide (NULL).
-- ATTENTION : les polygones importés sont perdus. Les exporter au préalable si nécessaire :
--   GET /api/geographie/{niveau}/limites
-- =========================================================

DROP INDEX IF EXISTS idx_ressource_eglise;
DROP INDEX IF EXISTS idx_eglise_paroisse;
DROP INDEX IF EXISTS idx_paroisse_district;
DROP INDEX IF EXISTS idx_district_region;
DROP INDEX IF EXISTS idx_region_diocese;

DROP INDEX IF EXISTS idx_eglise_localisation;
DROP INDEX IF EXISTS idx_paroisse_carte_qgis;
DROP INDEX IF EXISTS idx_district_carte_qgis;
DROP INDEX IF EXISTS idx_region_carte_qgis;
DROP INDEX IF EXISTS idx_diocese_carte_qgis;

-- La région n'avait pas de carte avant le Sprint 2.
ALTER TABLE region DROP COLUMN IF EXISTS carte_qgis;

DO $$
DECLARE
    nom_table TEXT;
BEGIN
    FOREACH nom_table IN ARRAY ARRAY['diocese', 'district', 'paroisse'] LOOP
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = current_schema()
                     AND table_name = nom_table
                     AND column_name = 'carte_qgis'
                     AND udt_name = 'geometry') THEN
            EXECUTE format('ALTER TABLE %I ALTER COLUMN carte_qgis TYPE VARCHAR(255) USING NULL', nom_table);
        END IF;
    END LOOP;
END $$;

-- L'extension unaccent est conservée : elle peut être utilisée ailleurs.
