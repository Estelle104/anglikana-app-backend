package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.config.OpenApiConfig;
import com.anglikana.backoffice.dto.ApercuHierarchieDTO;
import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.dto.StatistiquesEntiteDTO;
import com.anglikana.backoffice.dto.TableauBordDTO;
import com.anglikana.backoffice.security.AutorisationService;
import com.anglikana.backoffice.security.CascadeAuthorised;
import com.anglikana.backoffice.security.LevelRequired;
import com.anglikana.backoffice.security.NeedAuthorisation;
import com.anglikana.backoffice.security.UtilisateurConnecte;
import com.anglikana.backoffice.service.TableauBordService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/tableau-bord")
@Tag(name = "Tableau de bord", description = "Aperçu de la hiérarchie, tableau de bord et statistiques d'une entité (comptages calculés par PostgreSQL). "
        + "Back-office : jeton requis, limité au périmètre du responsable")
public class TableauBordController {

    private static final String DESCRIPTION_NIVEAU = "Niveau hiérarchique : diocese, region, district, paroisse ou eglise";

    private final TableauBordService tableauBordService;
    private final AutorisationService autorisationService;

    public TableauBordController(TableauBordService tableauBordService, AutorisationService autorisationService) {
        this.tableauBordService = tableauBordService;
        this.autorisationService = autorisationService;
    }

    @GetMapping
    @LevelRequired(5)
    @Operation(summary = "Aperçu des diocèses du responsable",
            description = "Réservé au niveau 5 (Évêque). Totaux des diocèses dont le clergé du compte est responsable (le clergé "
                    + "est compté une seule fois) et statistiques de chacun. Éléments supprimés (etat = -1) exclus.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "200", description = "Aperçu des diocèses du responsable (totaux à 0 s'il n'en a aucun)")
    @ApiResponse(responseCode = "403", description = "Jeton absent ou niveau 5 requis", content = @Content)
    public ApercuHierarchieDTO getApercu(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return tableauBordService.getApercu(autorisationService.dioceses(utilisateur));
    }

    @GetMapping("/{niveau}/{id}")
    @NeedAuthorisation(entityExpression = "#niveau", id = "#id")
    @CascadeAuthorised
    @Operation(summary = "Tableau de bord d'une entité",
            description = "Ancêtres, statistiques et responsable de l'entité, entités enfants (triées par nom) et clergé affecté dans son périmètre. "
                    + "vuesEnfants propose au plus les deux niveaux inférieurs ; pour une église, sa fiche et ses ressources détaillées. "
                    + "Réservé au responsable de l'entité ou de l'un de ses ancêtres. Éléments supprimés (etat = -1) exclus.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "200", description = "Tableau de bord de l'entité",
            content = @Content(schema = @Schema(implementation = TableauBordDTO.class)))
    @ApiResponse(responseCode = "400", description = "niveau = ressource (non géré par le tableau de bord), ou niveauEnfants inconnu",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "403", description = "Jeton absent, niveau inconnu, ou entité hors du périmètre", content = @Content)
    @ApiResponse(responseCode = "404", description = "Entité introuvable ou supprimée", content = @Content)
    public ResponseEntity<?> getTableauBord(@Parameter(description = DESCRIPTION_NIVEAU) @PathVariable String niveau,
                                            @Parameter(description = "Identifiant de l'entité") @PathVariable Integer id,
                                            @Parameter(description = "Niveau des enfants à lister, parmi vuesEnfants ; "
                                                    + "absent ou non proposé : niveau directement inférieur")
                                            @RequestParam(required = false) String niveauEnfants) {
        Optional<NiveauHierarchique> niveauOptionnel = NiveauHierarchique.depuisCode(niveau);
        if (niveauOptionnel.isEmpty()) {
            return ResponseEntity.badRequest().body("Niveau hiérarchique inconnu : " + niveau);
        }

        NiveauHierarchique niveauEnfantsDemande = null;
        if (niveauEnfants != null && !niveauEnfants.isBlank()) {
            Optional<NiveauHierarchique> niveauEnfantsOptionnel = NiveauHierarchique.depuisCode(niveauEnfants);
            if (niveauEnfantsOptionnel.isEmpty()) {
                return ResponseEntity.badRequest().body("Niveau hiérarchique inconnu : " + niveauEnfants);
            }
            niveauEnfantsDemande = niveauEnfantsOptionnel.get();
        }

        return tableauBordService.getTableauBord(niveauOptionnel.get(), id, niveauEnfantsDemande)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{niveau}/{id}/statistiques")
    @NeedAuthorisation(entityExpression = "#niveau", id = "#id")
    @CascadeAuthorised
    @Operation(summary = "Statistiques d'une entité",
            description = "Comptages des niveaux strictement inférieurs à l'entité, de ses ressources et du clergé responsable "
                    + "dans son périmètre. Réservé au responsable de l'entité ou de l'un de ses ancêtres. Éléments supprimés (etat = -1) exclus.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "200", description = "Statistiques de l'entité",
            content = @Content(schema = @Schema(implementation = StatistiquesEntiteDTO.class)))
    @ApiResponse(responseCode = "400", description = "niveau = ressource (non géré par le tableau de bord)",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "403", description = "Jeton absent, niveau inconnu, ou entité hors du périmètre", content = @Content)
    @ApiResponse(responseCode = "404", description = "Entité introuvable ou supprimée", content = @Content)
    public ResponseEntity<?> getStatistiques(@Parameter(description = DESCRIPTION_NIVEAU) @PathVariable String niveau,
                                             @Parameter(description = "Identifiant de l'entité") @PathVariable Integer id) {
        Optional<NiveauHierarchique> niveauOptionnel = NiveauHierarchique.depuisCode(niveau);
        if (niveauOptionnel.isEmpty()) {
            return ResponseEntity.badRequest().body("Niveau hiérarchique inconnu : " + niveau);
        }

        return tableauBordService.getStatistiques(niveauOptionnel.get(), id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
