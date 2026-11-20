package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.config.OpenApiConfig;
import com.anglikana.backoffice.security.LevelRequired;
import com.anglikana.backoffice.service.HierarchieService;
import com.anglikana.backoffice.dto.ClergeDTO;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.Role;
import com.anglikana.backoffice.mapper.ClergeMapper;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.RoleRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clerges")
@Tag(name = "Clergé", description = "CRUD des membres du clergé ; leur rôle (roleId) détermine les droits du compte utilisateur qui leur est lié")
public class ClergeController {

    private final ClergeRepository clergeRepository;
    private final RoleRepository roleRepository;
    private final HierarchieService hierarchieService;

    public ClergeController(ClergeRepository clergeRepository,
                             RoleRepository roleRepository,
                             HierarchieService hierarchieService) {
        this.clergeRepository = clergeRepository;
        this.roleRepository = roleRepository;
        this.hierarchieService = hierarchieService;
    }

    @GetMapping
    @Operation(summary = "Lister le clergé", description = "Tous les membres du clergé, sans pagination. Lecture publique, éléments supprimés (etat = -1) exclus.")
    @ApiResponse(responseCode = "200", description = "Liste des membres du clergé")
    public List<ClergeDTO> getAll() {
        return clergeRepository.findAll()
                .stream().map(ClergeMapper::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un membre du clergé", description = "Lecture publique, éléments supprimés (etat = -1) exclus.")
    @ApiResponse(responseCode = "200", description = "Membre du clergé trouvé")
    @ApiResponse(responseCode = "404", description = "Membre du clergé introuvable", content = @Content)
    public ResponseEntity<ClergeDTO> getById(@Parameter(description = "Identifiant du membre du clergé") @PathVariable Integer id) {
        Optional<Clerge> clergeOptionnel = clergeRepository.findById(id);

        if (clergeOptionnel.isPresent()) {
            Clerge clerge = clergeOptionnel.get();
            return ResponseEntity.ok(ClergeMapper.toDTO(clerge));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @LevelRequired(5)
    @Operation(summary = "Créer un membre du clergé",
            description = "Réservé au niveau 5 (Évêque). nom est obligatoire ; un roleId inconnu est ignoré (rôle vide).")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "201", description = "Membre du clergé créé")
    @ApiResponse(responseCode = "403", description = "Jeton absent ou niveau 5 requis", content = @Content)
    public ResponseEntity<ClergeDTO> create(@RequestBody ClergeDTO dto) {
        Clerge clerge = new Clerge();
        appliquerDTO(clerge, dto);

        Clerge saved = clergeRepository.save(clerge);
        return ResponseEntity.status(201).body(ClergeMapper.toDTO(saved));
    }

    @PutMapping("/{id}")
    @LevelRequired(5)
    @Operation(summary = "Modifier un membre du clergé",
            description = "Réservé au niveau 5 (Évêque). Remplace tous les champs ; roleId fixe le niveau des droits du clergé.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "200", description = "Membre du clergé modifié")
    @ApiResponse(responseCode = "403", description = "Jeton absent ou niveau 5 requis", content = @Content)
    @ApiResponse(responseCode = "404", description = "Membre du clergé introuvable", content = @Content)
    public ResponseEntity<ClergeDTO> update(@Parameter(description = "Identifiant du membre du clergé") @PathVariable Integer id, @RequestBody ClergeDTO dto) {
        Optional<Clerge> clergeOptionnel = clergeRepository.findById(id);

        if (clergeOptionnel.isPresent()) {
            Clerge clerge = clergeOptionnel.get();
            appliquerDTO(clerge, dto);
            Clerge updated = clergeRepository.save(clerge);
            return ResponseEntity.ok(ClergeMapper.toDTO(updated));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @LevelRequired(5)
    @Operation(summary = "Supprimer un membre du clergé",
            description = "Réservé au niveau 5 (Évêque). Suppression logique (etat = -1), refusée tant qu'il est responsable "
                    + "d'une entité ou d'une ressource active. Son compte ne peut plus se connecter et perd tous ses droits.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "204", description = "Membre du clergé supprimé", content = @Content)
    @ApiResponse(responseCode = "403", description = "Jeton absent ou niveau 5 requis", content = @Content)
    @ApiResponse(responseCode = "404", description = "Membre du clergé introuvable", content = @Content)
    @ApiResponse(responseCode = "409", description = "Encore responsable d'une entité ou d'une ressource active",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    public ResponseEntity<?> delete(@Parameter(description = "Identifiant du membre du clergé") @PathVariable Integer id) {
        if (!clergeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (hierarchieService.estResponsableActif(id)) {
            return ResponseEntity.status(409)
                    .body("Suppression impossible : ce membre du clergé est encore responsable d'une entité ou d'une ressource active.");
        }
        clergeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void appliquerDTO(Clerge clerge, ClergeDTO dto) {
        clerge.setNom(dto.getNom());
        clerge.setPrenom(dto.getPrenom());
        clerge.setDateNaissance(dto.getDateNaissance());
        clerge.setHierarchie(dto.getHierarchie());
        clerge.setTelephone(dto.getTelephone());
        clerge.setEmail(dto.getEmail());

        if (dto.getRoleId() != null) {
            Optional<Role> roleOptionnel = roleRepository.findById(dto.getRoleId());
            if (roleOptionnel.isPresent()) {
                clerge.setRole(roleOptionnel.get());
            } else {
                clerge.setRole(null);
            }
        } else {
            clerge.setRole(null);
        }
    }
}