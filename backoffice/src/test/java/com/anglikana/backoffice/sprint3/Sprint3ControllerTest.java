package com.anglikana.backoffice.sprint3;

import com.anglikana.backoffice.config.MethodSecurityConfig;
import com.anglikana.backoffice.config.SecurityConfig;
import com.anglikana.backoffice.controller.AuthController;
import com.anglikana.backoffice.controller.ClergeController;
import com.anglikana.backoffice.controller.DioceseController;
import com.anglikana.backoffice.controller.DistrictController;
import com.anglikana.backoffice.controller.MoiController;
import com.anglikana.backoffice.controller.ParoisseController;
import com.anglikana.backoffice.controller.RegionController;
import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.dto.ReferenceEntiteDTO;
import com.anglikana.backoffice.entity.Diocese;
import com.anglikana.backoffice.entity.District;
import com.anglikana.backoffice.entity.Paroisse;
import com.anglikana.backoffice.entity.Region;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.DioceseRepository;
import com.anglikana.backoffice.repository.DistrictRepository;
import com.anglikana.backoffice.repository.EgliseRepository;
import com.anglikana.backoffice.repository.ParoisseRepository;
import com.anglikana.backoffice.repository.PerimetreRepository;
import com.anglikana.backoffice.repository.RegionRepository;
import com.anglikana.backoffice.repository.RessourceRepository;
import com.anglikana.backoffice.repository.RoleRepository;
import com.anglikana.backoffice.repository.TypeRessourceRepository;
import com.anglikana.backoffice.repository.UtilisateurRepository;
import com.anglikana.backoffice.security.AutorisationService;
import com.anglikana.backoffice.security.EntiteProtegee;
import com.anglikana.backoffice.security.WithUtilisateurConnecte;
import com.anglikana.backoffice.service.CustomUserDetailsService;
import com.anglikana.backoffice.service.HierarchieService;
import com.anglikana.backoffice.service.JwtService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 3 : droits par niveau et par périmètre (annotations @LevelRequired, @NeedAuthorisation,
 * @CascadeAuthorised) et suppression logique. La base est simulée : PerimetreRepository renvoie
 * la chaîne des responsables (entité, puis ses ancêtres jusqu'au diocèse).
 */
@WebMvcTest(controllers = {DioceseController.class, RegionController.class, DistrictController.class,
        ParoisseController.class, ClergeController.class, AuthController.class, MoiController.class})
@Import({SecurityConfig.class, MethodSecurityConfig.class, AutorisationService.class, HierarchieService.class})
class Sprint3ControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PerimetreRepository perimetreRepository;
    @MockitoBean private DioceseRepository dioceseRepository;
    @MockitoBean private RegionRepository regionRepository;
    @MockitoBean private DistrictRepository districtRepository;
    @MockitoBean private ParoisseRepository paroisseRepository;
    @MockitoBean private EgliseRepository egliseRepository;
    @MockitoBean private RessourceRepository ressourceRepository;
    @MockitoBean private TypeRessourceRepository typeRessourceRepository;
    @MockitoBean private ClergeRepository clergeRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private UtilisateurRepository utilisateurRepository;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService userDetailsService;

    private void chaine(EntiteProtegee entite, int id, Integer... responsables) {
        when(perimetreRepository.chaineResponsables(entite, id)).thenReturn(Optional.of(Arrays.asList(responsables)));
    }

    // ------------------------------------------------------------------ Périmètre en écriture

    @Test
    @WithUtilisateurConnecte(clergeId = 3, niveau = 3)
    void doyenModifieUneParoisseDeSonDistrict() throws Exception {
        chaine(EntiteProtegee.PAROISSE, 1, null, 3, 1, 1);
        when(paroisseRepository.findById(1)).thenReturn(Optional.of(
                Paroisse.builder().id(1).nom("Saint-Joseph").district(District.builder().id(1).build()).build()));
        when(paroisseRepository.save(any(Paroisse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/paroisses/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\": \"Saint-Joseph\", \"districtId\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Saint-Joseph"));
    }

    @Test
    @WithUtilisateurConnecte(clergeId = 3, niveau = 3)
    void doyenRefuseHorsDeSonDistrict() throws Exception {
        chaine(EntiteProtegee.PAROISSE, 2, 4, 4, 2, 2);

        mockMvc.perform(put("/api/paroisses/2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\": \"Sainte-Marie\", \"districtId\": 2}"))
                .andExpect(status().isForbidden());
        verify(paroisseRepository, never()).save(any());
    }

    @Test
    @WithUtilisateurConnecte(clergeId = 10, niveau = 4)
    void archidiacreModifieSaRegionSansEtreResponsableDuDiocese() throws Exception {
        chaine(EntiteProtegee.REGION, 1, 10, 1);
        when(regionRepository.findById(1)).thenReturn(Optional.of(
                Region.builder().id(1).nom("Analamanga").diocese(Diocese.builder().id(1).build()).build()));
        when(regionRepository.save(any(Region.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/regions/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\": \"Analamanga\", \"dioceseId\": 1}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithUtilisateurConnecte(clergeId = 10, niveau = 4)
    void archidiacreNePeutPasDeplacerSaRegionHorsDeSonPerimetre() throws Exception {
        chaine(EntiteProtegee.REGION, 1, 10, 1);
        chaine(EntiteProtegee.DIOCESE, 2, 2);
        when(regionRepository.findById(1)).thenReturn(Optional.of(
                Region.builder().id(1).nom("Analamanga").diocese(Diocese.builder().id(1).build()).build()));

        mockMvc.perform(put("/api/regions/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\": \"Analamanga\", \"dioceseId\": 2}"))
                .andExpect(status().isForbidden());
        verify(regionRepository, never()).save(any());
    }

    @Test
    @WithUtilisateurConnecte(clergeId = 3, niveau = 3)
    void creationDansUnParentIntrouvable() throws Exception {
        when(perimetreRepository.chaineResponsables(EntiteProtegee.DISTRICT, 99)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/paroisses").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\": \"Nouvelle\", \"districtId\": 99}"))
                .andExpect(status().isNotFound());
        verify(paroisseRepository, never()).save(any());
    }

    @Test
    @WithUtilisateurConnecte(clergeId = 3, niveau = 3)
    void creationSansParentRefusee() throws Exception {
        mockMvc.perform(post("/api/paroisses").contentType(MediaType.APPLICATION_JSON).content("{\"nom\": \"Nouvelle\"}"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ Opérations de niveau 5

    @Test
    @WithUtilisateurConnecte(clergeId = 10, niveau = 4)
    void creationDeDioceseRefuseeSousLeNiveauCinq() throws Exception {
        mockMvc.perform(post("/api/dioceses").contentType(MediaType.APPLICATION_JSON).content("{\"nom\": \"Nouveau\"}"))
                .andExpect(status().isForbidden());
        verify(dioceseRepository, never()).save(any());
    }

    @Test
    @WithUtilisateurConnecte(clergeId = 1, niveau = 5)
    void creationDeDioceseParUnEveque() throws Exception {
        when(dioceseRepository.save(any(Diocese.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/dioceses").contentType(MediaType.APPLICATION_JSON).content("{\"nom\": \"Nouveau\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void creationDeCompteRefuseeSansAuthentification() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomUtilisateur\": \"x\", \"motDePasse\": \"y\", \"clergeId\": 1}"))
                .andExpect(status().isForbidden());
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    @WithUtilisateurConnecte(clergeId = 3, niveau = 3)
    void creationDeCompteRefuseeSousLeNiveauCinq() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomUtilisateur\": \"x\", \"motDePasse\": \"y\", \"clergeId\": 1}"))
                .andExpect(status().isForbidden());
        verify(utilisateurRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ Suppression logique

    @Test
    @WithUtilisateurConnecte(clergeId = 1, niveau = 5)
    void suppressionRefuseeTantQuUneFilleDirecteEstActive() throws Exception {
        chaine(EntiteProtegee.REGION, 1, 1, 1);
        when(regionRepository.existsById(1)).thenReturn(true);
        when(districtRepository.existsByRegionId(1)).thenReturn(true);

        mockMvc.perform(delete("/api/regions/1"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Suppression impossible : la région contient encore au moins un district actif."));
        verify(regionRepository, never()).deleteById(anyInt());
    }

    @Test
    @WithUtilisateurConnecte(clergeId = 3, niveau = 3)
    void suppressionLogiqueSansFilleActive() throws Exception {
        chaine(EntiteProtegee.DISTRICT, 5, 3, 1, 1);
        when(districtRepository.existsById(5)).thenReturn(true);
        when(paroisseRepository.existsByDistrictId(5)).thenReturn(false);

        mockMvc.perform(delete("/api/districts/5")).andExpect(status().isNoContent());
        verify(districtRepository).deleteById(5);
    }

    @Test
    @WithUtilisateurConnecte(clergeId = 1, niveau = 5)
    void suppressionDUnClergeEncoreResponsableRefusee() throws Exception {
        when(clergeRepository.existsById(3)).thenReturn(true);
        when(paroisseRepository.existsByResponsableId(3)).thenReturn(true);

        mockMvc.perform(delete("/api/clerges/3")).andExpect(status().isConflict());
        verify(clergeRepository, never()).deleteById(anyInt());
    }

    @Test
    @WithUtilisateurConnecte(clergeId = 1, niveau = 5)
    void suppressionDUnClergeSansResponsabilite() throws Exception {
        when(clergeRepository.existsById(6)).thenReturn(true);

        mockMvc.perform(delete("/api/clerges/6")).andExpect(status().isNoContent());
        verify(clergeRepository).deleteById(6);
    }

    // ------------------------------------------------------------------ Lectures

    @Test
    @WithUtilisateurConnecte(nomUtilisateur = "doyen", clergeId = 3, niveau = 3)
    void profilAvecLesRacinesDuPerimetre() throws Exception {
        when(perimetreRepository.racines(3)).thenReturn(List.of(new ReferenceEntiteDTO(NiveauHierarchique.DISTRICT, 1, "Tana Ville")));

        mockMvc.perform(get("/api/moi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomUtilisateur").value("doyen"))
                .andExpect(jsonPath("$.niveau").value(3))
                .andExpect(jsonPath("$.racines[0].niveau").value("district"))
                .andExpect(jsonPath("$.racines[0].id").value(1));
    }

    @Test
    void profilRefuseSansAuthentification() throws Exception {
        mockMvc.perform(get("/api/moi")).andExpect(status().isForbidden());
    }

    @Test
    void lecturesPubliquesSansJeton() throws Exception {
        mockMvc.perform(get("/api/dioceses")).andExpect(status().isOk());
        mockMvc.perform(get("/api/clerges")).andExpect(status().isOk());
    }
}
