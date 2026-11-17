package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.TypeRessourceDTO;
import com.anglikana.backoffice.service.HierarchieService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/types-ressource")
public class TypeRessourceController {

    private final HierarchieService hierarchieService;

    public TypeRessourceController(HierarchieService hierarchieService) {
        this.hierarchieService = hierarchieService;
    }

    @GetMapping
    public List<TypeRessourceDTO> getAll() {
        return hierarchieService.getTypesRessource();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TypeRessourceDTO> getById(@PathVariable Integer id) {
        return hierarchieService.getTypeRessource(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
