package com.anglikana.backoffice.repository;

import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.dto.ReferenceEntiteDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Agrégations statistiques de la hiérarchie ecclésiastique, calculées par PostgreSQL.
 *
 * <p>La CTE {@code perimetre} aplatit la hiérarchie : une ligne par entité (diocèse → ressource)
 * portant les identifiants de tous ses ancêtres. Chaque entité n'apparaît qu'une fois, ce qui
 * permet de compter par simple {@code COUNT(*) FILTER} sans risque de double comptage lié aux
 * jointures, et de compter le clergé par {@code COUNT(DISTINCT responsable_id)}.</p>
 *
 * <p>Les noms de colonnes injectés dans le SQL proviennent exclusivement de {@link #colonne}
 * (liste blanche indexée par l'énumération) : aucune donnée utilisateur n'est concaténée.</p>
 */
@Repository
public class HierarchieStatistiqueRepository {

    /** Partagée avec {@link PerimetreRepository} : seules les lignes actives (etat = 1) y figurent. */
    static final String PERIMETRE = """
            WITH perimetre AS (
                SELECT 'diocese' AS niveau, 1 AS rang, d.id, d.nom, d.responsable_id,
                       d.id AS diocese_id, CAST(NULL AS integer) AS region_id, CAST(NULL AS integer) AS district_id,
                       CAST(NULL AS integer) AS paroisse_id, CAST(NULL AS integer) AS eglise_id
                FROM diocese d
                WHERE d.etat = 1
                UNION ALL
                SELECT 'region', 2, r.id, r.nom, r.responsable_id,
                       r.diocese_id, r.id, NULL, NULL, NULL
                FROM region r
                WHERE r.etat = 1
                UNION ALL
                SELECT 'district', 3, di.id, di.nom, di.responsable_id,
                       r.diocese_id, di.region_id, di.id, NULL, NULL
                FROM district di
                JOIN region r ON r.id = di.region_id AND r.etat = 1
                WHERE di.etat = 1
                UNION ALL
                SELECT 'paroisse', 4, pa.id, pa.nom, pa.responsable_id,
                       r.diocese_id, di.region_id, pa.district_id, pa.id, NULL
                FROM paroisse pa
                JOIN district di ON di.id = pa.district_id AND di.etat = 1
                JOIN region r ON r.id = di.region_id AND r.etat = 1
                WHERE pa.etat = 1
                UNION ALL
                SELECT 'eglise', 5, e.id, e.nom, e.responsable_id,
                       r.diocese_id, di.region_id, pa.district_id, e.paroisse_id, e.id
                FROM eglise e
                JOIN paroisse pa ON pa.id = e.paroisse_id AND pa.etat = 1
                JOIN district di ON di.id = pa.district_id AND di.etat = 1
                JOIN region r ON r.id = di.region_id AND r.etat = 1
                WHERE e.etat = 1
                UNION ALL
                SELECT 'ressource', 6, rs.id, rs.nom, rs.responsable_id,
                       r.diocese_id, di.region_id, pa.district_id, e.paroisse_id, rs.eglise_id
                FROM ressource rs
                JOIN eglise e ON e.id = rs.eglise_id AND e.etat = 1
                JOIN paroisse pa ON pa.id = e.paroisse_id AND pa.etat = 1
                JOIN district di ON di.id = pa.district_id AND di.etat = 1
                JOIN region r ON r.id = di.region_id AND r.etat = 1
                WHERE rs.etat = 1
            )
            """;

    /**
     * Comptages communs. Le clergé exclut les responsables de ressources : il compte les membres
     * responsables d'une entité de la hiérarchie (diocèse → église), règle partagée avec le frontend.
     */
    private static final String COMPTAGES = """
                   COUNT(*) FILTER (WHERE p.niveau = 'diocese')   AS dioceses,
                   COUNT(*) FILTER (WHERE p.niveau = 'region')    AS regions,
                   COUNT(*) FILTER (WHERE p.niveau = 'district')  AS districts,
                   COUNT(*) FILTER (WHERE p.niveau = 'paroisse')  AS paroisses,
                   COUNT(*) FILTER (WHERE p.niveau = 'eglise')    AS eglises,
                   COUNT(*) FILTER (WHERE p.niveau = 'ressource') AS ressources,
                   COUNT(DISTINCT p.responsable_id) FILTER (WHERE p.niveau <> 'ressource') AS clerges
            """;

    @PersistenceContext
    private EntityManager entityManager;

    /** Comptages bruts d'une entité : chaque compteur inclut l'entité elle-même si elle est de ce niveau. */
    public record LigneStatistique(Integer id, String nom, Integer responsableId,
                                   long dioceses, long regions, long districts, long paroisses,
                                   long eglises, long ressources, long clerges) {
    }

    /** Membre du clergé responsable d'une entité du périmètre. */
    public record Responsabilite(Integer clergeId, String entiteNom) {
    }

    /**
     * Statistiques de chaque entité du niveau {@code niveauGroupe}, éventuellement restreintes
     * au périmètre d'une entité ({@code niveauPerimetre}, {@code idPerimetre}).
     * Exemples : (REGION, DIOCESE, 1) = chaque région du diocèse 1 ; (DIOCESE, null, null) = chaque diocèse.
     */
    public List<LigneStatistique> statistiquesParEntite(NiveauHierarchique niveauGroupe,
                                                        NiveauHierarchique niveauPerimetre,
                                                        Integer idPerimetre) {
        String colonneGroupe = colonne(niveauGroupe);
        StringBuilder sql = new StringBuilder(PERIMETRE)
                .append("SELECT p.").append(colonneGroupe).append(" AS id,\n")
                .append("       MAX(p.nom) FILTER (WHERE p.niveau = :niveauGroupe) AS nom,\n")
                .append("       MAX(p.responsable_id) FILTER (WHERE p.niveau = :niveauGroupe) AS responsable_id,\n")
                .append(COMPTAGES)
                .append("FROM perimetre p\n")
                .append("WHERE p.").append(colonneGroupe).append(" IS NOT NULL\n");
        if (niveauPerimetre != null) {
            sql.append("  AND p.").append(colonne(niveauPerimetre)).append(" = :idPerimetre\n");
        }
        sql.append("GROUP BY p.").append(colonneGroupe);

        Query query = entityManager.createNativeQuery(sql.toString(), Tuple.class)
                .setParameter("niveauGroupe", niveauGroupe.getCode());
        if (niveauPerimetre != null) {
            query.setParameter("idPerimetre", idPerimetre);
        }

        List<LigneStatistique> lignes = new ArrayList<>();
        for (Object resultat : query.getResultList()) {
            Tuple tuple = (Tuple) resultat;
            lignes.add(versLigne(tuple, entier(tuple, "id"), (String) tuple.get("nom"), entier(tuple, "responsable_id")));
        }
        return lignes;
    }

    /**
     * Totaux restreints à des diocèses (le clergé est compté une seule fois sur l'ensemble).
     * Tous les compteurs valent 0 si la liste est vide.
     */
    public LigneStatistique totaux(Collection<Integer> dioceseIds) {
        if (dioceseIds.isEmpty()) {
            return new LigneStatistique(null, null, null, 0, 0, 0, 0, 0, 0, 0);
        }
        String sql = PERIMETRE + "SELECT\n" + COMPTAGES + "FROM perimetre p\nWHERE p.diocese_id IN (:ids)";
        Tuple tuple = (Tuple) entityManager.createNativeQuery(sql, Tuple.class)
                .setParameter("ids", dioceseIds)
                .getSingleResult();
        return versLigne(tuple, null, null, null);
    }

    /**
     * Responsabilités du clergé dans le périmètre d'une entité (l'entité et ses descendants,
     * ressources exclues), triées du niveau le plus haut au plus bas.
     */
    public List<Responsabilite> responsabilites(NiveauHierarchique niveau, Integer id) {
        String sql = PERIMETRE + """
                SELECT p.responsable_id, p.nom
                FROM perimetre p
                WHERE p.%s = :id
                  AND p.niveau <> 'ressource'
                  AND p.responsable_id IS NOT NULL
                ORDER BY p.rang, p.nom
                """.formatted(colonne(niveau));

        List<Responsabilite> responsabilites = new ArrayList<>();
        for (Object resultat : entityManager.createNativeQuery(sql, Tuple.class).setParameter("id", id).getResultList()) {
            Tuple tuple = (Tuple) resultat;
            responsabilites.add(new Responsabilite(entier(tuple, "responsable_id"), (String) tuple.get("nom")));
        }
        return responsabilites;
    }

    /**
     * Chaîne hiérarchique d'une entité, du diocèse jusqu'à l'entité elle-même (incluse).
     * Liste vide si l'entité n'existe pas.
     */
    public List<ReferenceEntiteDTO> chaine(NiveauHierarchique niveau, Integer id) {
        String sql = PERIMETRE + """
                SELECT p.diocese_id, d.nom AS diocese_nom,
                       p.region_id, r.nom AS region_nom,
                       p.district_id, di.nom AS district_nom,
                       p.paroisse_id, pa.nom AS paroisse_nom,
                       p.eglise_id, e.nom AS eglise_nom
                FROM perimetre p
                LEFT JOIN diocese d ON d.id = p.diocese_id
                LEFT JOIN region r ON r.id = p.region_id
                LEFT JOIN district di ON di.id = p.district_id
                LEFT JOIN paroisse pa ON pa.id = p.paroisse_id
                LEFT JOIN eglise e ON e.id = p.eglise_id
                WHERE p.niveau = :niveau AND p.id = :id
                """;

        List<?> resultats = entityManager.createNativeQuery(sql, Tuple.class)
                .setParameter("niveau", niveau.getCode())
                .setParameter("id", id)
                .getResultList();

        List<ReferenceEntiteDTO> chaine = new ArrayList<>();
        if (resultats.isEmpty()) {
            return chaine;
        }

        Tuple tuple = (Tuple) resultats.get(0);
        for (NiveauHierarchique courant : NiveauHierarchique.values()) {
            Integer idCourant = entier(tuple, colonne(courant));
            if (idCourant != null) {
                chaine.add(new ReferenceEntiteDTO(courant, idCourant, (String) tuple.get(courant.getCode() + "_nom")));
            }
        }
        return chaine;
    }

    /** Colonne de la CTE {@code perimetre} portant l'identifiant de l'ancêtre de ce niveau. */
    static String colonne(NiveauHierarchique niveau) {
        return switch (niveau) {
            case DIOCESE -> "diocese_id";
            case REGION -> "region_id";
            case DISTRICT -> "district_id";
            case PAROISSE -> "paroisse_id";
            case EGLISE -> "eglise_id";
        };
    }

    private static LigneStatistique versLigne(Tuple tuple, Integer id, String nom, Integer responsableId) {
        return new LigneStatistique(id, nom, responsableId,
                compte(tuple, "dioceses"), compte(tuple, "regions"), compte(tuple, "districts"),
                compte(tuple, "paroisses"), compte(tuple, "eglises"), compte(tuple, "ressources"),
                compte(tuple, "clerges"));
    }

    private static Integer entier(Tuple tuple, String alias) {
        Number valeur = (Number) tuple.get(alias);
        return valeur != null ? valeur.intValue() : null;
    }

    private static long compte(Tuple tuple, String alias) {
        Number valeur = (Number) tuple.get(alias);
        return valeur != null ? valeur.longValue() : 0L;
    }
}
