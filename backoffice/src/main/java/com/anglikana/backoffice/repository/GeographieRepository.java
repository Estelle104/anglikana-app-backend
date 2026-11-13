package com.anglikana.backoffice.repository;

import com.anglikana.backoffice.dto.EgliseLocaliseeDTO;
import com.anglikana.backoffice.dto.NiveauHierarchique;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Requêtes spatiales PostGIS sur les limites des zones (diocèse, région, district).
 *
 * <p>Limites ({@code limite}) et localisations des églises sont stockées en SRID 4326 (WGS 84) :
 * les comparaisons spatiales se font sans transformation. Les géométries sont renvoyées
 * en GeoJSON (RFC 7946, ordre longitude/latitude) par {@code ST_AsGeoJSON}.</p>
 *
 * <p>Appartenance d'une église à une zone : {@code ST_Covers(limite, localisation)}, qui inclut
 * les points situés exactement sur la frontière (contrairement à {@code ST_Contains}) et
 * exploite les index GIST.</p>
 *
 * <p>Les noms de tables et colonnes injectés proviennent exclusivement des listes blanches
 * {@link #table} et {@link #colonneRattachement}.</p>
 */
@Repository
public class GeographieRepository {

    /** 6 décimales ≈ 10 cm : précision suffisante pour l'affichage, réponses plus légères. */
    private static final int PRECISION_GEOJSON = 6;

    private static final String SELECT_EGLISE_LOCALISEE = """
            SELECT e.id, e.nom, e.adresse,
                   ST_Y(e.localisation) AS latitude, ST_X(e.localisation) AS longitude,
                   pa.nom AS paroisse_nom
            """;

    private static final String JOINTURES_HIERARCHIE = """
            FROM eglise e
            JOIN paroisse pa ON pa.id = e.paroisse_id
            JOIN district di ON di.id = pa.district_id
            JOIN region r ON r.id = di.region_id
            """;

    @PersistenceContext
    private EntityManager entityManager;

    /** Zone : identifiant, nom et géométrie GeoJSON (null si la limite n'a pas été importée). */
    public record LigneZone(Integer id, String nom, String geojson) {
    }

    /** Résultat brut de recherche avec les noms des ancêtres géographiques. */
    public record LigneRecherche(String niveau, Integer id, String nom, String regionNom, String dioceseNom) {
    }

    /** Zones d'un niveau disposant d'une limite géographique. */
    public List<LigneZone> limites(NiveauHierarchique niveau) {
        String sql = """
                SELECT z.id, z.nom, ST_AsGeoJSON(z.limite, %d) AS geojson
                FROM %s z
                WHERE z.limite IS NOT NULL
                ORDER BY z.nom
                """.formatted(PRECISION_GEOJSON, table(niveau));
        return versZones(entityManager.createNativeQuery(sql, Tuple.class).getResultList());
    }

    public Optional<LigneZone> zone(NiveauHierarchique niveau, Integer id) {
        String sql = """
                SELECT z.id, z.nom, ST_AsGeoJSON(z.limite, %d) AS geojson
                FROM %s z
                WHERE z.id = :id
                """.formatted(PRECISION_GEOJSON, table(niveau));
        return versZones(entityManager.createNativeQuery(sql, Tuple.class).setParameter("id", id).getResultList())
                .stream().findFirst();
    }

    /** Zones du niveau géographique inférieur rattachées à la zone et disposant d'une limite. */
    public List<LigneZone> sousZones(NiveauHierarchique niveau, Integer id) {
        String sql = switch (niveau) {
            case DIOCESE -> "SELECT z.id, z.nom, ST_AsGeoJSON(z.limite, %d) AS geojson FROM region z "
                    + "WHERE z.diocese_id = :id AND z.limite IS NOT NULL ORDER BY z.nom";
            case REGION -> "SELECT z.id, z.nom, ST_AsGeoJSON(z.limite, %d) AS geojson FROM district z "
                    + "WHERE z.region_id = :id AND z.limite IS NOT NULL ORDER BY z.nom";
            default -> null;
        };
        if (sql == null) {
            return List.of();
        }
        return versZones(entityManager.createNativeQuery(sql.formatted(PRECISION_GEOJSON), Tuple.class)
                .setParameter("id", id).getResultList());
    }

    /** Églises dont la localisation est couverte par le polygone de la zone (frontière incluse). */
    public List<EgliseLocaliseeDTO> eglisesDansZone(NiveauHierarchique niveau, Integer id) {
        String sql = SELECT_EGLISE_LOCALISEE + """
                FROM %s z
                JOIN eglise e ON ST_Covers(z.limite, e.localisation)
                JOIN paroisse pa ON pa.id = e.paroisse_id
                WHERE z.id = :id
                ORDER BY e.nom
                """.formatted(table(niveau));
        return versEglises(entityManager.createNativeQuery(sql, Tuple.class).setParameter("id", id).getResultList());
    }

    /** Églises rattachées hiérarchiquement à la zone mais localisées hors de son polygone. */
    public List<EgliseLocaliseeDTO> eglisesHorsZone(NiveauHierarchique niveau, Integer id) {
        String sql = SELECT_EGLISE_LOCALISEE + JOINTURES_HIERARCHIE + """
                JOIN %s z ON z.id = %s
                WHERE %s = :id
                  AND e.localisation IS NOT NULL
                  AND z.limite IS NOT NULL
                  AND NOT ST_Covers(z.limite, e.localisation)
                ORDER BY e.nom
                """.formatted(table(niveau), colonneRattachement(niveau), colonneRattachement(niveau));
        return versEglises(entityManager.createNativeQuery(sql, Tuple.class).setParameter("id", id).getResultList());
    }

    /** Églises localisées rattachées hiérarchiquement à la zone (utilisé lorsqu'aucune limite n'est disponible). */
    public List<EgliseLocaliseeDTO> eglisesRattachees(NiveauHierarchique niveau, Integer id) {
        String sql = SELECT_EGLISE_LOCALISEE + JOINTURES_HIERARCHIE + """
                WHERE %s = :id
                  AND e.localisation IS NOT NULL
                ORDER BY e.nom
                """.formatted(colonneRattachement(niveau));
        return versEglises(entityManager.createNativeQuery(sql, Tuple.class).setParameter("id", id).getResultList());
    }

    /**
     * Recherche des zones par nom (insensible à la casse et aux accents) ou par identifiant exact.
     *
     * @param niveauCode code du niveau, ou chaîne vide pour tous les niveaux géographiques
     * @param terme      terme recherché, ou chaîne vide pour tout lister
     */
    public List<LigneRecherche> rechercher(String niveauCode, String terme, int limite) {
        String sql = """
                SELECT z.niveau, z.id, z.nom, z.region_nom, z.diocese_nom
                FROM (
                    SELECT 'diocese' AS niveau, d.id, d.nom,
                           CAST(NULL AS varchar) AS region_nom, CAST(NULL AS varchar) AS diocese_nom
                    FROM diocese d
                    UNION ALL
                    SELECT 'region', r.id, r.nom, NULL, d.nom
                    FROM region r
                    JOIN diocese d ON d.id = r.diocese_id
                    UNION ALL
                    SELECT 'district', di.id, di.nom, r.nom, d.nom
                    FROM district di
                    JOIN region r ON r.id = di.region_id
                    JOIN diocese d ON d.id = r.diocese_id
                ) z
                WHERE (:niveau = '' OR z.niveau = :niveau)
                  AND (:terme = ''
                       OR lower(unaccent(z.nom)) LIKE '%' || lower(unaccent(:motif)) || '%'
                       OR CAST(z.id AS varchar) = :terme)
                ORDER BY z.nom, z.niveau
                LIMIT :limite
                """;

        List<?> resultats = entityManager.createNativeQuery(sql, Tuple.class)
                .setParameter("niveau", niveauCode)
                .setParameter("terme", terme)
                .setParameter("motif", echapperLike(terme))
                .setParameter("limite", limite)
                .getResultList();

        List<LigneRecherche> lignes = new ArrayList<>();
        for (Object resultat : resultats) {
            Tuple tuple = (Tuple) resultat;
            lignes.add(new LigneRecherche((String) tuple.get("niveau"), entier(tuple, "id"), (String) tuple.get("nom"),
                    (String) tuple.get("region_nom"), (String) tuple.get("diocese_nom")));
        }
        return lignes;
    }

    /**
     * Enregistre la limite d'une zone à partir d'une géométrie GeoJSON.
     * La géométrie est reprojetée en 4326 si elle déclare un autre CRS, corrigée si invalide
     * (ST_MakeValid) et normalisée en MultiPolygon.
     *
     * @return nombre de lignes mises à jour (0 si la zone n'existe pas)
     */
    public int majLimite(NiveauHierarchique niveau, Integer id, String geojson) {
        String sql = """
                UPDATE %s
                SET limite = ST_Multi(ST_CollectionExtract(ST_MakeValid(
                        ST_Transform(ST_GeomFromGeoJSON(:geojson), 4326)), 3))
                WHERE id = :id
                """.formatted(table(niveau));
        return entityManager.createNativeQuery(sql)
                .setParameter("geojson", geojson)
                .setParameter("id", id)
                .executeUpdate();
    }

    static String table(NiveauHierarchique niveau) {
        return switch (niveau) {
            case DIOCESE -> "diocese";
            case REGION -> "region";
            case DISTRICT -> "district";
            default -> throw new IllegalArgumentException("Niveau sans limite géographique : " + niveau.getCode());
        };
    }

    /** Colonne (dans {@link #JOINTURES_HIERARCHIE}) rattachant une église à une zone de ce niveau. */
    static String colonneRattachement(NiveauHierarchique niveau) {
        return switch (niveau) {
            case DIOCESE -> "r.diocese_id";
            case REGION -> "di.region_id";
            case DISTRICT -> "pa.district_id";
            default -> throw new IllegalArgumentException("Niveau sans limite géographique : " + niveau.getCode());
        };
    }

    /** Neutralise les caractères spéciaux de LIKE (% _ \) pour une recherche littérale. */
    static String echapperLike(String terme) {
        return terme.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static List<LigneZone> versZones(List<?> resultats) {
        List<LigneZone> zones = new ArrayList<>();
        for (Object resultat : resultats) {
            Tuple tuple = (Tuple) resultat;
            zones.add(new LigneZone(entier(tuple, "id"), (String) tuple.get("nom"), (String) tuple.get("geojson")));
        }
        return zones;
    }

    private static List<EgliseLocaliseeDTO> versEglises(List<?> resultats) {
        List<EgliseLocaliseeDTO> eglises = new ArrayList<>();
        for (Object resultat : resultats) {
            Tuple tuple = (Tuple) resultat;
            EgliseLocaliseeDTO dto = new EgliseLocaliseeDTO();
            dto.setId(entier(tuple, "id"));
            dto.setNom((String) tuple.get("nom"));
            dto.setAdresse((String) tuple.get("adresse"));
            dto.setLatitude(decimal(tuple, "latitude"));
            dto.setLongitude(decimal(tuple, "longitude"));
            dto.setParoisseNom((String) tuple.get("paroisse_nom"));
            eglises.add(dto);
        }
        return eglises;
    }

    private static Integer entier(Tuple tuple, String alias) {
        Number valeur = (Number) tuple.get(alias);
        return valeur != null ? valeur.intValue() : null;
    }

    private static Double decimal(Tuple tuple, String alias) {
        Number valeur = (Number) tuple.get(alias);
        return valeur != null ? valeur.doubleValue() : null;
    }
}
