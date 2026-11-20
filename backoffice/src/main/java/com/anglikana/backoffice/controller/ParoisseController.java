package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.config.OpenApiConfig;
import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.security.AutorisationService;
import com.anglikana.backoffice.security.CascadeAuthorised;
import com.anglikana.backoffice.security.EntiteProtegee;
import com.anglikana.backoffice.security.NeedAuthorisation;
import com.anglikana.backoffice.dto.ParoisseDTO;
import com.anglikana.backoffice.dto.EgliseDTO;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.District;
import com.anglikana.backoffice.entity.Paroisse;
import com.anglikana.backoffice.mapper.ParoisseMapper;
import com.anglikana.backoffice.mapper.GeometrieMapper;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.DistrictRepository;
import com.anglikana.backoffice.repository.ParoisseRepository;
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
@RequestMapping("/api/paroisses")
@Tag(name = "Paroisses", description = "CRUD des paroisses (niveau 4 de la hiérarchie, rattachées à un district) et églises d'une paroisse")
public class ParoisseController {

    private final ParoisseRepository paroisseRepository;
    private final DistrictRepository districtRepository;
    private final ClergeRepository clergeRepository;
    private final HierarchieService hierarchieService;
    private final AutorisationService autorisationService;

    public ParoisseController(ParoisseRepository paroisseRepository,
                               DistrictRepository districtRepository,
                               ClergeRepository clergeRepository,
                               HierarchieService hierarchieService,
                              AutorisationService autorisationService) {
        this.autorisationService = autorisationService;
        this.paroisseRepository = paroisseRepository;
        this.districtRepository = districtRepository;
        this.clergeRepository = clergeRepository;
        this.hierarchieService = hierarchieService;
    }

    @GetMapping
    @Operation(summary = "Lister les paroisses", description = "Toutes les paroisses, sans pagination. Lecture publique, éléments supprimés (etat = -1) exclus.")
    @ApiResponse(responseCode = "200", description = "Liste des paroisses")
    public List<ParoisseDTO> getAll() {
        return paroisseRepository.findAll()
                .stream().map(ParoisseMapper::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une paroisse", description = "Lecture publique, éléments supprimés (etat = -1) exclus. La carte QGIS est renvoyée en GeoJSON Polygon (null si absente).")
    @ApiResponse(responseCode = "200", description = "Paroisse trouvée")
    @ApiResponse(responseCode = "404", description = "Paroisse introuvable", content = @Content)
    public ResponseEntity<ParoisseDTO> getById(@Parameter(description = "Identifiant de la paroisse") @PathVariable Integer id) {
        Optional<Paroisse> paroisseOptionnel = paroisseRepository.findById(id);

        if (paroisseOptionnel.isPresent()) {
            Paroisse paroisse = paroisseOptionnel.get();
            return ResponseEntity.ok(ParoisseMapper.toDTO(paroisse));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/eglises")
    @Operation(summary = "Lister les églises d'une paroisse", description = "Églises rattachées à la paroisse, triées par nom. Lecture publique, éléments supprimés (etat = -1) exclus.")
    @ApiResponse(responseCode = "200", description = "Églises de la paroisse (liste vide si aucune)")
    @ApiResponse(responseCode = "404", description = "Paroisse introuvable", content = @Content)
    public ResponseEntity<List<EgliseDTO>> getEglises(@Parameter(description = "Identifiant de la paroisse") @PathVariable Integer id) {
        return hierarchieService.getEglisesDeLaParoisse(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @NeedAuthorisation(entity = EntiteProtegee.DISTRICT, id = "#dto.districtId")
    @CascadeAuthorised
    @Operation(summary = "Créer une paroisse",
            description = "Réservé au responsable du district districtId ou de l'un de ses ancêtres. districtId est obligatoire ; carteQgis, si fournie, doit être un GeoJSON Polygon d'un seul tenant en WGS 84.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "201", description = "Paroisse créée",
            content = @Content(schema = @Schema(implementation = ParoisseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Carte QGIS invalide (message explicatif)",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "403", description = "Jeton absent, districtId absent, ou hors du périmètre du district districtId", content = @Content)
    @ApiResponse(responseCode = "404", description = "District districtId introuvable ou supprimé", content = @Content)
    public ResponseEntity<?> create(@RequestBody ParoisseDTO dto) {
        Paroisse paroisse = new Paroisse();
        try {
            appliquerDTO(paroisse, dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        Paroisse saved = paroisseRepository.save(paroisse);
        return ResponseEntity.status(201).body(ParoisseMapper.toDTO(saved));
    }

    @PutMapping("/{id}")
    @NeedAuthorisation(entity = EntiteProtegee.PAROISSE, id = "#id")
    @CascadeAuthorised
    @Operation(summary = "Modifier une paroisse",
            description = "Réservé au responsable de la paroisse ou de l'un de ses ancêtres. Changer districtId exige aussi d'être responsable du nouveau district ou d'un de ses ancêtres. Remplace tous les champs, y compris la carte QGIS (null pour la supprimer).")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "200", description = "Paroisse modifiée",
            content = @Content(schema = @Schema(implementation = ParoisseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Carte QGIS invalide (message explicatif)",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "403", description = "Jeton absent, ou hors du périmètre de la paroisse ou du nouveau district", content = @Content)
    @ApiResponse(responseCode = "404", description = "Paroisse ou nouveau district introuvable", content = @Content)
    public ResponseEntity<?> update(@Parameter(description = "Identifiant de la paroisse") @PathVariable Integer id, @RequestBody ParoisseDTO dto) {
        Optional<Paroisse> paroisseOptionnel = paroisseRepository.findById(id);

        if (paroisseOptionnel.isPresent()) {
            Paroisse paroisse = paroisseOptionnel.get();
            Integer parentActuel = paroisse.getDistrict() != null ? paroisse.getDistrict().getId() : null;
            if (!Objects.equals(dto.getDistrictId(), parentActuel)) {
                // Rattachement à un autre parent : il doit lui aussi être dans le périmètre.
                autorisationService.verifier(EntiteProtegee.DISTRICT, dto.getDistrictId(), true);
            }
            try {
                appliquerDTO(paroisse, dto);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            Paroisse updated = paroisseRepository.save(paroisse);
            return ResponseEntity.ok(ParoisseMapper.toDTO(updated));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @NeedAuthorisation(entity = EntiteProtegee.PAROISSE, id = "#id")
    @CascadeAuthorised
    @Operation(summary = "Supprimer une paroisse",
            description = "Réservé au responsable de la paroisse ou de l'un de ses ancêtres. Suppression logique (etat = -1), refusée tant qu'une église active y est rattachée.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "204", description = "Paroisse supprimée", content = @Content)
    @ApiResponse(responseCode = "403", description = "Jeton absent ou hors du périmètre de la paroisse", content = @Content)
    @ApiResponse(responseCode = "404", description = "Paroisse introuvable", content = @Content)
    @ApiResponse(responseCode = "409", description = "Au moins une fille directe est encore active (message explicatif)",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    public ResponseEntity<?> delete(@Parameter(description = "Identifiant de la paroisse") @PathVariable Integer id) {
        if (!paroisseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        Optional<String> refus = hierarchieService.refusSuppression(NiveauHierarchique.PAROISSE, id);
        if (refus.isPresent()) {
            return ResponseEntity.status(409).body(refus.get());
        }
        paroisseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void appliquerDTO(Paroisse paroisse, ParoisseDTO dto) {
        Polygon carteQgis = GeometrieMapper.depuisGeoJson(dto.getCarteQgis());
        paroisse.setCarteQgis(carteQgis);
        paroisse.setNom(dto.getNom());

        if (dto.getDistrictId() != null) {
            Optional<District> districtOptionnel = districtRepository.findById(dto.getDistrictId());
            if (districtOptionnel.isPresent()) {
                paroisse.setDistrict(districtOptionnel.get());
            } else {
                paroisse.setDistrict(null);
            }
        } else {
            paroisse.setDistrict(null);
        }

        if (dto.getResponsableId() != null) {
            Optional<Clerge> clergeOptionnel = clergeRepository.findById(dto.getResponsableId());
            if (clergeOptionnel.isPresent()) {
                paroisse.setResponsable(clergeOptionnel.get());
            } else {
                paroisse.setResponsable(null);
            }
        } else {
            paroisse.setResponsable(null);
        }
    }
}