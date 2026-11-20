package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.config.OpenApiConfig;
import com.anglikana.backoffice.dto.RessourceDTO;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.Eglise;
import com.anglikana.backoffice.entity.Ressource;
import com.anglikana.backoffice.entity.TypeRessource;
import com.anglikana.backoffice.mapper.RessourceMapper;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.EgliseRepository;
import com.anglikana.backoffice.repository.RessourceRepository;
import com.anglikana.backoffice.repository.TypeRessourceRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ressources")
@Tag(name = "Ressources", description = "CRUD des ressources : infrastructures (école, dispensaire, toby…) rattachées à une église")
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
    @Operation(summary = "Lister les ressources", description = "Toutes les ressources, sans pagination. Lecture publique.")
    @ApiResponse(responseCode = "200", description = "Liste des ressources")
    public List<RessourceDTO> getAll() {
        return ressourceRepository.findAll()
                .stream().map(RessourceMapper::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une ressource", description = "Lecture publique.")
    @ApiResponse(responseCode = "200", description = "Ressource trouvée")
    @ApiResponse(responseCode = "404", description = "Ressource introuvable", content = @Content)
    public ResponseEntity<RessourceDTO> getById(@Parameter(description = "Identifiant de la ressource") @PathVariable Integer id) {
        Optional<Ressource> ressourceOptionnel = ressourceRepository.findById(id);

        if (ressourceOptionnel.isPresent()) {
            Ressource ressource = ressourceOptionnel.get();
            return ResponseEntity.ok(RessourceMapper.toDTO(ressource));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une ressource",
            description = "Réservé au rôle ADMIN. typeId (voir /api/types-ressource) et egliseId sont obligatoires.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "201", description = "Ressource créée")
    @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis ou jeton absent", content = @Content)
    public ResponseEntity<RessourceDTO> create(@RequestBody RessourceDTO dto) {
        Ressource ressource = new Ressource();
        appliquerDTO(ressource, dto);

        Ressource saved = ressourceRepository.save(ressource);
        return ResponseEntity.status(201).body(RessourceMapper.toDTO(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier une ressource", description = "Réservé au rôle ADMIN. Remplace tous les champs.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "200", description = "Ressource modifiée")
    @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis ou jeton absent", content = @Content)
    @ApiResponse(responseCode = "404", description = "Ressource introuvable", content = @Content)
    public ResponseEntity<RessourceDTO> update(@Parameter(description = "Identifiant de la ressource") @PathVariable Integer id, @RequestBody RessourceDTO dto) {
        Optional<Ressource> ressourceOptionnel = ressourceRepository.findById(id);

        if (ressourceOptionnel.isPresent()) {
            Ressource ressource = ressourceOptionnel.get();
            appliquerDTO(ressource, dto);
            Ressource updated = ressourceRepository.save(ressource);
            return ResponseEntity.ok(RessourceMapper.toDTO(updated));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une ressource", description = "Réservé au rôle ADMIN.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "204", description = "Ressource supprimée", content = @Content)
    @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis ou jeton absent", content = @Content)
    @ApiResponse(responseCode = "404", description = "Ressource introuvable", content = @Content)
    public ResponseEntity<?> delete(@Parameter(description = "Identifiant de la ressource") @PathVariable Integer id) {
        if (!ressourceRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        ressourceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
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