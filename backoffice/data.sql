
-- =========================================================
-- DATA : DIOCESE
-- =========================================================
INSERT INTO diocese (id, nom, carte_qgis, presentation, coordonnees, responsable_id) VALUES
(1, 'Diocèse d''Antananarivo', 'maps/diocese_tana.qgz', 'Diocèse métropolitain fondé à la fin du XIXe siècle.', '-18.8792, 47.5079', 1),
(2, 'Diocèse de Toamasina', 'maps/diocese_toamasina.qgz', 'Diocèse couvrant la région côtière de l''Est.', '-18.1492, 49.4023', 2);

-- =========================================================
-- DATA : REGION
-- =========================================================
INSERT INTO region (id, nom, diocese_id, responsable_id) VALUES
(1, 'Région Analamanga Centre', 1, 1),
(2, 'Région Atsinanana Littoral', 2, 2);

-- =========================================================
-- DATA : DISTRICT
-- =========================================================
INSERT INTO district (id, nom, carte_qgis, region_id, responsable_id) VALUES
(1, 'District de Tana Ville', 'maps/district_tana_ville.qgz', 1, 3),
(2, 'District de Toamasina Urbain', 'maps/district_toamasina_urbain.qgz', 2, 4);

-- =========================================================
-- DATA : PAROISSE
-- =========================================================
INSERT INTO paroisse (id, nom, carte_qgis, district_id, responsable_id) VALUES
(1, 'Paroisse Saint-Joseph', 'maps/paroisse_st_joseph.qgz', 1, 3),
(2, 'Paroisse Sainte-Marie', 'maps/paroisse_st_marie.qgz', 2, 4);

-- =========================================================
-- DATA : EGLISE
-- =========================================================
INSERT INTO eglise (id, nom, adresse, localisation, historique, lien_facebook, paroisse_id, responsable_id) VALUES
(
    1, 
    'Cathédrale Saint-Joseph', 
    'Analakely, Antananarivo 101', 
    ST_SetSRID(ST_MakePoint(47.5256, -18.9101), 4326), 
    'Édifice historique majeur inauguré au début du XXe siècle.', 
    'https://facebook.com/cathedrale.stjoseph', 
    1, 
    3
),
(
    2, 
    'Église Sainte-Thérèse', 
    'Boulevard Joffre, Toamasina 501', 
    ST_SetSRID(ST_MakePoint(49.4083, -18.1551), 4326), 
    'Paroisse dynamique fondée en 1952.', 
    'https://facebook.com/eglise.stetherese.toamasina', 
    2, 
    5
);

-- =========================================================
-- DATA : HORAIRE_CULTE
-- =========================================================
INSERT INTO horaire_culte (id, jour, libelle, debut, fin, eglise_id) VALUES
(1, 'Dimanche', 'Messe matinale', '06:30:00', '08:00:00', 1),
(2, 'Dimanche', 'Messe des jeunes', '09:00:00', '10:30:00', 1),
(3, 'Vendredi', 'Messe en semaine', '17:00:00', '18:00:00', 1),
(4, 'Dimanche', 'Culte principal', '07:00:00', '08:30:00', 2);

-- =========================================================
-- REINITIALISATION DES SEQUENCES
-- =========================================================
SELECT setval('clerge_id_seq', (SELECT MAX(id) FROM clerge));
SELECT setval('diocese_id_seq', (SELECT MAX(id) FROM diocese));
SELECT setval('region_id_seq', (SELECT MAX(id) FROM region));
SELECT setval('district_id_seq', (SELECT MAX(id) FROM district));
SELECT setval('paroisse_id_seq', (SELECT MAX(id) FROM paroisse));
SELECT setval('eglise_id_seq', (SELECT MAX(id) FROM eglise));
SELECT setval('horaire_culte_id_seq', (SELECT MAX(id) FROM horaire_culte));