package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.config.OpenApiConfig;
import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.security.AutorisationService;
import com.anglikana.backoffice.security.CascadeAuthorised;
import com.anglikana.backoffice.security.EntiteProtegee;
import com.anglikana.backoffice.security.NeedAuthorisation;
import com.anglikana.backoffice.dto.RegionDTO;
import com.anglikana.backoffice.dto.DistrictDTO;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.Diocese;
import com.anglikana.backoffice.entity.Region;
import com.anglikana.backoffice.mapper.RegionMapper;
import com.anglikana.backoffice.mapper.GeometrieMapper;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.DioceseRepository;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/regions")
@Tag(name = "Régions", description = "CRUD des régions (niveau 2 de la hiérarchie, rattachées à un diocèse) et districts d'une région")
public class RegionController {

    private final RegionRepository regionRepository;
    private final DioceseRepository dioceseRepository;
    private final ClergeRepository clergeRepository;
    private final HierarchieService hierarchieService;
    private final AutorisationService autorisationService;

    public RegionController(RegionRepository regionRepository,DioceseRepository dioceseRepository,ClergeRepository clergeRepository,
                            HierarchieService hierarchieService,
                            AutorisationService autorisationService) {
        this.autorisationService = autorisationService;
        this.regionRepository = regionRepository;
        this.dioceseRepository = dioceseRepository;
        this.clergeRepository = clergeRepository;
        this.hierarchieService = hierarchieService;
    }

    @GetMapping
    @Operation(summary = "Lister les régions", description = "Toutes les régions, sans pagination. Lecture publique, éléments supprimés (etat = -1) exclus.")
    @ApiResponse(responseCode = "200", description = "Liste des régions")
    public List<RegionDTO> getAll() {
        return regionRepository.findAll()
                .stream().map(RegionMapper::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une région", description = "Lecture publique, éléments supprimés (etat = -1) exclus. La carte QGIS est renvoyée en GeoJSON Polygon (null si absente).")
    @ApiResponse(responseCode = "200", description = "Région trouvée")
    @ApiResponse(responseCode = "404", description = "Région introuvable", content = @Content)
    public ResponseEntity<RegionDTO> getById(@Parameter(description = "Identifiant de la région") @PathVariable Integer id) {
        Optional<Region> regionOptionnel = regionRepository.findById(id);

        if (regionOptionnel.isPresent()) {
            Region region = regionOptionnel.get();
            return ResponseEntity.ok(RegionMapper.toDTO(region));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/districts")
    @Operation(summary = "Lister les districts d'une région", description = "Districts rattachés à la région, triés par nom. Lecture publique, éléments supprimés (etat = -1) exclus.")
    @ApiResponse(responseCode = "200", description = "Districts de la région (liste vide si aucun)")
    @ApiResponse(responseCode = "404", description = "Région introuvable", content = @Content)
    public ResponseEntity<List<DistrictDTO>> getDistricts(@Parameter(description = "Identifiant de la région") @PathVariable Integer id) {
        return hierarchieService.getDistrictsDeLaRegion(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @NeedAuthorisation(entity = EntiteProtegee.DIOCESE, id = "#dto.dioceseId")
    @CascadeAuthorised
    @Operation(summary = "Créer une région",
            description = "Réservé au responsable du diocèse dioceseId. dioceseId est obligatoire ; carteQgis, si fournie, doit être un GeoJSON Polygon d'un seul tenant en WGS 84.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "201", description = "Région créée",
            content = @Content(schema = @Schema(implementation = RegionDTO.class)))
    @ApiResponse(responseCode = "400", description = "Carte QGIS invalide (message explicatif)",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "403", description = "Jeton absent, dioceseId absent, ou hors du périmètre du diocèse dioceseId", content = @Content)
    @ApiResponse(responseCode = "404", description = "Diocèse dioceseId introuvable ou supprimé", content = @Content)
    public ResponseEntity<?> create(@RequestBody RegionDTO dto) {
        Region region = new Region();
        try {
            appliquerDTO(region, dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        Region saved = regionRepository.save(region);
        return ResponseEntity.status(201).body(RegionMapper.toDTO(saved));
    }

    @PutMapping("/{id}")
    @NeedAuthorisation(entity = EntiteProtegee.REGION, id = "#id")
    @CascadeAuthorised
    @Operation(summary = "Modifier une région",
            description = "Réservé au responsable de la région ou de l'un de ses ancêtres. Changer dioceseId exige aussi d'être responsable du nouveau diocèse ou d'un de ses ancêtres. Remplace tous les champs, y compris la carte QGIS (null pour la supprimer).")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "200", description = "Région modifiée",
            content = @Content(schema = @Schema(implementation = RegionDTO.class)))
    @ApiResponse(responseCode = "400", description = "Carte QGIS invalide (message explicatif)",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "403", description = "Jeton absent, ou hors du périmètre de la région ou du nouveau diocèse", content = @Content)
    @ApiResponse(responseCode = "404", description = "Région ou nouveau diocèse introuvable", content = @Content)
    public ResponseEntity<?> update(@Parameter(description = "Identifiant de la région") @PathVariable Integer id, @RequestBody RegionDTO dto) {
        Optional<Region> regionOptionnel = regionRepository.findById(id);

        if (regionOptionnel.isPresent()) {
            Region region = regionOptionnel.get();
            Integer parentActuel = region.getDiocese() != null ? region.getDiocese().getId() : null;
            if (!Objects.equals(dto.getDioceseId(), parentActuel)) {
                // Rattachement à un autre parent : il doit lui aussi être dans le périmètre.
                autorisationService.verifier(EntiteProtegee.DIOCESE, dto.getDioceseId(), true);
            }
            try {
                appliquerDTO(region, dto);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            Region updated = regionRepository.save(region);
            return ResponseEntity.ok(RegionMapper.toDTO(updated));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @NeedAuthorisation(entity = EntiteProtegee.REGION, id = "#id")
    @CascadeAuthorised
    @Operation(summary = "Supprimer une région",
            description = "Réservé au responsable de la région ou de l'un de ses ancêtres. Suppression logique (etat = -1), refusée tant qu'un district actif y est rattaché.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "204", description = "Région supprimée", content = @Content)
    @ApiResponse(responseCode = "403", description = "Jeton absent ou hors du périmètre de la région", content = @Content)
    @ApiResponse(responseCode = "404", description = "Région introuvable", content = @Content)
    @ApiResponse(responseCode = "409", description = "Au moins une fille directe est encore active (message explicatif)",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    public ResponseEntity<?> delete(@Parameter(description = "Identifiant de la région") @PathVariable Integer id) {
        if (!regionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        Optional<String> refus = hierarchieService.refusSuppression(NiveauHierarchique.REGION, id);
        if (refus.isPresent()) {
            return ResponseEntity.status(409).body(refus.get());
        }
        regionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void appliquerDTO(Region region, RegionDTO dto) {
        Polygon carteQgis = GeometrieMapper.depuisGeoJson(dto.getCarteQgis());
        region.setCarteQgis(carteQgis);
        region.setNom(dto.getNom());

        if (dto.getDioceseId() != null) {
            Optional<Diocese> dioceseOptionnel = dioceseRepository.findById(dto.getDioceseId());
            if (dioceseOptionnel.isPresent()) {
                region.setDiocese(dioceseOptionnel.get());
            } else {
                region.setDiocese(null);
            }
        } else {
            region.setDiocese(null);
        }

        if (dto.getResponsableId() != null) {
            Optional<Clerge> clergeOptionnel = clergeRepository.findById(dto.getResponsableId());
            if (clergeOptionnel.isPresent()) {
                region.setResponsable(clergeOptionnel.get());
            } else {
                region.setResponsable(null);
            }
        } else {
            region.setResponsable(null);
        }
    }
}