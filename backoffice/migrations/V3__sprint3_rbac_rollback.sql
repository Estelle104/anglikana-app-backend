-- =========================================================
-- ANNULATION du script V3__sprint3_rbac.sql
--
-- ATTENTION :
--   - les lignes supprimées logiquement (etat = -1) redeviennent visibles une fois la colonne
--     retirée. Pour les effacer vraiment, exécuter d'abord, de la ressource au diocèse :
--       DELETE FROM ressource WHERE etat = -1; ... DELETE FROM diocese WHERE etat = -1;
--   - les rôles reviennent à Admin / Curé / Diacre : Évêque → Admin,
--     Archidiacre, Doyen et Presbytère → Curé, Katekista → Diacre.
--   - remettre en place le code du sprint 2 (rôle ADMIN) avant de redémarrer l'application.
-- =========================================================

DROP INDEX IF EXISTS idx_ressource_responsable_actif;
DROP INDEX IF EXISTS idx_eglise_responsable_actif;
DROP INDEX IF EXISTS idx_paroisse_responsable_actif;
DROP INDEX IF EXISTS idx_district_responsable_actif;
DROP INDEX IF EXISTS idx_region_responsable_actif;
DROP INDEX IF EXISTS idx_diocese_responsable_actif;

DO $$
DECLARE
    nom_table TEXT;
BEGIN
    FOREACH nom_table IN ARRAY ARRAY['clerge', 'diocese', 'region', 'district', 'paroisse', 'eglise', 'ressource'] LOOP
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I', nom_table, 'ck_' || nom_table || '_etat');
        EXECUTE format('ALTER TABLE %I DROP COLUMN IF EXISTS etat', nom_table);
    END LOOP;
END $$;

ALTER TABLE role DROP CONSTRAINT IF EXISTS ck_role_niveau;
ALTER TABLE role DROP CONSTRAINT IF EXISTS uk_role_niveau;
ALTER TABLE role ALTER COLUMN niveau DROP NOT NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM role WHERE nom = 'Évêque') THEN
        -- Ancien rôle de chaque clergé, d'après son niveau actuel.
        CREATE TEMPORARY TABLE ancien_role ON COMMIT DROP AS
        SELECT c.id AS clerge_id,
               CASE WHEN r.niveau = 5 THEN 1 WHEN r.niveau = 1 THEN 3 ELSE 2 END AS role_id
        FROM clerge c
        JOIN role r ON r.id = c.role_id;

        UPDATE clerge SET role_id = NULL;
        DELETE FROM role;

        INSERT INTO role (id, nom, niveau, description) VALUES
        (1, 'Admin', 1, 'Administrateur système'),
        (2, 'Curé', 2, 'Responsable paroissial'),
        (3, 'Diacre', 3, 'Aide pastorale');

        UPDATE clerge c SET role_id = a.role_id FROM ancien_role a WHERE a.clerge_id = c.id;

        PERFORM setval('role_id_seq', (SELECT MAX(id) FROM role));
    END IF;
END $$;
