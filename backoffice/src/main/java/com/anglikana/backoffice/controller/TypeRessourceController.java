package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.TypeRessourceDTO;
import com.anglikana.backoffice.service.HierarchieService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/types-ressource")
@Tag(name = "Types de ressource", description = "Référentiel des types de ressource, en lecture seule")
public class TypeRessourceController {

    private final HierarchieService hierarchieService;

    public TypeRessourceController(HierarchieService hierarchieService) {
        this.hierarchieService = hierarchieService;
    }

    @GetMapping
    @Operation(summary = "Lister les types de ressource", description = "Types triés par nom. Lecture publique.")
    @ApiResponse(responseCode = "200", description = "Liste des types de ressource")
    public List<TypeRessourceDTO> getAll() {
        return hierarchieService.getTypesRessource();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un type de ressource", description = "Lecture publique.")
    @ApiResponse(responseCode = "200", description = "Type de ressource trouvé")
    @ApiResponse(responseCode = "404", description = "Type de ressource introuvable", content = @Content)
    public ResponseEntity<TypeRessourceDTO> getById(@Parameter(description = "Identifiant du type de ressource") @PathVariable Integer id) {
        return hierarchieService.getTypeRessource(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
