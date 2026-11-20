package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.config.OpenApiConfig;
import com.anglikana.backoffice.dto.DistrictDTO;
import com.anglikana.backoffice.dto.ParoisseDTO;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.District;
import com.anglikana.backoffice.entity.Region;
import com.anglikana.backoffice.mapper.DistrictMapper;
import com.anglikana.backoffice.mapper.GeometrieMapper;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.DistrictRepository;
import com.anglikana.backoffice.repository.RegionRepository;
import com.anglikana.backoffice.service.HierarchieService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.locationtech.jts.geom.Polygon;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/districts")
@Tag(name = "Districts", description = "CRUD des districts (niveau 3 de la hiérarchie, rattachés à une région) et paroisses d'un district")
public class DistrictController {

    private final DistrictRepository districtRepository;
    private final RegionRepository regionRepository;
    private final ClergeRepository clergeRepository;
    private final HierarchieService hierarchieService;

    public DistrictController(DistrictRepository districtRepository,
                               RegionRepository regionRepository,
                               ClergeRepository clergeRepository,
                               HierarchieService hierarchieService) {
        this.districtRepository = districtRepository;
        this.regionRepository = regionRepository;
        this.clergeRepository = clergeRepository;
        this.hierarchieService = hierarchieService;
    }

    @GetMapping
    @Operation(summary = "Lister les districts", description = "Tous les districts, sans pagination. Lecture publique.")
    @ApiResponse(responseCode = "200", description = "Liste des districts")
    public List<DistrictDTO> getAll() {
        return districtRepository.findAll()
                .stream().map(DistrictMapper::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un district", description = "Lecture publique. La carte QGIS est renvoyée en GeoJSON Polygon (null si absente).")
    @ApiResponse(responseCode = "200", description = "District trouvé")
    @ApiResponse(responseCode = "404", description = "District introuvable", content = @Content)
    public ResponseEntity<DistrictDTO> getById(@Parameter(description = "Identifiant du district") @PathVariable Integer id) {
        Optional<District> districtOptionnel = districtRepository.findById(id);

        if (districtOptionnel.isPresent()) {
            District district = districtOptionnel.get();
            return ResponseEntity.ok(DistrictMapper.toDTO(district));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/paroisses")
    @Operation(summary = "Lister les paroisses d'un district", description = "Paroisses rattachées au district, triées par nom. Lecture publique.")
    @ApiResponse(responseCode = "200", description = "Paroisses du district (liste vide si aucune)")
    @ApiResponse(responseCode = "404", description = "District introuvable", content = @Content)
    public ResponseEntity<List<ParoisseDTO>> getParoisses(@Parameter(description = "Identifiant du district") @PathVariable Integer id) {
        return hierarchieService.getParoissesDuDistrict(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un district",
            description = "Réservé au rôle ADMIN. regionId est obligatoire ; carteQgis, si fournie, doit être un GeoJSON Polygon d'un seul tenant en WGS 84.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "201", description = "District créé",
            content = @Content(schema = @Schema(implementation = DistrictDTO.class)))
    @ApiResponse(responseCode = "400", description = "Carte QGIS invalide (message explicatif)",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis ou jeton absent", content = @Content)
    public ResponseEntity<?> create(@RequestBody DistrictDTO dto) {
        District district = new District();
        try {
            appliquerDTO(district, dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        District saved = districtRepository.save(district);
        return ResponseEntity.status(201).body(DistrictMapper.toDTO(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un district",
            description = "Réservé au rôle ADMIN. Remplace tous les champs, y compris la carte QGIS (null pour la supprimer).")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "200", description = "District modifié",
            content = @Content(schema = @Schema(implementation = DistrictDTO.class)))
    @ApiResponse(responseCode = "400", description = "Carte QGIS invalide (message explicatif)",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis ou jeton absent", content = @Content)
    @ApiResponse(responseCode = "404", description = "District introuvable", content = @Content)
    public ResponseEntity<?> update(@Parameter(description = "Identifiant du district") @PathVariable Integer id, @RequestBody DistrictDTO dto) {
        Optional<District> districtOptionnel = districtRepository.findById(id);

        if (districtOptionnel.isPresent()) {
            District district = districtOptionnel.get();
            try {
                appliquerDTO(district, dto);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            District updated = districtRepository.save(district);
            return ResponseEntity.ok(DistrictMapper.toDTO(updated));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un district",
            description = "Réservé au rôle ADMIN. La base supprime en cascade ses paroisses, églises et ressources.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "204", description = "District supprimé", content = @Content)
    @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis ou jeton absent", content = @Content)
    @ApiResponse(responseCode = "404", description = "District introuvable", content = @Content)
    public ResponseEntity<?> delete(@Parameter(description = "Identifiant du district") @PathVariable Integer id) {
        if (!districtRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        districtRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void appliquerDTO(District district, DistrictDTO dto) {
        Polygon carteQgis = GeometrieMapper.depuisGeoJson(dto.getCarteQgis());
        district.setCarteQgis(carteQgis);
        district.setNom(dto.getNom());

        if (dto.getRegionId() != null) {
            Optional<Region> regionOptionnel = regionRepository.findById(dto.getRegionId());
            if (regionOptionnel.isPresent()) {
                district.setRegion(regionOptionnel.get());
            } else {
                district.setRegion(null);
            }
        } else {
            district.setRegion(null);
        }

        if (dto.getResponsableId() != null) {
            Optional<Clerge> clergeOptionnel = clergeRepository.findById(dto.getResponsableId());
            if (clergeOptionnel.isPresent()) {
                district.setResponsable(clergeOptionnel.get());
            } else {
                district.setResponsable(null);
            }
        } else {
            district.setResponsable(null);
        }
    }
}