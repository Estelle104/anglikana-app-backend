package com.anglikana.backoffice.sprint2;

import com.anglikana.backoffice.config.SecurityConfig;
import com.anglikana.backoffice.controller.DioceseController;
import com.anglikana.backoffice.controller.GeographieController;
import com.anglikana.backoffice.controller.TableauBordController;
import com.anglikana.backoffice.dto.ImportLimitesResultatDTO;
import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.dto.NoeudHierarchiqueDTO;
import com.anglikana.backoffice.dto.ReferenceEntiteDTO;
import com.anglikana.backoffice.dto.RegionDTO;
import com.anglikana.backoffice.dto.ResultatRechercheDTO;
import com.anglikana.backoffice.dto.StatistiquesEntiteDTO;
import com.anglikana.backoffice.dto.TableauBordDTO;
import com.anglikana.backoffice.dto.ZoneFeatureCollectionDTO;
import com.anglikana.backoffice.dto.ZoneFeatureDTO;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.DioceseRepository;
import com.anglikana.backoffice.service.CustomUserDetailsService;
import com.anglikana.backoffice.service.GeographieService;
import com.anglikana.backoffice.service.HierarchieService;
import com.anglikana.backoffice.service.JwtService;
import com.anglikana.backoffice.service.TableauBordService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {TableauBordController.class, GeographieController.class, DioceseController.class})
@Import(SecurityConfig.class)
class Sprint2ControllerTest {

    private static final String COLLECTION_VIDE = "{\"type\":\"FeatureCollection\",\"features\":[]}";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TableauBordService tableauBordService;
    @MockitoBean private GeographieService geographieService;
    @MockitoBean private HierarchieService hierarchieService;
    @MockitoBean private DioceseRepository dioceseRepository;
    @MockitoBean private ClergeRepository clergeRepository;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService userDetailsService;

    // ------------------------------------------------------------------ Consultation hiérarchique

    @Test
    void regionsDUnDiocese() throws Exception {
        RegionDTO region = new RegionDTO();
        region.setId(3);
        region.setNom("Analamanga");
        region.setDioceseId(1);
        when(hierarchieService.getRegionsDuDiocese(1)).thenReturn(Optional.of(List.of(region)));

        mockMvc.perform(get("/api/dioceses/1/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].dioceseId").value(1));
    }

    @Test
    void regionsDUnDioceseInexistant() throws Exception {
        when(hierarchieService.getRegionsDuDiocese(9)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/dioceses/9/regions")).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ Tableau de bord

    @Test
    void tableauBordPublicEnLectureAvecNiveauxEnCode() throws Exception {
        NoeudHierarchiqueDTO entite = new NoeudHierarchiqueDTO();
        entite.setNiveau(NiveauHierarchique.DIOCESE);
        entite.setId(1);
        entite.setNom("Diocèse");
        StatistiquesEntiteDTO statistiques = new StatistiquesEntiteDTO();
        statistiques.setEglises(12);
        entite.setStatistiques(statistiques);

        TableauBordDTO tableau = new TableauBordDTO();
        tableau.setEntite(entite);
        tableau.setAncetres(List.of());
        tableau.setNiveauEnfants(NiveauHierarchique.DISTRICT);
        tableau.setVuesEnfants(List.of(NiveauHierarchique.REGION, NiveauHierarchique.DISTRICT));
        when(tableauBordService.getTableauBord(NiveauHierarchique.DIOCESE, 1, NiveauHierarchique.DISTRICT))
                .thenReturn(Optional.of(tableau));

        mockMvc.perform(get("/api/tableau-bord/diocese/1").param("niveauEnfants", "district"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entite.niveau").value("diocese"))
                .andExpect(jsonPath("$.entite.statistiques.eglises").value(12))
                .andExpect(jsonPath("$.niveauEnfants").value("district"))
                .andExpect(jsonPath("$.vuesEnfants[0]").value("region"))
                .andExpect(jsonPath("$.eglise").isEmpty());
    }

    @Test
    void tableauBordSansNiveauEnfants() throws Exception {
        when(tableauBordService.getTableauBord(eq(NiveauHierarchique.REGION), eq(2), isNull()))
                .thenReturn(Optional.of(new TableauBordDTO()));

        mockMvc.perform(get("/api/tableau-bord/region/2")).andExpect(status().isOk());
    }

    @Test
    void tableauBordNiveauInconnu() throws Exception {
        mockMvc.perform(get("/api/tableau-bord/eveche/1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Niveau hiérarchique inconnu : eveche"));
        verify(tableauBordService, never()).getTableauBord(any(), any(), any());
    }

    @Test
    void tableauBordNiveauEnfantsInconnu() throws Exception {
        mockMvc.perform(get("/api/tableau-bord/diocese/1").param("niveauEnfants", "x"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tableauBordEntiteInexistante() throws Exception {
        when(tableauBordService.getTableauBord(NiveauHierarchique.PAROISSE, 404, null)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tableau-bord/paroisse/404")).andExpect(status().isNotFound());
    }

    @Test
    void statistiquesDUneEntite() throws Exception {
        StatistiquesEntiteDTO statistiques = new StatistiquesEntiteDTO();
        statistiques.setClerges(4);
        when(tableauBordService.getStatistiques(NiveauHierarchique.DISTRICT, 5)).thenReturn(Optional.of(statistiques));

        mockMvc.perform(get("/api/tableau-bord/district/5/statistiques"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clerges").value(4));
    }

    // ------------------------------------------------------------------ Géographie

    @Test
    void limitesRenvoyeesEnGeoJsonExploitableDirectement() throws Exception {
        ZoneFeatureDTO feature = new ZoneFeatureDTO(new ReferenceEntiteDTO(NiveauHierarchique.REGION, 2, "Région"),
                "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[47,-19],[48,-19],[48,-18],[47,-19]]]]}");
        when(geographieService.getLimites(NiveauHierarchique.REGION)).thenReturn(new ZoneFeatureCollectionDTO(List.of(feature)));

        mockMvc.perform(get("/api/geographie/region/limites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features[0].type").value("Feature"))
                .andExpect(jsonPath("$.features[0].properties.niveau").value("region"))
                .andExpect(jsonPath("$.features[0].geometry.type").value("MultiPolygon"))
                .andExpect(jsonPath("$.features[0].geometry.coordinates[0][0][0][0]").value(47));
    }

    @Test
    void niveauSansLimitesGeographiquesRefuse() throws Exception {
        mockMvc.perform(get("/api/geographie/paroisse/limites")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/geographie/eglise/1")).andExpect(status().isBadRequest());
    }

    @Test
    void rechercheGeographique() throws Exception {
        when(geographieService.rechercher(NiveauHierarchique.DISTRICT, "tana", 10))
                .thenReturn(List.of(new ResultatRechercheDTO(NiveauHierarchique.DISTRICT, 1, "Tana Ville", "Région · Diocèse")));

        mockMvc.perform(get("/api/geographie/recherche").param("q", "tana").param("niveau", "district").param("limite", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].niveau").value("district"))
                .andExpect(jsonPath("$[0].contexte").value("Région · Diocèse"));
    }

    @Test
    void rechercheParametresInvalides() throws Exception {
        mockMvc.perform(get("/api/geographie/recherche").param("limite", "500")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/geographie/recherche").param("niveau", "eglise")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/geographie/recherche").param("q", "x".repeat(151))).andExpect(status().isBadRequest());
    }

    @Test
    void zoneInexistante() throws Exception {
        when(geographieService.getZone(NiveauHierarchique.DIOCESE, 77)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/geographie/diocese/77")).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ Sécurité de l'import

    @Test
    void importDesLimitesRefuseSansAuthentification() throws Exception {
        mockMvc.perform(put("/api/geographie/district/limites").contentType(MediaType.APPLICATION_JSON).content(COLLECTION_VIDE))
                .andExpect(status().isForbidden());
        verify(geographieService, never()).importerLimites(any(), any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void importDesLimitesRefusePourUnNonAdministrateur() throws Exception {
        mockMvc.perform(put("/api/geographie/district/limites").contentType(MediaType.APPLICATION_JSON).content(COLLECTION_VIDE))
                .andExpect(status().isForbidden());
        verify(geographieService, never()).importerLimites(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void importDesLimitesParUnAdministrateur() throws Exception {
        ImportLimitesResultatDTO resultat = new ImportLimitesResultatDTO();
        resultat.setNiveau(NiveauHierarchique.DISTRICT);
        resultat.setZonesMisesAJour(3);
        when(geographieService.importerLimites(eq(NiveauHierarchique.DISTRICT), any())).thenReturn(resultat);

        mockMvc.perform(put("/api/geographie/district/limites").contentType(MediaType.APPLICATION_JSON).content(COLLECTION_VIDE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zonesMisesAJour").value(3));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void importDesLimitesDocumentInvalide() throws Exception {
        when(geographieService.importerLimites(eq(NiveauHierarchique.DISTRICT), any()))
                .thenThrow(new IllegalArgumentException("Le document doit être une FeatureCollection GeoJSON"));

        mockMvc.perform(put("/api/geographie/district/limites").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Le document doit être une FeatureCollection GeoJSON"));
    }
}
