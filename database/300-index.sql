-- =========================================================
-- INDEX : relations polymorphes, hiérarchie et données spatiales
-- =========================================================

-- Index utiles pour les relations polymorphes
CREATE INDEX idx_photo_entite ON photo (type_entite, entite_id);
CREATE INDEX idx_contribution_entite ON contribution (type_entite, entite_id);

-- =========================================================
-- SPRINT 2 : recherche, index hiérarchiques et spatiaux
-- (voir backoffice/migrations/V2__sprint2_geographie_statistiques.sql)
-- =========================================================

CREATE INDEX idx_region_diocese    ON region    (diocese_id);
CREATE INDEX idx_district_region   ON district  (region_id);
CREATE INDEX idx_paroisse_district ON paroisse  (district_id);
CREATE INDEX idx_eglise_paroisse   ON eglise    (paroisse_id);
CREATE INDEX idx_ressource_eglise  ON ressource (eglise_id);

CREATE INDEX idx_diocese_carte_qgis  ON diocese  USING GIST (carte_qgis);
CREATE INDEX idx_region_carte_qgis   ON region   USING GIST (carte_qgis);
CREATE INDEX idx_district_carte_qgis ON district USING GIST (carte_qgis);
CREATE INDEX idx_paroisse_carte_qgis ON paroisse USING GIST (carte_qgis);
CREATE INDEX idx_eglise_localisation ON eglise   USING GIST (localisation);
