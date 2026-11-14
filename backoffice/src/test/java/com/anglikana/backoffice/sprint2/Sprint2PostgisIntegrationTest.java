package com.anglikana.backoffice.sprint2;

import com.anglikana.backoffice.dto.ApercuHierarchieDTO;
import com.anglikana.backoffice.dto.ClergeAffecteDTO;
import com.anglikana.backoffice.dto.EgliseLocaliseeDTO;
import com.anglikana.backoffice.dto.ImportLimitesRequest;
import com.anglikana.backoffice.dto.ImportLimitesResultatDTO;
import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.dto.NoeudHierarchiqueDTO;
import com.anglikana.backoffice.dto.ReferenceEntiteDTO;
import com.anglikana.backoffice.dto.RegionDTO;
import com.anglikana.backoffice.entity.Region;
import com.anglikana.backoffice.mapper.GeometrieMapper;
import com.anglikana.backoffice.mapper.RegionMapper;
import com.anglikana.backoffice.dto.ResultatRechercheDTO;
import com.anglikana.backoffice.dto.StatistiquesEntiteDTO;
import com.anglikana.backoffice.dto.TableauBordDTO;
import com.anglikana.backoffice.dto.TotauxHierarchieDTO;
import com.anglikana.backoffice.dto.ZoneFeatureDTO;
import com.anglikana.backoffice.dto.ZoneGeographiqueDTO;
import com.anglikana.backoffice.repository.GeographieRepository;
import com.anglikana.backoffice.repository.HierarchieStatistiqueRepository;
import com.anglikana.backoffice.repository.HierarchieStatistiqueRepository.LigneStatistique;
import com.anglikana.backoffice.service.GeographieService;
import com.anglikana.backoffice.service.HierarchieService;
import com.anglikana.backoffice.service.TableauBordService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests d'intégration sur la base PostgreSQL/PostGIS configurée (application.properties).
 * Chaque test s'exécute dans une transaction annulée à la fin : aucune donnée n'est conservée.
 * Le schéma n'est jamais modifié (ddl-auto=none) ; la migration V2 doit avoir été appliquée.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@Import({HierarchieStatistiqueRepository.class, GeographieRepository.class,
        TableauBordService.class, GeographieService.class, HierarchieService.class})
class Sprint2PostgisIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired private TestEntityManager em;
    @Autowired private HierarchieStatistiqueRepository statistiqueRepository;
    @Autowired private GeographieRepository geographieRepository;
    @Autowired private TableauBordService tableauBordService;
    @Autowired private GeographieService geographieService;
    @Autowired private HierarchieService hierarchieService;

    private TotauxHierarchieDTO totauxAvant;
    private JeuDonneesHierarchie donnees;

    @BeforeEach
    void preparer() {
        totauxAvant = tableauBordService.getApercu().getTotaux();
        donnees = new JeuDonneesHierarchie().inserer(em);
    }

    // ------------------------------------------------------------------ Agrégations (repository)

    @Test
    void statistiquesDuDioceseComptentTousLesDescendantsSansDoubleComptage() {
        LigneStatistique ligne = statistiqueRepository
                .statistiquesParEntite(NiveauHierarchique.DIOCESE, NiveauHierarchique.DIOCESE, donnees.d.getId()).get(0);

        assertThat(ligne.nom()).isEqualTo(donnees.d.getNom());
        assertThat(ligne.responsableId()).isEqualTo(donnees.a.getId());
        assertThat(ligne.regions()).isEqualTo(2);
        assertThat(ligne.districts()).isEqualTo(2);
        assertThat(ligne.paroisses()).isEqualTo(2);
        assertThat(ligne.eglises()).isEqualTo(4);
        assertThat(ligne.ressources()).isEqualTo(2);
        // A, B, C : chacun responsable de plusieurs entités, compté une fois ; X (ressource seule) exclu.
        assertThat(ligne.clerges()).isEqualTo(3);
    }

    @Test
    void statistiquesDesRegionsDuDioceseIncluentLesRegionsVides() {
        Map<Integer, LigneStatistique> parRegion = new HashMap<>();
        statistiqueRepository.statistiquesParEntite(NiveauHierarchique.REGION, NiveauHierarchique.DIOCESE, donnees.d.getId())
                .forEach(ligne -> parRegion.put(ligne.id(), ligne));

        assertThat(parRegion).containsOnlyKeys(donnees.r1.getId(), donnees.r2.getId());
        LigneStatistique r1 = parRegion.get(donnees.r1.getId());
        assertThat(r1.districts()).isEqualTo(2);
        assertThat(r1.eglises()).isEqualTo(4);
        assertThat(r1.clerges()).isEqualTo(3);

        LigneStatistique r2 = parRegion.get(donnees.r2.getId());
        assertThat(r2.districts()).isZero();
        assertThat(r2.eglises()).isZero();
        assertThat(r2.clerges()).isZero();
        assertThat(r2.responsableId()).isNull();
    }

    @Test
    void sommeDesEnfantsEgaleLeTotalDuParent() {
        List<LigneStatistique> districts = statistiqueRepository
                .statistiquesParEntite(NiveauHierarchique.DISTRICT, NiveauHierarchique.REGION, donnees.r1.getId());
        LigneStatistique region = statistiqueRepository
                .statistiquesParEntite(NiveauHierarchique.REGION, NiveauHierarchique.REGION, donnees.r1.getId()).get(0);

        assertThat(districts.stream().mapToLong(LigneStatistique::eglises).sum()).isEqualTo(region.eglises());
        assertThat(districts.stream().mapToLong(LigneStatistique::paroisses).sum()).isEqualTo(region.paroisses());
        assertThat(districts.stream().mapToLong(LigneStatistique::ressources).sum()).isEqualTo(region.ressources());
    }

    @Test
    void chaineHierarchiqueDuDioceseALEglise() {
        List<ReferenceEntiteDTO> chaine = statistiqueRepository.chaine(NiveauHierarchique.EGLISE, donnees.e4.getId());

        assertThat(chaine).extracting(ReferenceEntiteDTO::getNiveau).containsExactly(
                NiveauHierarchique.DIOCESE, NiveauHierarchique.REGION, NiveauHierarchique.DISTRICT,
                NiveauHierarchique.PAROISSE, NiveauHierarchique.EGLISE);
        assertThat(chaine).extracting(ReferenceEntiteDTO::getId).containsExactly(
                donnees.d.getId(), donnees.r1.getId(), donnees.di2.getId(), donnees.p2.getId(), donnees.e4.getId());
        assertThat(statistiqueRepository.chaine(NiveauHierarchique.DISTRICT, -1)).isEmpty();
    }

    // ------------------------------------------------------------------ Tableau de bord (service)

    @Test
    void apercuAugmenteExactementDuJeuDeDonnees() {
        ApercuHierarchieDTO apercu = tableauBordService.getApercu();
        TotauxHierarchieDTO apres = apercu.getTotaux();

        assertThat(apres.getDioceses() - totauxAvant.getDioceses()).isEqualTo(1);
        assertThat(apres.getRegions() - totauxAvant.getRegions()).isEqualTo(2);
        assertThat(apres.getDistricts() - totauxAvant.getDistricts()).isEqualTo(2);
        assertThat(apres.getParoisses() - totauxAvant.getParoisses()).isEqualTo(2);
        assertThat(apres.getEglises() - totauxAvant.getEglises()).isEqualTo(4);
        assertThat(apres.getRessources() - totauxAvant.getRessources()).isEqualTo(2);
        assertThat(apres.getClerges() - totauxAvant.getClerges()).isEqualTo(3);
        assertThat(apercu.getDioceses()).extracting(NoeudHierarchiqueDTO::getId).contains(donnees.d.getId());
    }

    @Test
    void tableauBordDuDioceseListeLesRegionsEtLeClergeDistinct() {
        TableauBordDTO tableau = tableauBordService.getTableauBord(NiveauHierarchique.DIOCESE, donnees.d.getId(), null).orElseThrow();

        StatistiquesEntiteDTO stats = tableau.getEntite().getStatistiques();
        assertThat(stats.getRegions()).isEqualTo(2);
        assertThat(stats.getEglises()).isEqualTo(4);
        assertThat(stats.getClerges()).isEqualTo(3);
        assertThat(tableau.getEntite().getResponsable().getId()).isEqualTo(donnees.a.getId());
        assertThat(tableau.getAncetres()).isEmpty();
        assertThat(tableau.getVuesEnfants()).containsExactly(NiveauHierarchique.REGION, NiveauHierarchique.DISTRICT);
        assertThat(tableau.getNiveauEnfants()).isEqualTo(NiveauHierarchique.REGION);
        // Tri alphabétique français : « Deux » avant « Un ».
        assertThat(tableau.getEnfants()).extracting(NoeudHierarchiqueDTO::getId)
                .containsExactly(donnees.r2.getId(), donnees.r1.getId());

        assertThat(tableau.getClerges()).extracting(affecte -> affecte.getClerge().getId())
                .containsExactly(donnees.a.getId(), donnees.b.getId(), donnees.c.getId());
        ClergeAffecteDTO clergeA = tableau.getClerges().get(0);
        assertThat(clergeA.getResponsabilites()).containsExactly(
                donnees.d.getNom(), donnees.r1.getNom(), donnees.e4.getNom());
        assertThat(tableau.getEglise()).isNull();
        assertThat(tableau.getRessources()).isEmpty();
    }

    @Test
    void tableauBordDuDistrictAvecVueEglises() {
        TableauBordDTO tableau = tableauBordService
                .getTableauBord(NiveauHierarchique.DISTRICT, donnees.di1.getId(), NiveauHierarchique.EGLISE).orElseThrow();

        assertThat(tableau.getNiveauEnfants()).isEqualTo(NiveauHierarchique.EGLISE);
        assertThat(tableau.getEnfants()).hasSize(3);
        assertThat(tableau.getAncetres()).extracting(ReferenceEntiteDTO::getId)
                .containsExactly(donnees.d.getId(), donnees.r1.getId());
        StatistiquesEntiteDTO stats = tableau.getEntite().getStatistiques();
        assertThat(stats.getDistricts()).isZero();
        assertThat(stats.getParoisses()).isEqualTo(1);
        assertThat(stats.getEglises()).isEqualTo(3);
        assertThat(stats.getClerges()).isEqualTo(2);
    }

    @Test
    void tableauBordDeLaParoisse() {
        StatistiquesEntiteDTO stats = tableauBordService
                .getStatistiques(NiveauHierarchique.PAROISSE, donnees.p1.getId()).orElseThrow();

        assertThat(stats.getParoisses()).isZero();
        assertThat(stats.getEglises()).isEqualTo(3);
        assertThat(stats.getRessources()).isEqualTo(2);
        assertThat(stats.getClerges()).isEqualTo(2);
    }

    @Test
    void tableauBordDeLEgliseContientSesRessourcesDetaillees() {
        TableauBordDTO tableau = tableauBordService.getTableauBord(NiveauHierarchique.EGLISE, donnees.e1.getId(), null).orElseThrow();

        assertThat(tableau.getNiveauEnfants()).isNull();
        assertThat(tableau.getVuesEnfants()).isEmpty();
        assertThat(tableau.getEnfants()).isEmpty();
        assertThat(tableau.getEntite().getStatistiques().getEglises()).isZero();
        assertThat(tableau.getEntite().getStatistiques().getRessources()).isEqualTo(2);
        assertThat(tableau.getEntite().getStatistiques().getClerges()).isEqualTo(1);
        assertThat(tableau.getEglise().getLongitude()).isEqualTo(47.5);
        assertThat(tableau.getRessources()).hasSize(2);
        assertThat(tableau.getRessources()).allSatisfy(ressource -> assertThat(ressource.getTypeNom()).isEqualTo("Épreuve École"));
        assertThat(tableau.getRessources()).extracting(ressource -> ressource.getResponsable().getId())
                .containsExactlyInAnyOrder(donnees.c.getId(), donnees.x.getId());
    }

    @Test
    void entiteInexistante() {
        assertThat(tableauBordService.getTableauBord(NiveauHierarchique.REGION, -1, null)).isEmpty();
        assertThat(tableauBordService.getStatistiques(NiveauHierarchique.EGLISE, -1)).isEmpty();
        assertThat(hierarchieService.getParoissesDuDistrict(-1)).isEmpty();
    }

    @Test
    void consultationHierarchiqueDesEnfants() {
        assertThat(hierarchieService.getRegionsDuDiocese(donnees.d.getId()).orElseThrow()).hasSize(2);
        assertThat(hierarchieService.getDistrictsDuDiocese(donnees.d.getId()).orElseThrow()).hasSize(2);
        assertThat(hierarchieService.getDistrictsDeLaRegion(donnees.r2.getId()).orElseThrow()).isEmpty();
        assertThat(hierarchieService.getEglisesDeLaParoisse(donnees.p1.getId()).orElseThrow()).hasSize(3);
        assertThat(hierarchieService.getRessourcesDeLEglise(donnees.e1.getId()).orElseThrow()).hasSize(2);
    }

    // ------------------------------------------------------------------ Géographie (PostGIS)

    @Test
    void eglisesDansLePolygoneFrontiereIncluse() {
        geographieRepository.majCarteQgis(NiveauHierarchique.DISTRICT, donnees.di1.getId(), JeuDonneesHierarchie.CARRE_DI1);

        List<EgliseLocaliseeDTO> dansZone = geographieRepository.eglisesDansZone(NiveauHierarchique.DISTRICT, donnees.di1.getId());
        // E1 à l'intérieur, E2 sur la frontière (ST_Covers), E4 à l'intérieur bien que rattachée à Di2.
        assertThat(dansZone).extracting(EgliseLocaliseeDTO::getId)
                .contains(donnees.e1.getId(), donnees.e2.getId(), donnees.e4.getId())
                .doesNotContain(donnees.e3.getId());

        List<EgliseLocaliseeDTO> horsZone = geographieRepository.eglisesHorsZone(NiveauHierarchique.DISTRICT, donnees.di1.getId());
        assertThat(horsZone).extracting(EgliseLocaliseeDTO::getId).containsExactly(donnees.e3.getId());
        assertThat(horsZone.get(0).getLongitude()).isEqualTo(49.5);
        assertThat(horsZone.get(0).getParoisseNom()).isEqualTo(donnees.p1.getNom());
    }

    @Test
    void carteQgisEnregistreeParLeCrudEstUtiliseeParLaCarte() {
        // Chemin du CRUD : DTO GeoJSON → entité JPA (Polygon) → colonne carte_qgis.
        donnees.r2.setCarteQgis(GeometrieMapper.depuisGeoJson(JSON.readTree(JeuDonneesHierarchie.CARRE_DI1)));
        em.flush();
        em.clear();

        // Lecture par la carte (SQL natif PostGIS).
        ZoneGeographiqueDTO zone = geographieService.getZone(NiveauHierarchique.REGION, donnees.r2.getId()).orElseThrow();
        assertThat(zone.isLimiteDisponible()).isTrue();
        assertThat(zone.getFeature().getGeometry()).startsWith("{\"type\":\"Polygon\"");
        assertThat(zone.getEglises()).extracting(EgliseLocaliseeDTO::getId)
                .contains(donnees.e1.getId(), donnees.e2.getId(), donnees.e4.getId());
        // Toutes les églises localisées de R1 sont hors de R2 : aucune n'est rattachée à R2.
        assertThat(zone.getEglisesHorsZone()).isEmpty();

        // Relecture par le CRUD : entité → DTO GeoJSON.
        RegionDTO dto = RegionMapper.toDTO(em.find(Region.class, donnees.r2.getId()));
        assertThat(dto.getCarteQgis().get("type").asString()).isEqualTo("Polygon");
        assertThat(GeometrieMapper.depuisGeoJson(dto.getCarteQgis()).getArea()).isEqualTo(1.0);
    }

    @Test
    void zoneSansLimiteRenvoieLesEglisesRattachees() {
        ZoneGeographiqueDTO zone = geographieService.getZone(NiveauHierarchique.DIOCESE, donnees.d.getId()).orElseThrow();

        assertThat(zone.isLimiteDisponible()).isFalse();
        assertThat(zone.getFeature().getGeometry()).isNull();
        assertThat(zone.getContexte()).isEqualTo("Diocèse");
        assertThat(zone.getEglises()).hasSize(4);
        assertThat(zone.getEglisesHorsZone()).isEmpty();
        assertThat(zone.getSousZones()).isEmpty();
    }

    @Test
    void importDesLimitesQgisPuisConsultationDeLaZone() {
        ImportLimitesResultatDTO resultat = geographieService.importerLimites(NiveauHierarchique.DISTRICT, collection(
                feature(donnees.di1.getId(), JeuDonneesHierarchie.CARRE_DI1),
                feature("999999999", JeuDonneesHierarchie.CARRE_DI1),
                feature(null, JeuDonneesHierarchie.CARRE_DI1),
                feature(donnees.di2.getId(), "{\"type\":\"Point\",\"coordinates\":[47,-18]}")));

        assertThat(resultat.getZonesMisesAJour()).isEqualTo(1);
        assertThat(resultat.getIdsInconnus()).containsExactly(999999999);
        assertThat(resultat.getErreurs()).hasSize(2);

        List<ZoneFeatureDTO> couche = geographieService.getLimites(NiveauHierarchique.DISTRICT).getFeatures();
        ZoneFeatureDTO feature = couche.stream()
                .filter(f -> f.getProperties().getId().equals(donnees.di1.getId())).findFirst().orElseThrow();
        assertThat(feature.getGeometry()).startsWith("{\"type\":\"Polygon\"");
        assertThat(couche).noneMatch(f -> f.getProperties().getId().equals(donnees.di2.getId()));

        ZoneGeographiqueDTO zone = geographieService.getZone(NiveauHierarchique.DISTRICT, donnees.di1.getId()).orElseThrow();
        assertThat(zone.isLimiteDisponible()).isTrue();
        assertThat(zone.getContexte()).isEqualTo(donnees.r1.getNom() + " · " + donnees.d.getNom());
        assertThat(zone.getEglisesHorsZone()).extracting(EgliseLocaliseeDTO::getId).containsExactly(donnees.e3.getId());

        // La région R1 expose Di1 comme sous-zone (seules les zones avec limite sont renvoyées).
        ZoneGeographiqueDTO region = geographieService.getZone(NiveauHierarchique.REGION, donnees.r1.getId()).orElseThrow();
        assertThat(region.getSousZones()).extracting(f -> f.getProperties().getId()).containsExactly(donnees.di1.getId());
    }

    @Test
    void geometriesNonConformesSignaleesSansBloquerLImport() {
        String deuxParties = "{\"type\":\"MultiPolygon\",\"coordinates\":["
                + "[[[47,-19],[48,-19],[48,-18],[47,-19]]],[[[49,-19],[50,-19],[50,-18],[49,-19]]]]}";

        ImportLimitesResultatDTO resultat = geographieService.importerLimites(NiveauHierarchique.DISTRICT, collection(
                feature(donnees.di1.getId(), JeuDonneesHierarchie.CARRE_DI1),
                feature(donnees.di2.getId(), "{\"type\":\"Polygon\",\"coordinates\":[[[47]]]}"),
                feature(donnees.di2.getId(), deuxParties)));

        assertThat(resultat.getZonesMisesAJour()).isEqualTo(1);
        assertThat(resultat.getErreurs()).hasSize(2);
        assertThat(resultat.getErreurs().get(0)).contains("Feature n°2").contains("4 positions");
        assertThat(resultat.getErreurs().get(1)).contains("Feature n°3").contains("seul tenant");
        assertThat(geographieService.getZone(NiveauHierarchique.DISTRICT, donnees.di2.getId()).orElseThrow()
                .isLimiteDisponible()).isFalse();
    }

    @Test
    void documentQuiNEstPasUneFeatureCollectionRefuse() {
        ImportLimitesRequest request = new ImportLimitesRequest();
        request.setType("Feature");

        assertThatThrownBy(() -> geographieService.importerLimites(NiveauHierarchique.DISTRICT, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FeatureCollection");
    }

    @Test
    void rechercheInsensibleAuxAccentsEtParIdentifiant() {
        List<ResultatRechercheDTO> parNom = geographieService.rechercher(null, "epreuve district", 50);
        assertThat(parNom).extracting(ResultatRechercheDTO::getId)
                .containsExactlyInAnyOrder(donnees.di1.getId(), donnees.di2.getId());
        assertThat(parNom.get(0).getContexte()).isEqualTo(donnees.r1.getNom() + " · " + donnees.d.getNom());

        List<ResultatRechercheDTO> parId = geographieService.rechercher(NiveauHierarchique.REGION, String.valueOf(donnees.r2.getId()), 50);
        assertThat(parId).extracting(ResultatRechercheDTO::getId).contains(donnees.r2.getId());
        assertThat(parId).allMatch(resultat -> resultat.getNiveau() == NiveauHierarchique.REGION);

        // Les jokers LIKE sont traités littéralement.
        assertThat(geographieService.rechercher(null, "%", 50)).isEmpty();
    }

    private static ImportLimitesRequest collection(ImportLimitesRequest.Feature... features) {
        ImportLimitesRequest request = new ImportLimitesRequest();
        request.setType("FeatureCollection");
        request.setFeatures(new ArrayList<>(List.of(features)));
        return request;
    }

    private static ImportLimitesRequest.Feature feature(Object id, String geometrie) {
        ImportLimitesRequest.Feature feature = new ImportLimitesRequest.Feature();
        Map<String, Object> proprietes = new HashMap<>();
        if (id != null) {
            proprietes.put("id", id);
        }
        feature.setProperties(proprietes);
        feature.setGeometry(JSON.readTree(geometrie));
        return feature;
    }
}
