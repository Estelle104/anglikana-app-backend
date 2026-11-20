package com.anglikana.backoffice.sprint3;

import com.anglikana.backoffice.dto.EgliseDTO;
import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.dto.ReferenceEntiteDTO;
import com.anglikana.backoffice.entity.Ressource;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.DistrictRepository;
import com.anglikana.backoffice.repository.EgliseRepository;
import com.anglikana.backoffice.repository.GeographieRepository;
import com.anglikana.backoffice.repository.HierarchieStatistiqueRepository;
import com.anglikana.backoffice.repository.ParoisseRepository;
import com.anglikana.backoffice.repository.PerimetreRepository;
import com.anglikana.backoffice.repository.RessourceRepository;
import com.anglikana.backoffice.security.EntiteProtegee;
import com.anglikana.backoffice.service.HierarchieService;
import com.anglikana.backoffice.sprint2.JeuDonneesHierarchie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration du sprint 3 sur la base PostgreSQL/PostGIS configurée : périmètre des
 * responsables et suppression logique (etat = -1). Chaque test est annulé à la fin.
 * Le schéma n'est jamais modifié (ddl-auto=none) ; la migration V3 doit avoir été appliquée.
 *
 * <p>Responsabilités du jeu de données : A = diocèse D, région R1, église E4 ; B = districts DI1
 * et DI2, paroisse P1 ; C = paroisse P2, église E1, ressource 1 ; X = ressource 2.</p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@Import({PerimetreRepository.class, HierarchieStatistiqueRepository.class, GeographieRepository.class, HierarchieService.class})
class Sprint3PostgisIntegrationTest {

    @Autowired private TestEntityManager em;
    @Autowired private PerimetreRepository perimetreRepository;
    @Autowired private HierarchieStatistiqueRepository statistiqueRepository;
    @Autowired private GeographieRepository geographieRepository;
    @Autowired private HierarchieService hierarchieService;
    @Autowired private EgliseRepository egliseRepository;
    @Autowired private ParoisseRepository paroisseRepository;
    @Autowired private DistrictRepository districtRepository;
    @Autowired private RessourceRepository ressourceRepository;
    @Autowired private ClergeRepository clergeRepository;

    private JeuDonneesHierarchie donnees;

    @BeforeEach
    void preparer() {
        donnees = new JeuDonneesHierarchie().inserer(em);
    }

    /** Valeur brute de la colonne etat, lue sans le filtre @SQLRestriction. */
    private Number etat(String table, Integer id) {
        return (Number) em.getEntityManager()
                .createNativeQuery("SELECT etat FROM " + table + " WHERE id = :id")
                .setParameter("id", id)
                .getSingleResult();
    }

    private void supprimerEtVider(Runnable suppression) {
        suppression.run();
        em.flush();
        em.clear();
    }

    // ------------------------------------------------------------------ Périmètre

    @Test
    void chaineDesResponsablesDeLEgliseAuDiocese() {
        assertThat(perimetreRepository.chaineResponsables(EntiteProtegee.EGLISE, donnees.e1.getId())).hasValue(List.of(
                donnees.c.getId(), donnees.b.getId(), donnees.b.getId(), donnees.a.getId(), donnees.a.getId()));
        assertThat(perimetreRepository.chaineResponsables(EntiteProtegee.DIOCESE, donnees.d.getId()))
                .hasValue(List.of(donnees.a.getId()));
        assertThat(perimetreRepository.chaineResponsables(EntiteProtegee.DISTRICT, -1)).isEmpty();
    }

    @Test
    void chaineAvecDesNiveauxSansResponsable() {
        // E2 n'a pas de responsable ; R2 non plus (mais E2 est sous R1).
        assertThat(perimetreRepository.chaineResponsables(EntiteProtegee.EGLISE, donnees.e2.getId()).orElseThrow())
                .containsExactly(null, donnees.b.getId(), donnees.b.getId(), donnees.a.getId(), donnees.a.getId());
    }

    @Test
    void racinesSansLesEntitesDejaComprisesDansUneAutre() {
        assertThat(perimetreRepository.racines(donnees.a.getId()))
                .extracting(ReferenceEntiteDTO::getNiveau, ReferenceEntiteDTO::getId)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(NiveauHierarchique.DIOCESE, donnees.d.getId()));

        assertThat(perimetreRepository.racines(donnees.b.getId())).extracting(ReferenceEntiteDTO::getId)
                .containsExactlyInAnyOrder(donnees.di1.getId(), donnees.di2.getId());

        // P2 et E1 (sous P1) : deux racines, du niveau le plus haut au plus bas.
        assertThat(perimetreRepository.racines(donnees.c.getId())).extracting(ReferenceEntiteDTO::getNiveau)
                .containsExactly(NiveauHierarchique.PAROISSE, NiveauHierarchique.EGLISE);

        // Responsable d'une ressource seulement : aucune racine.
        assertThat(perimetreRepository.racines(donnees.x.getId())).isEmpty();
    }

    // ------------------------------------------------------------------ Suppression logique

    @Test
    void suppressionRefuseeTantQuUneFilleDirecteEstActive() {
        assertThat(hierarchieService.refusSuppression(NiveauHierarchique.DIOCESE, donnees.d.getId())).isPresent();
        assertThat(hierarchieService.refusSuppression(NiveauHierarchique.DISTRICT, donnees.di1.getId())).isPresent();
        assertThat(hierarchieService.refusSuppression(NiveauHierarchique.EGLISE, donnees.e1.getId())).isPresent();
        // E2 n'a aucune ressource.
        assertThat(hierarchieService.refusSuppression(NiveauHierarchique.EGLISE, donnees.e2.getId())).isEmpty();
    }

    @Test
    void egliseSupprimeeLogiquementEtIgnoreePartout() {
        supprimerEtVider(() -> egliseRepository.deleteById(donnees.e4.getId()));

        // La ligne existe toujours, à etat = -1.
        assertThat(etat("eglise", donnees.e4.getId()).intValue()).isEqualTo(-1);

        // JPA : fiche, liste des enfants.
        assertThat(egliseRepository.findById(donnees.e4.getId())).isEmpty();
        assertThat(hierarchieService.getEglisesDeLaParoisse(donnees.p2.getId()).orElseThrow()).isEmpty();

        // SQL natif : statistiques, géographie, périmètre.
        assertThat(statistiqueRepository
                .statistiquesParEntite(NiveauHierarchique.DIOCESE, NiveauHierarchique.DIOCESE, donnees.d.getId()).get(0).eglises())
                .isEqualTo(3);
        assertThat(geographieRepository.eglisesRattachees(NiveauHierarchique.DIOCESE, donnees.d.getId()))
                .extracting(dto -> dto.getId()).doesNotContain(donnees.e4.getId());
        assertThat(perimetreRepository.chaineResponsables(EntiteProtegee.EGLISE, donnees.e4.getId())).isEmpty();

        // P2 n'a plus de fille active : sa suppression devient possible.
        assertThat(hierarchieService.refusSuppression(NiveauHierarchique.PAROISSE, donnees.p2.getId())).isEmpty();
    }

    @Test
    void seulesLesFillesDirectesComptent() {
        // DI2 → P2 → E4 : on supprime E4 puis P2 ; DI2 n'a plus de fille directe active.
        supprimerEtVider(() -> egliseRepository.deleteById(donnees.e4.getId()));
        supprimerEtVider(() -> paroisseRepository.deleteById(donnees.p2.getId()));

        assertThat(hierarchieService.refusSuppression(NiveauHierarchique.DISTRICT, donnees.di2.getId())).isEmpty();
        assertThat(perimetreRepository.racines(donnees.c.getId())).extracting(ReferenceEntiteDTO::getNiveau)
                .containsExactly(NiveauHierarchique.EGLISE);

        supprimerEtVider(() -> districtRepository.deleteById(donnees.di2.getId()));
        assertThat(geographieRepository.rechercher("district", donnees.di2.getNom(), 10)).isEmpty();
        assertThat(statistiqueRepository
                .statistiquesParEntite(NiveauHierarchique.REGION, NiveauHierarchique.REGION, donnees.r1.getId()).get(0).districts())
                .isEqualTo(1);
    }

    @Test
    void clergeResponsableActifPuisSupprimeLogiquement() {
        assertThat(hierarchieService.estResponsableActif(donnees.x.getId())).isTrue();

        List<Ressource> ressources = ressourceRepository.findByEgliseIdOrderByNomAsc(donnees.e1.getId());
        Ressource ressourceDeX = ressources.stream()
                .filter(ressource -> ressource.getResponsable().getId().equals(donnees.x.getId())).findFirst().orElseThrow();
        supprimerEtVider(() -> ressourceRepository.deleteById(ressourceDeX.getId()));

        assertThat(hierarchieService.estResponsableActif(donnees.x.getId())).isFalse();

        supprimerEtVider(() -> clergeRepository.deleteById(donnees.x.getId()));
        assertThat(clergeRepository.findById(donnees.x.getId())).isEmpty();
        assertThat(clergeRepository.findAvecRoleById(donnees.x.getId())).isEmpty();
        assertThat(etat("clerge", donnees.x.getId()).intValue()).isEqualTo(-1);
    }

    @Test
    void listesPubliquesSansLesElementsSupprimes() {
        supprimerEtVider(() -> egliseRepository.deleteById(donnees.e2.getId()));

        List<EgliseDTO> eglises = hierarchieService.getEglisesDeLaParoisse(donnees.p1.getId()).orElseThrow();
        assertThat(eglises).extracting(EgliseDTO::getId)
                .containsExactlyInAnyOrder(donnees.e1.getId(), donnees.e3.getId());
        assertThat(egliseRepository.findAll()).extracting(eglise -> eglise.getId()).doesNotContain(donnees.e2.getId());
    }
}
