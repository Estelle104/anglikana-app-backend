
-- =========================================================
-- DATA : ROLE
-- =========================================================
-- id = niveau ; 5 = le plus haut. Les droits viennent du niveau et des responsable_id.
INSERT INTO role (id, nom, niveau, description) VALUES
(1, 'Katekista', 1, 'Responsable d''une église'),
(2, 'Presbytère', 2, 'Responsable d''une paroisse'),
(3, 'Doyen', 3, 'Responsable d''un district'),
(4, 'Archidiacre', 4, 'Responsable d''une région'),
(5, 'Évêque', 5, 'Responsable d''un diocèse');

-- =========================================================
-- DATA : CLERGE
-- role_id = plus haute responsabilité : 1 et 2 diocèse (Évêque), 6 région (Archidiacre),
-- 3 et 4 district (Doyen), 7 paroisse (Presbytère), 5 église (Katekista).
-- 8 : aucune responsabilité ni compte (test de suppression d'un clergé et de création de compte).
-- =========================================================
INSERT INTO clerge (id, nom, prenom, date_naissance, hierarchie, telephone, email, role_id) VALUES
(1, 'Rakoto', 'Jean', '1980-05-12', 'Prêtre', '+261 34 12 345 67', 'jean.rakoto@eglise.mg', 5),
(2, 'Randria', 'Marie', '1985-02-20', 'Diacre', '+261 34 22 456 78', 'marie.randria@eglise.mg', 5),
(3, 'Andriamihaja', 'Paul', '1975-11-08', 'Curé', '+261 34 33 567 89', 'paul.andria@eglise.mg', 3),
(4, 'Rabe', 'Benoît', '1988-09-15', 'Prêtre', '+261 34 44 678 90', 'benoit.rabe@eglise.mg', 3),
(5, 'Rasolonjatovo', 'Claire', '1990-06-11', 'Diaconesse', '+261 34 55 789 01', 'claire.rasolon@eglise.mg', 1),
(6, 'Ravelojaona', 'Michel', '1970-03-22', 'Archidiacre', '+261 34 66 890 12', 'michel.ravelo@eglise.mg', 4),
(7, 'Rakotondrabe', 'Hery', '1982-08-30', 'Prêtre', '+261 34 77 901 23', 'hery.rakotondrabe@eglise.mg', 2),
(8, 'Razafy', 'Lalao', '1995-01-17', 'Catéchiste', '+261 34 88 012 34', 'lalao.razafy@eglise.mg', 1);

-- =========================================================
-- DATA : DIOCESE
-- carte_qgis : polygone PostGIS, importé depuis QGIS (PUT /api/geographie/{niveau}/limites)
-- =========================================================
INSERT INTO diocese (id, nom, carte_qgis, presentation, coordonnees, responsable_id) VALUES
(1, 'Diocèse d''Antananarivo', NULL, 'Diocèse métropolitain fondé à la fin du XIXe siècle.', '-18.8792, 47.5079', 1),
(2, 'Diocèse de Toamasina', NULL, 'Diocèse couvrant la région côtière de l''Est.', '-18.1492, 49.4023', 2);

-- =========================================================
-- DATA : REGION
-- =========================================================
INSERT INTO region (id, nom, diocese_id, responsable_id) VALUES
(1, 'Région Analamanga Centre', 1, 6),
(2, 'Région Atsinanana Littoral', 2, 2);

-- =========================================================
-- DATA : DISTRICT
-- =========================================================
INSERT INTO district (id, nom, carte_qgis, region_id, responsable_id) VALUES
(1, 'District de Tana Ville', NULL, 1, 3),
(2, 'District de Toamasina Urbain', NULL, 2, 4);

-- =========================================================
-- DATA : PAROISSE
-- =========================================================
INSERT INTO paroisse (id, nom, carte_qgis, district_id, responsable_id) VALUES
(1, 'Paroisse Saint-Joseph', NULL, 1, 7),
(2, 'Paroisse Sainte-Marie', NULL, 2, 4);

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
-- DATA : TYPE_RESSOURCE ET RESSOURCE
-- La ressource de l'église 1 empêche sa suppression (409) tant qu'elle est active.
-- =========================================================
INSERT INTO type_ressource (id, nom) VALUES
(1, 'École'),
(2, 'Dispensaire');

INSERT INTO ressource (id, nom, adresse, localisation, type_id, eglise_id, responsable_id) VALUES
(1, 'École Saint-Joseph', 'Analakely, Antananarivo 101', ST_SetSRID(ST_MakePoint(47.5262, -18.9105), 4326), 1, 1, 3);

-- =========================================================
-- DATA : UTILISATEUR
-- Comptes de TEST, mot de passe commun : password123 (hash BCrypt).
-- À NE JAMAIS CHARGER EN PRODUCTION.
-- Les droits viennent du clergé : niveau de son rôle et entités dont il est responsable.
-- =========================================================
INSERT INTO utilisateur (id, nom_utilisateur, mot_de_passe, actif, clerge_id) VALUES
(1, 'eveque.antananarivo', '$2a$10$6t16IiXI21pL4UUOraJXxuq4HgppB/W8DqwZnZt6CiBYFeIHwViEO', TRUE, 1),
(2, 'eveque.toamasina', '$2a$10$6t16IiXI21pL4UUOraJXxuq4HgppB/W8DqwZnZt6CiBYFeIHwViEO', TRUE, 2),
(3, 'archidiacre.analamanga', '$2a$10$6t16IiXI21pL4UUOraJXxuq4HgppB/W8DqwZnZt6CiBYFeIHwViEO', TRUE, 6),
(4, 'doyen.tana', '$2a$10$6t16IiXI21pL4UUOraJXxuq4HgppB/W8DqwZnZt6CiBYFeIHwViEO', TRUE, 3),
(5, 'doyen.toamasina', '$2a$10$6t16IiXI21pL4UUOraJXxuq4HgppB/W8DqwZnZt6CiBYFeIHwViEO', TRUE, 4),
(6, 'presbytere.saintjoseph', '$2a$10$6t16IiXI21pL4UUOraJXxuq4HgppB/W8DqwZnZt6CiBYFeIHwViEO', TRUE, 7),
(7, 'katekista.saintetherese', '$2a$10$6t16IiXI21pL4UUOraJXxuq4HgppB/W8DqwZnZt6CiBYFeIHwViEO', TRUE, 5);

-- =========================================================
-- REINITIALISATION DES SEQUENCES
-- =========================================================
SELECT setval('role_id_seq', (SELECT MAX(id) FROM role));
SELECT setval('clerge_id_seq', (SELECT MAX(id) FROM clerge));
SELECT setval('diocese_id_seq', (SELECT MAX(id) FROM diocese));
SELECT setval('region_id_seq', (SELECT MAX(id) FROM region));
SELECT setval('district_id_seq', (SELECT MAX(id) FROM district));
SELECT setval('paroisse_id_seq', (SELECT MAX(id) FROM paroisse));
SELECT setval('eglise_id_seq', (SELECT MAX(id) FROM eglise));
SELECT setval('horaire_culte_id_seq', (SELECT MAX(id) FROM horaire_culte));
SELECT setval('type_ressource_id_seq', (SELECT MAX(id) FROM type_ressource));
SELECT setval('ressource_id_seq', (SELECT MAX(id) FROM ressource));
SELECT setval('utilisateur_id_seq', (SELECT MAX(id) FROM utilisateur));