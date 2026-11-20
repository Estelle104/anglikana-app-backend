package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.config.OpenApiConfig;
import com.anglikana.backoffice.dto.EgliseLocaliseeDTO;
import com.anglikana.backoffice.dto.ImportLimitesRequest;
import com.anglikana.backoffice.dto.ImportLimitesResultatDTO;
import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.dto.ResultatRechercheDTO;
import com.anglikana.backoffice.dto.ZoneFeatureCollectionDTO;
import com.anglikana.backoffice.dto.ZoneGeographiqueDTO;
import com.anglikana.backoffice.service.GeographieService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/geographie")
@Tag(name = "Géographie", description = "Limites QGIS (GeoJSON) des diocèses, régions et districts, recherche de zones et églises situées dans une zone (PostGIS)")
public class GeographieController {

    private static final String DESCRIPTION_NIVEAU = "Niveau géographique : diocese, region ou district";
    private static final String DESCRIPTION_400_NIVEAU = "Niveau géographique inconnu (attendu : diocese, region ou district)";

    private static final int LIMITE_RECHERCHE_MAX = 200;
    private static final int LONGUEUR_TERME_MAX = 150;

    private final GeographieService geographieService;

    public GeographieController(GeographieService geographieService) {
        this.geographieService = geographieService;
    }

    @GetMapping("/recherche")
    @Operation(summary = "Rechercher des zones",
            description = "Recherche par nom (insensible à la casse et aux accents) ou par identifiant exact, triée par nom. "
                    + "Chaque résultat porte son rattachement lisible (ex. « Région X · Diocèse Y »). Lecture publique.")
    @ApiResponse(responseCode = "200", description = "Zones trouvées",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResultatRechercheDTO.class))))
    @ApiResponse(responseCode = "400", description = "Niveau inconnu, limite hors de [1, 200] ou terme de plus de 150 caractères",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    public ResponseEntity<?> rechercher(@Parameter(description = "Terme recherché (nom ou identifiant) ; vide pour tout lister, 150 caractères au plus")
                                        @RequestParam(required = false) String q,
                                        @Parameter(description = "Restreindre à un niveau : diocese, region ou district ; vide pour les trois")
                                        @RequestParam(required = false) String niveau,
                                        @Parameter(description = "Nombre maximal de résultats, entre 1 et 200")
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
    @Operation(summary = "Exporter les limites d'un niveau",
            description = "FeatureCollection GeoJSON (RFC 7946, WGS 84) des zones du niveau dont la limite QGIS a été importée ; "
                    + "propriétés de chaque feature : niveau, id, nom. Lecture publique.")
    @ApiResponse(responseCode = "200", description = "Couche GeoJSON du niveau",
            content = @Content(schema = @Schema(implementation = ZoneFeatureCollectionDTO.class)))
    @ApiResponse(responseCode = "400", description = DESCRIPTION_400_NIVEAU,
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    public ResponseEntity<?> getLimites(@Parameter(description = DESCRIPTION_NIVEAU) @PathVariable String niveau) {
        Optional<NiveauHierarchique> niveauOptionnel = niveauGeographique(niveau);
        if (niveauOptionnel.isEmpty()) {
            return niveauGeographiqueInvalide(niveau);
        }
        return ResponseEntity.ok(geographieService.getLimites(niveauOptionnel.get()));
    }

    @PutMapping("/{niveau}/limites")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Importer les limites d'un niveau depuis QGIS",
            description = "Réservé au rôle ADMIN. Corps : FeatureCollection GeoJSON exportée de QGIS en EPSG:4326, chaque feature portant "
                    + "la propriété « id » de la zone. Chaque géométrie doit être un Polygon (ou MultiPolygon d'une seule partie). "
                    + "Les features refusées sont listées dans « erreurs » sans bloquer les autres ; les id absents de la base dans « idsInconnus ».")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "200", description = "Bilan de l'import",
            content = @Content(schema = @Schema(implementation = ImportLimitesResultatDTO.class)))
    @ApiResponse(responseCode = "400", description = "Niveau inconnu, ou corps qui n'est pas une FeatureCollection",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis ou jeton absent", content = @Content)
    public ResponseEntity<?> importerLimites(@Parameter(description = DESCRIPTION_NIVEAU) @PathVariable String niveau, @RequestBody ImportLimitesRequest request) {
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
    @Operation(summary = "Consulter une zone",
            description = "Polygone de la zone, sous-zones disposant d'une limite, églises situées dans le polygone (frontière incluse) "
                    + "et églises rattachées mais localisées hors du polygone (incohérences). Sans limite importée : "
                    + "limiteDisponible = false et les églises rattachées hiérarchiquement sont renvoyées. Lecture publique.")
    @ApiResponse(responseCode = "200", description = "Zone trouvée",
            content = @Content(schema = @Schema(implementation = ZoneGeographiqueDTO.class)))
    @ApiResponse(responseCode = "400", description = DESCRIPTION_400_NIVEAU,
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "404", description = "Zone introuvable", content = @Content)
    public ResponseEntity<?> getZone(@Parameter(description = DESCRIPTION_NIVEAU) @PathVariable String niveau,
                                     @Parameter(description = "Identifiant de la zone") @PathVariable Integer id) {
        Optional<NiveauHierarchique> niveauOptionnel = niveauGeographique(niveau);
        if (niveauOptionnel.isEmpty()) {
            return niveauGeographiqueInvalide(niveau);
        }

        return geographieService.getZone(niveauOptionnel.get(), id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{niveau}/{id}/eglises")
    @Operation(summary = "Lister les églises d'une zone",
            description = "Églises situées dans le polygone de la zone (frontière incluse) ; sans limite importée, "
                    + "églises localisées rattachées hiérarchiquement. Lecture publique.")
    @ApiResponse(responseCode = "200", description = "Églises de la zone",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = EgliseLocaliseeDTO.class))))
    @ApiResponse(responseCode = "400", description = DESCRIPTION_400_NIVEAU,
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "404", description = "Zone introuvable", content = @Content)
    public ResponseEntity<?> getEglisesDeLaZone(@Parameter(description = DESCRIPTION_NIVEAU) @PathVariable String niveau,
                                                @Parameter(description = "Identifiant de la zone") @PathVariable Integer id) {
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
