package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.ImportLimitesRequest;
import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.service.GeographieService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/geographie")
public class GeographieController {

    private static final int LIMITE_RECHERCHE_MAX = 200;
    private static final int LONGUEUR_TERME_MAX = 150;

    private final GeographieService geographieService;

    public GeographieController(GeographieService geographieService) {
        this.geographieService = geographieService;
    }

    @GetMapping("/recherche")
    public ResponseEntity<?> rechercher(@RequestParam(required = false) String q,
                                        @RequestParam(required = false) String niveau,
                                        @RequestParam(defaultValue = "50") int limite) {
        NiveauHierarchique niveauFiltre = null;
        if (niveau != null && !niveau.isBlank()) {
            Optional<NiveauHierarchique> niveauOptionnel = niveauGeographique(niveau);
            if (niveauOptionnel.isEmpty()) {
                return niveauGeographiqueInvalide(niveau);
            }
            niveauFiltre = niveauOptionnel.get();
        }
        if (limite < 1 || limite > LIMITE_RECHERCHE_MAX) {
            return ResponseEntity.badRequest().body("Le paramètre limite doit être compris entre 1 et " + LIMITE_RECHERCHE_MAX);
        }
        if (q != null && q.length() > LONGUEUR_TERME_MAX) {
            return ResponseEntity.badRequest().body("Le terme recherché ne doit pas dépasser " + LONGUEUR_TERME_MAX + " caractères");
        }

        return ResponseEntity.ok(geographieService.rechercher(niveauFiltre, q, limite));
    }

    @GetMapping("/{niveau}/limites")
    public ResponseEntity<?> getLimites(@PathVariable String niveau) {
        Optional<NiveauHierarchique> niveauOptionnel = niveauGeographique(niveau);
        if (niveauOptionnel.isEmpty()) {
            return niveauGeographiqueInvalide(niveau);
        }
        return ResponseEntity.ok(geographieService.getLimites(niveauOptionnel.get()));
    }

    @PutMapping("/{niveau}/limites")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importerLimites(@PathVariable String niveau, @RequestBody ImportLimitesRequest request) {
        Optional<NiveauHierarchique> niveauOptionnel = niveauGeographique(niveau);
        if (niveauOptionnel.isEmpty()) {
            return niveauGeographiqueInvalide(niveau);
        }

        try {
            return ResponseEntity.ok(geographieService.importerLimites(niveauOptionnel.get(), request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{niveau}/{id}")
    public ResponseEntity<?> getZone(@PathVariable String niveau, @PathVariable Integer id) {
        Optional<NiveauHierarchique> niveauOptionnel = niveauGeographique(niveau);
        if (niveauOptionnel.isEmpty()) {
            return niveauGeographiqueInvalide(niveau);
        }

        return geographieService.getZone(niveauOptionnel.get(), id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{niveau}/{id}/eglises")
    public ResponseEntity<?> getEglisesDeLaZone(@PathVariable String niveau, @PathVariable Integer id) {
        Optional<NiveauHierarchique> niveauOptionnel = niveauGeographique(niveau);
        if (niveauOptionnel.isEmpty()) {
            return niveauGeographiqueInvalide(niveau);
        }

        return geographieService.getEglisesDeLaZone(niveauOptionnel.get(), id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private Optional<NiveauHierarchique> niveauGeographique(String code) {
        return NiveauHierarchique.depuisCode(code).filter(NiveauHierarchique::estGeographique);
    }

    private ResponseEntity<?> niveauGeographiqueInvalide(String code) {
        return ResponseEntity.badRequest().body("Niveau géographique inconnu : " + code + " (attendu : diocese, region ou district)");
    }
}
