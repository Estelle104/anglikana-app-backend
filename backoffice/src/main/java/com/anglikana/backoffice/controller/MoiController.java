package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.config.OpenApiConfig;
import com.anglikana.backoffice.dto.ProfilDTO;
import com.anglikana.backoffice.mapper.ClergeMapper;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.security.AutorisationService;
import com.anglikana.backoffice.security.LevelRequired;
import com.anglikana.backoffice.security.UtilisateurConnecte;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/moi")
@Tag(name = "Profil", description = "Utilisateur connecté : clergé, niveau et périmètre de responsabilité")
public class MoiController {

    private final ClergeRepository clergeRepository;
    private final AutorisationService autorisationService;

    public MoiController(ClergeRepository clergeRepository, AutorisationService autorisationService) {
        this.clergeRepository = clergeRepository;
        this.autorisationService = autorisationService;
    }

    @GetMapping
    @LevelRequired(0)
    @Operation(summary = "Consulter son profil",
            description = "Tout compte connecté et actif. Renvoie le clergé lié au compte, le nom et le niveau de son rôle "
                    + "(5 = Évêque ... 1 = Katekista, 0 sans rôle) et les racines de son périmètre : les entités dont il est "
                    + "responsable, sans celles déjà comprises dans une autre racine, triées du diocèse à l'église. "
                    + "Le responsable gère ces entités et toute leur descendance.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "200", description = "Profil de l'utilisateur connecté",
            content = @Content(schema = @Schema(implementation = ProfilDTO.class)))
    @ApiResponse(responseCode = "403", description = "Jeton absent ou invalide", content = @Content)
    public ProfilDTO getProfil(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        ProfilDTO profil = new ProfilDTO();
        profil.setNomUtilisateur(utilisateur.getUsername());
        profil.setRole(utilisateur.getRoleNom());
        profil.setNiveau(utilisateur.getNiveau());
        if (utilisateur.getClergeId() != null) {
            clergeRepository.findById(utilisateur.getClergeId()).map(ClergeMapper::toDTO).ifPresent(profil::setClerge);
        }
        profil.setRacines(autorisationService.racines(utilisateur));
        return profil;
    }
}
