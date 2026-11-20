-- =========================================================
-- SPRINT 3 : contrôle d'accès par niveau et par périmètre, suppression logique
--
-- 1. Rôles : Admin / Curé / Diacre sont remplacés par les cinq responsables de la hiérarchie,
--    portés par role.niveau (5 = le plus haut) :
--      5 Évêque (diocèse), 4 Archidiacre (région), 3 Doyen (district),
--      2 Presbytère (paroisse), 1 Katekista (église).
--    Chaque clergé reçoit le rôle de sa plus haute responsabilité (responsable_id). Un clergé
--    sans responsabilité n'a plus de rôle.
-- 2. Suppression logique : colonne etat (1 = actif, -1 = supprimé) sur clerge, diocese, region,
--    district, paroisse, eglise et ressource. L'API ne fait plus de DELETE physique.
--
-- Script idempotent : les rôles ne sont remplacés que si l'ancien rôle « Admin » existe encore ;
-- les colonnes, contraintes et index ne sont créés qu'une fois.
-- Annulation : V3__sprint3_rbac_rollback.sql
-- =========================================================

-- ---------------------------------------------------------
-- Suppression logique : colonne etat
-- ---------------------------------------------------------
DO $$
DECLARE
    nom_table TEXT;
BEGIN
    FOREACH nom_table IN ARRAY ARRAY['clerge', 'diocese', 'region', 'district', 'paroisse', 'eglise', 'ressource'] LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS etat SMALLINT NOT NULL DEFAULT 1', nom_table);
        -- Colonne déjà créée par Hibernate (ddl-auto=update) : on garantit défaut et NOT NULL.
        EXECUTE format('UPDATE %I SET etat = 1 WHERE etat IS NULL', nom_table);
        EXECUTE format('ALTER TABLE %I ALTER COLUMN etat SET DEFAULT 1', nom_table);
        EXECUTE format('ALTER TABLE %I ALTER COLUMN etat SET NOT NULL', nom_table);
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_' || nom_table || '_etat') THEN
            EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I CHECK (etat IN (1, -1))',
                           nom_table, 'ck_' || nom_table || '_etat');
        END IF;
    END LOOP;
END $$;

-- Index partiels des responsables actifs : calcul du périmètre (GET /api/moi) et refus de
-- suppression d'un clergé encore responsable.
CREATE INDEX IF NOT EXISTS idx_diocese_responsable_actif   ON diocese   (responsable_id) WHERE etat = 1;
CREATE INDEX IF NOT EXISTS idx_region_responsable_actif    ON region    (responsable_id) WHERE etat = 1;
CREATE INDEX IF NOT EXISTS idx_district_responsable_actif  ON district  (responsable_id) WHERE etat = 1;
CREATE INDEX IF NOT EXISTS idx_paroisse_responsable_actif  ON paroisse  (responsable_id) WHERE etat = 1;
CREATE INDEX IF NOT EXISTS idx_eglise_responsable_actif    ON eglise    (responsable_id) WHERE etat = 1;
CREATE INDEX IF NOT EXISTS idx_ressource_responsable_actif ON ressource (responsable_id) WHERE etat = 1;

-- ---------------------------------------------------------
-- Rôles des responsables (id = niveau)
-- ---------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM role WHERE nom = 'Admin') THEN
        UPDATE clerge SET role_id = NULL;
        DELETE FROM role;

        INSERT INTO role (id, nom, niveau, description) VALUES
        (1, 'Katekista', 1, 'Responsable d''une église'),
        (2, 'Presbytère', 2, 'Responsable d''une paroisse'),
        (3, 'Doyen', 3, 'Responsable d''un district'),
        (4, 'Archidiacre', 4, 'Responsable d''une région'),
        (5, 'Évêque', 5, 'Responsable d''un diocèse');

        -- Plus haute responsabilité de chaque clergé (entités actives uniquement).
        UPDATE clerge c
        SET role_id = r.niveau_max
        FROM (
            SELECT responsable_id, MAX(niveau) AS niveau_max
            FROM (
                SELECT responsable_id, 5 AS niveau FROM diocese  WHERE etat = 1
                UNION ALL SELECT responsable_id, 4 FROM region   WHERE etat = 1
                UNION ALL SELECT responsable_id, 3 FROM district WHERE etat = 1
                UNION ALL SELECT responsable_id, 2 FROM paroisse WHERE etat = 1
                UNION ALL SELECT responsable_id, 1 FROM eglise   WHERE etat = 1
            ) responsabilites
            WHERE responsable_id IS NOT NULL
            GROUP BY responsable_id
        ) r
        WHERE r.responsable_id = c.id;

        PERFORM setval('role_id_seq', (SELECT MAX(id) FROM role));
    END IF;
END $$;

ALTER TABLE role ALTER COLUMN niveau SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_role_niveau') THEN
        ALTER TABLE role ADD CONSTRAINT uk_role_niveau UNIQUE (niveau);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_role_niveau') THEN
        ALTER TABLE role ADD CONSTRAINT ck_role_niveau CHECK (niveau BETWEEN 1 AND 5);
    END IF;
END $$;
