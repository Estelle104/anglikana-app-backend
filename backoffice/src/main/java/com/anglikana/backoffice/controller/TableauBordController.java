package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.ApercuHierarchieDTO;
import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.service.TableauBordService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/tableau-bord")
public class TableauBordController {

    private final TableauBordService tableauBordService;

    public TableauBordController(TableauBordService tableauBordService) {
        this.tableauBordService = tableauBordService;
    }

    @GetMapping
    public ApercuHierarchieDTO getApercu() {
        return tableauBordService.getApercu();
    }

    @GetMapping("/{niveau}/{id}")
    public ResponseEntity<?> getTableauBord(@PathVariable String niveau,
                                            @PathVariable Integer id,
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
    public ResponseEntity<?> getStatistiques(@PathVariable String niveau, @PathVariable Integer id) {
        Optional<NiveauHierarchique> niveauOptionnel = NiveauHierarchique.depuisCode(niveau);
        if (niveauOptionnel.isEmpty()) {
            return ResponseEntity.badRequest().body("Niveau hiérarchique inconnu : " + niveau);
        }

        return tableauBordService.getStatistiques(niveauOptionnel.get(), id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
