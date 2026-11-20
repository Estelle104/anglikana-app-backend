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

-- =========================================================
-- SPRINT 3 : suppression logique (etat = -1)
-- (voir backoffice/migrations/V3__sprint3_rbac.sql)
-- Index partiels : seules les lignes actives sont parcourues par l'API.
-- =========================================================

CREATE INDEX idx_diocese_responsable_actif   ON diocese   (responsable_id) WHERE etat = 1;
CREATE INDEX idx_region_responsable_actif    ON region    (responsable_id) WHERE etat = 1;
CREATE INDEX idx_district_responsable_actif  ON district  (responsable_id) WHERE etat = 1;
CREATE INDEX idx_paroisse_responsable_actif  ON paroisse  (responsable_id) WHERE etat = 1;
CREATE INDEX idx_eglise_responsable_actif    ON eglise    (responsable_id) WHERE etat = 1;
CREATE INDEX idx_ressource_responsable_actif ON ressource (responsable_id) WHERE etat = 1;
