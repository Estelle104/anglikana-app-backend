package com.anglikana.backoffice.repository;

import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.dto.ReferenceEntiteDTO;
import com.anglikana.backoffice.security.EntiteProtegee;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Périmètre d'un responsable : chaîne des responsables d'une entité et entités racines d'un
 * membre du clergé. Seules les lignes actives (etat = 1) sont prises en compte.
 *
 * <p>Les noms de tables et de colonnes injectés proviennent exclusivement de
 * {@link EntiteProtegee} (liste blanche) : aucune donnée utilisateur n'est concaténée.</p>
 */
@Repository
public class PerimetreRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Responsables de l'entité puis de ses ancêtres, de l'entité jusqu'au diocèse. Une valeur
     * vaut null quand le niveau n'a pas de responsable.
     *
     * @return vide si l'entité n'existe pas ou est supprimée
     */
    public Optional<List<Integer>> chaineResponsables(EntiteProtegee entite, Integer id) {
        List<EntiteProtegee> chaine = new ArrayList<>();
        for (EntiteProtegee courante = entite; courante != null; courante = courante.parent().orElse(null)) {
            chaine.add(courante);
        }

        StringBuilder colonnes = new StringBuilder();
        StringBuilder jointures = new StringBuilder();
        for (int i = 0; i < chaine.size(); i++) {
            colonnes.append(i == 0 ? "" : ", ").append("t").append(i).append(".responsable_id AS r").append(i);
            if (i > 0) {
                jointures.append("JOIN ").append(chaine.get(i).getTable()).append(" t").append(i)
                        .append(" ON t").append(i).append(".id = t").append(i - 1).append('.')
                        .append(chaine.get(i - 1).getColonneParent())
                        .append(" AND t").append(i).append(".etat = 1\n");
            }
        }
        String sql = "SELECT " + colonnes + "\nFROM " + entite.getTable() + " t0\n" + jointures
                + "WHERE t0.id = :id AND t0.etat = 1";

        List<?> resultats = entityManager.createNativeQuery(sql, Tuple.class).setParameter("id", id).getResultList();
        if (resultats.isEmpty()) {
            return Optional.empty();
        }

        Tuple tuple = (Tuple) resultats.get(0);
        List<Integer> responsables = new ArrayList<>();
        for (int i = 0; i < chaine.size(); i++) {
            Number valeur = (Number) tuple.get("r" + i);
            responsables.add(valeur != null ? valeur.intValue() : null);
        }
        return Optional.of(responsables);
    }

    /**
     * Entités de la hiérarchie (diocèse → église) dont le clergé est responsable, sans celles déjà
     * comprises dans une autre de ses racines. Triées du niveau le plus haut au plus bas, puis par
     * nom. Les ressources n'en font pas partie.
     */
    public List<ReferenceEntiteDTO> racines(Integer clergeId) {
        String sql = HierarchieStatistiqueRepository.PERIMETRE + """
                SELECT p.niveau, p.id, p.nom
                FROM perimetre p
                WHERE p.responsable_id = :clerge
                  AND p.niveau <> 'ressource'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM perimetre a
                      WHERE a.responsable_id = :clerge
                        AND a.niveau <> 'ressource'
                        AND a.rang < p.rang
                        AND a.id = CASE a.niveau
                                       WHEN 'diocese'  THEN p.diocese_id
                                       WHEN 'region'   THEN p.region_id
                                       WHEN 'district' THEN p.district_id
                                       WHEN 'paroisse' THEN p.paroisse_id
                                   END
                  )
                ORDER BY p.rang, p.nom
                """;

        List<ReferenceEntiteDTO> racines = new ArrayList<>();
        for (Object resultat : entityManager.createNativeQuery(sql, Tuple.class).setParameter("clerge", clergeId).getResultList()) {
            Tuple tuple = (Tuple) resultat;
            NiveauHierarchique niveau = NiveauHierarchique.depuisCode((String) tuple.get("niveau")).orElseThrow();
            racines.add(new ReferenceEntiteDTO(niveau, ((Number) tuple.get("id")).intValue(), (String) tuple.get("nom")));
        }
        return racines;
    }
}
