package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.config.OpenApiConfig;
import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.security.EntiteProtegee;
import com.anglikana.backoffice.security.LevelRequired;
import com.anglikana.backoffice.security.NeedAuthorisation;
import com.anglikana.backoffice.dto.DioceseDTO;
import com.anglikana.backoffice.dto.DistrictDTO;
import com.anglikana.backoffice.dto.RegionDTO;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.Diocese;
import com.anglikana.backoffice.mapper.GeometrieMapper;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.DioceseRepository;
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
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dioceses")
@Tag(name = "Diocèses", description = "CRUD des diocèses (niveau 1 de la hiérarchie) et régions ou districts d'un diocèse")
public class DioceseController {

    private final DioceseRepository dioceseRepository;
    private final ClergeRepository clergeRepository;
    private final HierarchieService hierarchieService;

    public DioceseController(DioceseRepository dioceseRepository,
                              ClergeRepository clergeRepository,
                              HierarchieService hierarchieService) {
        this.dioceseRepository = dioceseRepository;
        this.clergeRepository = clergeRepository;
        this.hierarchieService = hierarchieService;
    }

    @GetMapping
    @Operation(summary = "Lister les diocèses", description = "Tous les diocèses, sans pagination. Lecture publique, éléments supprimés (etat = -1) exclus.")
    @ApiResponse(responseCode = "200", description = "Liste des diocèses")
    public List<DioceseDTO> getAll() {
        return dioceseRepository.findAll()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un diocèse", description = "Lecture publique, éléments supprimés (etat = -1) exclus. La carte QGIS est renvoyée en GeoJSON Polygon (null si absente).")
    @ApiResponse(responseCode = "200", description = "Diocèse trouvé")
    @ApiResponse(responseCode = "404", description = "Diocèse introuvable", content = @Content)
    public ResponseEntity<DioceseDTO> getById(@Parameter(description = "Identifiant du diocèse") @PathVariable Integer id) {
        Optional<Diocese> dioceseOptionnel = dioceseRepository.findById(id);

        if (dioceseOptionnel.isPresent()) {
            Diocese diocese = dioceseOptionnel.get(); 
            return ResponseEntity.ok(toDTO(diocese));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/regions")
    @Operation(summary = "Lister les régions d'un diocèse", description = "Régions rattachées au diocèse, triées par nom. Lecture publique, éléments supprimés (etat = -1) exclus.")
    @ApiResponse(responseCode = "200", description = "Régions du diocèse (liste vide si aucune)")
    @ApiResponse(responseCode = "404", description = "Diocèse introuvable", content = @Content)
    public ResponseEntity<List<RegionDTO>> getRegions(@Parameter(description = "Identifiant du diocèse") @PathVariable Integer id) {
        return hierarchieService.getRegionsDuDiocese(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/districts")
    @Operation(summary = "Lister les districts d'un diocèse",
            description = "Districts de toutes les régions du diocèse, triés par nom. Lecture publique, éléments supprimés (etat = -1) exclus.")
    @ApiResponse(responseCode = "200", description = "Districts du diocèse (liste vide si aucun)")
    @ApiResponse(responseCode = "404", description = "Diocèse introuvable", content = @Content)
    public ResponseEntity<List<DistrictDTO>> getDistricts(@Parameter(description = "Identifiant du diocèse") @PathVariable Integer id) {
        return hierarchieService.getDistrictsDuDiocese(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @LevelRequired(5)
    @Operation(summary = "Créer un diocèse",
            description = "Réservé au niveau 5 (Évêque). carteQgis, si fournie, doit être un GeoJSON Polygon d'un seul tenant en WGS 84.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "201", description = "Diocèse créé",
            content = @Content(schema = @Schema(implementation = DioceseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Carte QGIS invalide (message explicatif)",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "403", description = "Jeton absent ou niveau 5 requis", content = @Content)
    public ResponseEntity<?> create(@RequestBody DioceseDTO dto) {
        Diocese diocese = new Diocese();
        try {
            appliquerDTO(diocese, dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        Diocese saved = dioceseRepository.save(diocese);
        return ResponseEntity.status(201).body(toDTO(saved));
    }

    @PutMapping("/{id}")
    @NeedAuthorisation(entity = EntiteProtegee.DIOCESE, id = "#id")
    @Operation(summary = "Modifier un diocèse",
            description = "Réservé au responsable du diocèse. Remplace tous les champs, y compris la carte QGIS (null pour la supprimer).")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "200", description = "Diocèse modifié",
            content = @Content(schema = @Schema(implementation = DioceseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Carte QGIS invalide (message explicatif)",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "403", description = "Jeton absent ou hors du périmètre du diocèse", content = @Content)
    @ApiResponse(responseCode = "404", description = "Diocèse introuvable", content = @Content)
    public ResponseEntity<?> update(@Parameter(description = "Identifiant du diocèse") @PathVariable Integer id, @RequestBody DioceseDTO dto) {
        Optional<Diocese> dioceseOptionnel = dioceseRepository.findById(id);

        if (dioceseOptionnel.isPresent()) {
            Diocese diocese = dioceseOptionnel.get(); 
            
            try {
                appliquerDTO(diocese, dto);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            Diocese updated = dioceseRepository.save(diocese);
            return ResponseEntity.ok(toDTO(updated));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @NeedAuthorisation(entity = EntiteProtegee.DIOCESE, id = "#id")
    @Operation(summary = "Supprimer un diocèse",
            description = "Réservé au responsable du diocèse. Suppression logique (etat = -1), refusée tant qu'une région active y est rattachée.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "204", description = "Diocèse supprimé", content = @Content)
    @ApiResponse(responseCode = "403", description = "Jeton absent ou hors du périmètre du diocèse", content = @Content)
    @ApiResponse(responseCode = "404", description = "Diocèse introuvable", content = @Content)
    @ApiResponse(responseCode = "409", description = "Au moins une fille directe est encore active (message explicatif)",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    public ResponseEntity<?> delete(@Parameter(description = "Identifiant du diocèse") @PathVariable Integer id) {
        if (!dioceseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        Optional<String> refus = hierarchieService.refusSuppression(NiveauHierarchique.DIOCESE, id);
        if (refus.isPresent()) {
            return ResponseEntity.status(409).body(refus.get());
        }
        dioceseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private DioceseDTO toDTO(Diocese diocese) {
        DioceseDTO dto = new DioceseDTO();
        dto.setId(diocese.getId());
        dto.setNom(diocese.getNom());
        dto.setCarteQgis(GeometrieMapper.versGeoJson(diocese.getCarteQgis()));
        dto.setPresentation(diocese.getPresentation());
        dto.setCoordonnees(diocese.getCoordonnees());
        dto.setResponsableId(diocese.getResponsable() != null ? diocese.getResponsable().getId() : null);
        
        return dto;
    }

    private void appliquerDTO(Diocese diocese, DioceseDTO dto) {
        Polygon carteQgis = GeometrieMapper.depuisGeoJson(dto.getCarteQgis());
        diocese.setCarteQgis(carteQgis);
        diocese.setNom(dto.getNom());
        diocese.setPresentation(dto.getPresentation());
        diocese.setCoordonnees(dto.getCoordonnees());

        if (dto.getResponsableId() != null) {
            Optional<Clerge> clergeOptionnel = clergeRepository.findById(dto.getResponsableId());
            
            if (clergeOptionnel.isPresent()) {
                diocese.setResponsable(clergeOptionnel.get());
            } else {
                diocese.setResponsable(null);
            }
        } else {
            diocese.setResponsable(null);
        }
    }
}
