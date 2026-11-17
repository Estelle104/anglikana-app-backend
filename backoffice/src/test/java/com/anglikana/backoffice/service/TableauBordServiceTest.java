package com.anglikana.backoffice.service;

import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.dto.ReferenceEntiteDTO;
import com.anglikana.backoffice.dto.StatistiquesEntiteDTO;
import com.anglikana.backoffice.dto.TableauBordDTO;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.EgliseRepository;
import com.anglikana.backoffice.repository.HierarchieStatistiqueRepository;
import com.anglikana.backoffice.repository.HierarchieStatistiqueRepository.LigneStatistique;
import com.anglikana.backoffice.repository.HierarchieStatistiqueRepository.Responsabilite;
import com.anglikana.backoffice.repository.RessourceRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Règles métier du tableau de bord, indépendamment de la base de données. */
class TableauBordServiceTest {

    private HierarchieStatistiqueRepository statistiqueRepository;
    private ClergeRepository clergeRepository;
    private EgliseRepository egliseRepository;
    private TableauBordService service;

    @BeforeEach
    void preparer() {
        statistiqueRepository = mock(HierarchieStatistiqueRepository.class);
        clergeRepository = mock(ClergeRepository.class);
        egliseRepository = mock(EgliseRepository.class);
        service = new TableauBordService(statistiqueRepository, clergeRepository, egliseRepository, mock(RessourceRepository.class));
    }

    @Test
    void uneEntiteNeCompteQueSesDescendants() {
        // Comptages bruts : la ligne d'un district inclut le district lui-même.
        LigneStatistique ligne = new LigneStatistique(7, "District", null, 0, 0, 1, 3, 5, 2, 4);

        StatistiquesEntiteDTO stats = TableauBordService.versStatistiques(NiveauHierarchique.DISTRICT, ligne);

        assertThat(stats.getRegions()).isZero();
        assertThat(stats.getDistricts()).isZero();
        assertThat(stats.getParoisses()).isEqualTo(3);
        assertThat(stats.getEglises()).isEqualTo(5);
        assertThat(stats.getRessources()).isEqualTo(2);
        assertThat(stats.getClerges()).isEqualTo(4);
    }

    @Test
    void uneEgliseNeCompteNiEglisesNiNiveauxSuperieurs() {
        LigneStatistique ligne = new LigneStatistique(1, "Église", 3, 0, 0, 0, 0, 1, 2, 1);

        StatistiquesEntiteDTO stats = TableauBordService.versStatistiques(NiveauHierarchique.EGLISE, ligne);

        assertThat(stats.getEglises()).isZero();
        assertThat(stats.getParoisses()).isZero();
        assertThat(stats.getRessources()).isEqualTo(2);
        assertThat(stats.getClerges()).isEqualTo(1);
    }

    @Test
    void niveauEnfantsNonProposeRevientAuNiveauDirectementInferieur() {
        preparerRegion();

        TableauBordDTO tableau = service.getTableauBord(NiveauHierarchique.REGION, 1, NiveauHierarchique.EGLISE).orElseThrow();

        assertThat(tableau.getVuesEnfants()).containsExactly(NiveauHierarchique.DISTRICT, NiveauHierarchique.PAROISSE);
        assertThat(tableau.getNiveauEnfants()).isEqualTo(NiveauHierarchique.DISTRICT);
        verify(statistiqueRepository).statistiquesParEntite(NiveauHierarchique.DISTRICT, NiveauHierarchique.REGION, 1);
    }

    @Test
    void niveauEnfantsProposeEstRespecte() {
        preparerRegion();

        TableauBordDTO tableau = service.getTableauBord(NiveauHierarchique.REGION, 1, NiveauHierarchique.PAROISSE).orElseThrow();

        assertThat(tableau.getNiveauEnfants()).isEqualTo(NiveauHierarchique.PAROISSE);
    }

    @Test
    void enfantsTriesAlphabetiquementEtClergeRegroupe() {
        preparerRegion();
        when(statistiqueRepository.statistiquesParEntite(NiveauHierarchique.DISTRICT, NiveauHierarchique.REGION, 1)).thenReturn(List.of(
                new LigneStatistique(11, "Zébu", 5, 0, 0, 1, 0, 0, 0, 1),
                new LigneStatistique(12, "Église haute", null, 0, 0, 1, 0, 0, 0, 0),
                new LigneStatistique(13, "Ambohidratrimo", 5, 0, 0, 1, 0, 0, 0, 1)));
        when(statistiqueRepository.responsabilites(NiveauHierarchique.REGION, 1)).thenReturn(List.of(
                new Responsabilite(5, "Région"), new Responsabilite(5, "Zébu"), new Responsabilite(6, "Paroisse")));
        when(clergeRepository.findAllById(anyCollection())).thenReturn(List.of(
                Clerge.builder().id(5).nom("Rakoto").build(), Clerge.builder().id(6).nom("Rabe").build()));

        TableauBordDTO tableau = service.getTableauBord(NiveauHierarchique.REGION, 1, null).orElseThrow();

        assertThat(tableau.getEnfants()).extracting(ReferenceEntiteDTO::getNom)
                .containsExactly("Ambohidratrimo", "Église haute", "Zébu");
        assertThat(tableau.getEnfants().get(0).getResponsable().getNom()).isEqualTo("Rakoto");
        assertThat(tableau.getEnfants().get(1).getResponsable()).isNull();
        assertThat(tableau.getClerges()).hasSize(2);
        assertThat(tableau.getClerges().get(0).getResponsabilites()).containsExactly("Région", "Zébu");
        assertThat(tableau.getAncetres()).extracting(ReferenceEntiteDTO::getNiveau).containsExactly(NiveauHierarchique.DIOCESE);
    }

    @Test
    void entiteInexistanteRenvoieVide() {
        when(statistiqueRepository.chaine(NiveauHierarchique.DIOCESE, 99)).thenReturn(List.of());

        assertThat(service.getTableauBord(NiveauHierarchique.DIOCESE, 99, null)).isEmpty();
        verify(statistiqueRepository, never()).statistiquesParEntite(any(), any(), any());
        verify(egliseRepository, never()).findById(eq(99));
    }

    private void preparerRegion() {
        when(statistiqueRepository.chaine(NiveauHierarchique.REGION, 1)).thenReturn(List.of(
                new ReferenceEntiteDTO(NiveauHierarchique.DIOCESE, 1, "Diocèse"),
                new ReferenceEntiteDTO(NiveauHierarchique.REGION, 1, "Région")));
        when(statistiqueRepository.statistiquesParEntite(NiveauHierarchique.REGION, NiveauHierarchique.REGION, 1))
                .thenReturn(List.of(new LigneStatistique(1, "Région", 5, 0, 1, 3, 0, 0, 0, 2)));
    }
}
