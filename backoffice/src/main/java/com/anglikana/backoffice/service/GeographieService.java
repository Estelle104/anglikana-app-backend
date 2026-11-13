package com.anglikana.backoffice.service;

import com.anglikana.backoffice.dto.EgliseLocaliseeDTO;
import com.anglikana.backoffice.dto.ImportLimitesRequest;
import com.anglikana.backoffice.dto.ImportLimitesResultatDTO;
import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.dto.ReferenceEntiteDTO;
import com.anglikana.backoffice.dto.ResultatRechercheDTO;
import com.anglikana.backoffice.dto.ZoneFeatureCollectionDTO;
import com.anglikana.backoffice.dto.ZoneFeatureDTO;
import com.anglikana.backoffice.dto.ZoneGeographiqueDTO;
import com.anglikana.backoffice.repository.GeographieRepository;
import com.anglikana.backoffice.repository.GeographieRepository.LigneRecherche;
import com.anglikana.backoffice.repository.GeographieRepository.LigneZone;
import com.anglikana.backoffice.repository.HierarchieStatistiqueRepository;

import jakarta.persistence.PersistenceException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Consultation cartographique des zones (diocèse, région, district) : limites QGIS en GeoJSON,
 * recherche, et églises situées dans les polygones (requêtes spatiales PostGIS).
 */
@Service
public class GeographieService {

    private static final Set<String> TYPES_SURFACIQUES = Set.of("Polygon", "MultiPolygon");

    private final GeographieRepository geographieRepository;
    private final HierarchieStatistiqueRepository statistiqueRepository;

    public GeographieService(GeographieRepository geographieRepository,
                             HierarchieStatistiqueRepository statistiqueRepository) {
        this.geographieRepository = geographieRepository;
        this.statistiqueRepository = statistiqueRepository;
    }

    /** Couche GeoJSON d'un niveau : uniquement les zones dont la limite a été importée. */
    @Transactional(readOnly = true)
    public ZoneFeatureCollectionDTO getLimites(NiveauHierarchique niveau) {
        return new ZoneFeatureCollectionDTO(versFeatures(niveau, geographieRepository.limites(niveau)));
    }

    /**
     * Recherche par nom (insensible à la casse et aux accents) ou par identifiant.
     *
     * @param niveau null pour rechercher dans tous les niveaux géographiques
     * @param terme  null ou vide pour tout lister
     */
    @Transactional(readOnly = true)
    public List<ResultatRechercheDTO> rechercher(NiveauHierarchique niveau, String terme, int limite) {
        String niveauCode = niveau != null ? niveau.getCode() : "";
        String termeNormalise = terme != null ? terme.trim() : "";

        List<ResultatRechercheDTO> resultats = new ArrayList<>();
        for (LigneRecherche ligne : geographieRepository.rechercher(niveauCode, termeNormalise, limite)) {
            NiveauHierarchique niveauLigne = NiveauHierarchique.depuisCode(ligne.niveau()).orElseThrow();
            String contexte = contexte(niveauLigne, ligne.regionNom(), ligne.dioceseNom());
            resultats.add(new ResultatRechercheDTO(niveauLigne, ligne.id(), ligne.nom(), contexte));
        }
        return resultats;
    }

    /**
     * Zone sélectionnée : polygone, sous-zones et églises.
     * Les églises affichées sont celles situées dans le polygone (filtre spatial) ; les églises
     * rattachées à l'entité mais localisées hors du polygone sont signalées comme incohérences.
     * Sans limite importée, les églises rattachées hiérarchiquement sont renvoyées.
     */
    @Transactional(readOnly = true)
    public Optional<ZoneGeographiqueDTO> getZone(NiveauHierarchique niveau, Integer id) {
        Optional<LigneZone> ligneZone = geographieRepository.zone(niveau, id);
        if (ligneZone.isEmpty()) {
            return Optional.empty();
        }

        LigneZone zone = ligneZone.get();
        boolean limiteDisponible = zone.geojson() != null;

        ZoneGeographiqueDTO dto = new ZoneGeographiqueDTO();
        dto.setNiveau(niveau);
        dto.setId(zone.id());
        dto.setNom(zone.nom());
        dto.setContexte(contexte(niveau, id));
        dto.setFeature(versFeature(niveau, zone));
        dto.setLimiteDisponible(limiteDisponible);
        dto.setSousZones(niveau.enfantGeographique()
                .map(niveauInferieur -> versFeatures(niveauInferieur, geographieRepository.sousZones(niveau, id)))
                .orElse(List.of()));
        dto.setEglises(eglisesDeLaZone(niveau, id, limiteDisponible));
        dto.setEglisesHorsZone(limiteDisponible ? geographieRepository.eglisesHorsZone(niveau, id) : List.of());
        return Optional.of(dto);
    }

    /** Églises situées dans la zone ; vide si la zone n'existe pas. */
    @Transactional(readOnly = true)
    public Optional<List<EgliseLocaliseeDTO>> getEglisesDeLaZone(NiveauHierarchique niveau, Integer id) {
        return geographieRepository.zone(niveau, id)
                .map(zone -> eglisesDeLaZone(niveau, id, zone.geojson() != null));
    }

    /**
     * Importe les limites d'un niveau depuis une FeatureCollection GeoJSON exportée de QGIS.
     * Les features sans identifiant ou non surfaciques sont ignorées et signalées ;
     * une géométrie rejetée par PostGIS annule l'ensemble de l'import.
     *
     * @throws IllegalArgumentException si le document ou une géométrie est invalide
     */
    @Transactional
    public ImportLimitesResultatDTO importerLimites(NiveauHierarchique niveau, ImportLimitesRequest request) {
        if (request == null || !"FeatureCollection".equals(request.getType()) || request.getFeatures() == null) {
            throw new IllegalArgumentException("Le document doit être une FeatureCollection GeoJSON");
        }

        ImportLimitesResultatDTO resultat = new ImportLimitesResultatDTO();
        resultat.setNiveau(niveau);

        int rang = 0;
        for (ImportLimitesRequest.Feature feature : request.getFeatures()) {
            rang++;
            if (feature == null) {
                resultat.getErreurs().add("Feature n°" + rang + " : vide");
                continue;
            }

            Integer id = identifiant(feature.getProperties());
            if (id == null) {
                resultat.getErreurs().add("Feature n°" + rang + " : propriété « id » absente ou non numérique");
                continue;
            }

            JsonNode geometrie = feature.getGeometry();
            String type = geometrie != null && geometrie.has("type") ? geometrie.get("type").asString() : null;
            if (!TYPES_SURFACIQUES.contains(type)) {
                resultat.getErreurs().add("Feature n°" + rang + " (id " + id + ") : géométrie Polygon ou MultiPolygon attendue");
                continue;
            }

            int lignes;
            try {
                lignes = geographieRepository.majLimite(niveau, id, geometrie.toString());
            } catch (DataAccessException | PersistenceException e) {
                throw new IllegalArgumentException("Géométrie GeoJSON invalide pour la zone " + id, e);
            }

            if (lignes == 0) {
                resultat.getIdsInconnus().add(id);
            } else {
                resultat.setZonesMisesAJour(resultat.getZonesMisesAJour() + 1);
            }
        }
        return resultat;
    }

    private List<EgliseLocaliseeDTO> eglisesDeLaZone(NiveauHierarchique niveau, Integer id, boolean limiteDisponible) {
        return limiteDisponible
                ? geographieRepository.eglisesDansZone(niveau, id)
                : geographieRepository.eglisesRattachees(niveau, id);
    }

    /** Libellé du rattachement hiérarchique, du parent direct au diocèse : « Région X · Diocèse Y ». */
    private String contexte(NiveauHierarchique niveau, Integer id) {
        List<ReferenceEntiteDTO> chaine = statistiqueRepository.chaine(niveau, id);
        List<String> ancetres = new ArrayList<>();
        for (int i = chaine.size() - 2; i >= 0; i--) {
            ancetres.add(chaine.get(i).getNom());
        }
        return ancetres.isEmpty() ? niveau.getLibelle() : String.join(" · ", ancetres);
    }

    private static String contexte(NiveauHierarchique niveau, String regionNom, String dioceseNom) {
        String contexte = Stream.of(regionNom, dioceseNom)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" · "));
        return contexte.isEmpty() ? niveau.getLibelle() : contexte;
    }

    private static List<ZoneFeatureDTO> versFeatures(NiveauHierarchique niveau, List<LigneZone> zones) {
        return zones.stream().map(zone -> versFeature(niveau, zone)).toList();
    }

    private static ZoneFeatureDTO versFeature(NiveauHierarchique niveau, LigneZone zone) {
        return new ZoneFeatureDTO(new ReferenceEntiteDTO(niveau, zone.id(), zone.nom()), zone.geojson());
    }

    private static Integer identifiant(Map<String, Object> proprietes) {
        if (proprietes == null) {
            return null;
        }
        Object valeur = proprietes.get("id");
        if (valeur instanceof Number nombre) {
            return nombre.intValue();
        }
        if (valeur instanceof String texte) {
            try {
                return Integer.valueOf(texte.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
