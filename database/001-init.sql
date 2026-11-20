-- =========================================================
-- INITIALISATION : extensions PostgreSQL
-- Exécuté en premier par docker-entrypoint-initdb.d (ordre alphabétique).
-- =========================================================

-- Types géométriques (GEOMETRY(Polygon/Point, 4326)) des tables diocese, region,
-- district, paroisse et eglise
CREATE EXTENSION IF NOT EXISTS postgis;

-- Recherche insensible aux accents (« Diocese » trouve « Diocèse »)
CREATE EXTENSION IF NOT EXISTS unaccent;
