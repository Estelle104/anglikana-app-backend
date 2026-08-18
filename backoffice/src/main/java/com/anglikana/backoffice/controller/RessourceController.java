package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.RessourceDTO;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.Eglise;
import com.anglikana.backoffice.entity.Ressource;
import com.anglikana.backoffice.entity.TypeRessource;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.EgliseRepository;
import com.anglikana.backoffice.repository.RessourceRepository;
import com.anglikana.backoffice.repository.TypeRessourceRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ressources")
public class RessourceController {

    private final RessourceRepository ressourceRepository;
    private final TypeRessourceRepository typeRessourceRepository;
    private final EgliseRepository egliseRepository;
    private final ClergeRepository clergeRepository;

    public RessourceController(RessourceRepository ressourceRepository,
                                TypeRessourceRepository typeRessourceRepository,
                                EgliseRepository egliseRepository,
                                ClergeRepository clergeRepository) {
        this.ressourceRepository = ressourceRepository;
        this.typeRessourceRepository = typeRessourceRepository;
        this.egliseRepository = egliseRepository;
        this.clergeRepository = clergeRepository;
    }

    @GetMapping
    public List<RessourceDTO> getAll() {
        return ressourceRepository.findAll()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RessourceDTO> getById(@PathVariable Integer id) {
        Optional<Ressource> ressourceOptionnel = ressourceRepository.findById(id);

        if (ressourceOptionnel.isPresent()) {
            Ressource ressource = ressourceOptionnel.get();
            return ResponseEntity.ok(toDTO(ressource));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RessourceDTO> create(@RequestBody RessourceDTO dto) {
        Ressource ressource = new Ressource();
        appliquerDTO(ressource, dto);

        Ressource saved = ressourceRepository.save(ressource);
        return ResponseEntity.status(201).body(toDTO(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RessourceDTO> update(@PathVariable Integer id, @RequestBody RessourceDTO dto) {
        Optional<Ressource> ressourceOptionnel = ressourceRepository.findById(id);

        if (ressourceOptionnel.isPresent()) {
            Ressource ressource = ressourceOptionnel.get();
            appliquerDTO(ressource, dto);
            Ressource updated = ressourceRepository.save(ressource);
            return ResponseEntity.ok(toDTO(updated));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        if (!ressourceRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        ressourceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private RessourceDTO toDTO(Ressource ressource) {
        RessourceDTO dto = new RessourceDTO();
        dto.setId(ressource.getId());
        dto.setNom(ressource.getNom());
        dto.setAdresse(ressource.getAdresse());
        dto.setTypeId(ressource.getType() != null ? ressource.getType().getId() : null);
        dto.setEgliseId(ressource.getEglise() != null ? ressource.getEglise().getId() : null);
        dto.setResponsableId(ressource.getResponsable() != null ? ressource.getResponsable().getId() : null);
        return dto;
    }

    private void appliquerDTO(Ressource ressource, RessourceDTO dto) {
        ressource.setNom(dto.getNom());
        ressource.setAdresse(dto.getAdresse());

        if (dto.getTypeId() != null) {
            Optional<TypeRessource> typeOptionnel = typeRessourceRepository.findById(dto.getTypeId());
            if (typeOptionnel.isPresent()) {
                ressource.setType(typeOptionnel.get());
            } else {
                ressource.setType(null);
            }
        } else {
            ressource.setType(null);
        }

        if (dto.getEgliseId() != null) {
            Optional<Eglise> egliseOptionnel = egliseRepository.findById(dto.getEgliseId());
            if (egliseOptionnel.isPresent()) {
                ressource.setEglise(egliseOptionnel.get());
            } else {
                ressource.setEglise(null);
            }
        } else {
            ressource.setEglise(null);
        }

        if (dto.getResponsableId() != null) {
            Optional<Clerge> clergeOptionnel = clergeRepository.findById(dto.getResponsableId());
            if (clergeOptionnel.isPresent()) {
                ressource.setResponsable(clergeOptionnel.get());
            } else {
                ressource.setResponsable(null);
            }
        } else {
            ressource.setResponsable(null);
        }
    }
}