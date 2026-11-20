package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.ApercuHierarchieDTO;
import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.dto.StatistiquesEntiteDTO;
import com.anglikana.backoffice.dto.TableauBordDTO;
import com.anglikana.backoffice.service.TableauBordService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/tableau-bord")
@Tag(name = "Tableau de bord", description = "Aperçu de la hiérarchie, tableau de bord et statistiques d'une entité (comptages calculés par PostgreSQL)")
public class TableauBordController {

    private static final String DESCRIPTION_NIVEAU = "Niveau hiérarchique : diocese, region, district, paroisse ou eglise";

    private final TableauBordService tableauBordService;

    public TableauBordController(TableauBordService tableauBordService) {
        this.tableauBordService = tableauBordService;
    }

    @GetMapping
    @Operation(summary = "Aperçu de la hiérarchie",
            description = "Totaux de toute la hiérarchie (le clergé est compté une seule fois) et statistiques de chaque diocèse. Lecture publique.")
    @ApiResponse(responseCode = "200", description = "Aperçu de la hiérarchie")
    public ApercuHierarchieDTO getApercu() {
        return tableauBordService.getApercu();
    }

    @GetMapping("/{niveau}/{id}")
    @Operation(summary = "Tableau de bord d'une entité",
            description = "Ancêtres, statistiques et responsable de l'entité, entités enfants (triées par nom) et clergé affecté dans son périmètre. "
                    + "vuesEnfants propose au plus les deux niveaux inférieurs ; pour une église, sa fiche et ses ressources détaillées. Lecture publique.")
    @ApiResponse(responseCode = "200", description = "Tableau de bord de l'entité",
            content = @Content(schema = @Schema(implementation = TableauBordDTO.class)))
    @ApiResponse(responseCode = "400", description = "Niveau hiérarchique inconnu (niveau ou niveauEnfants)",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "404", description = "Entité introuvable", content = @Content)
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
    @Operation(summary = "Statistiques d'une entité",
            description = "Comptages des niveaux strictement inférieurs à l'entité, de ses ressources et du clergé responsable "
                    + "dans son périmètre. Lecture publique.")
    @ApiResponse(responseCode = "200", description = "Statistiques de l'entité",
            content = @Content(schema = @Schema(implementation = StatistiquesEntiteDTO.class)))
    @ApiResponse(responseCode = "400", description = "Niveau hiérarchique inconnu",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "404", description = "Entité introuvable", content = @Content)
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
