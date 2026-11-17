package com.anglikana.backoffice.mapper;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.geom.util.PolygonExtracter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Conversion entre la colonne {@code carte_qgis} (Polygon JTS, SRID 4326) et sa
 * représentation GeoJSON (RFC 7946, positions [longitude, latitude]) échangée avec le frontend.
 *
 * <p>Une zone est d'un seul tenant : seul un Polygon est enregistré (trous autorisés).
 * Un MultiPolygon d'une seule partie, forme fréquente des exports QGIS, est accepté et
 * converti en Polygon ; un MultiPolygon de plusieurs parties est refusé. Une géométrie
 * invalide (anneau auto-intersectant...) est corrigée par {@link GeometryFixer}, équivalent
 * de ST_MakeValid, à condition que le résultat reste d'un seul tenant.</p>
 */
public final class GeometrieMapper {

    public static final int SRID_GPS = 4326;

    private static final GeometryFactory FABRIQUE = new GeometryFactory(new PrecisionModel(), SRID_GPS);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TYPE_ATTENDU = "La carte QGIS doit être une géométrie GeoJSON Polygon";

    private GeometrieMapper() {
    }

    /** Géométrie GeoJSON Polygon de la carte, ou null si aucune carte n'est enregistrée. */
    public static JsonNode versGeoJson(Polygon carte) {
        if (carte == null) {
            return null;
        }

        ObjectNode geojson = JSON.createObjectNode();
        geojson.put("type", "Polygon");
        ArrayNode anneaux = geojson.putArray("coordinates");
        ecrireAnneau(anneaux, carte.getExteriorRing());
        for (int i = 0; i < carte.getNumInteriorRing(); i++) {
            ecrireAnneau(anneaux, carte.getInteriorRingN(i));
        }
        return geojson;
    }

    /**
     * Carte à enregistrer à partir d'une géométrie GeoJSON.
     * Absente, JSON null ou texte vide : aucune carte (null).
     *
     * @throws IllegalArgumentException si la géométrie n'est pas un polygone GeoJSON d'un seul tenant en WGS 84
     */
    public static Polygon depuisGeoJson(JsonNode geojson) {
        if (geojson == null || geojson.isNull() || (geojson.isString() && geojson.asString().isBlank())) {
            return null;
        }

        JsonNode noeudType = geojson.get("type");
        JsonNode coordonnees = geojson.get("coordinates");
        if (noeudType == null || !noeudType.isString() || coordonnees == null || !coordonnees.isArray()) {
            throw new IllegalArgumentException(TYPE_ATTENDU);
        }

        Polygon polygone = switch (noeudType.asString()) {
            case "Polygon" -> lirePolygone(coordonnees);
            case "MultiPolygon" -> {
                if (coordonnees.size() != 1) {
                    throw new IllegalArgumentException("La zone doit être d'un seul tenant : la carte contient "
                            + coordonnees.size() + " parties (MultiPolygon)");
                }
                yield lirePolygone(coordonnees.get(0));
            }
            default -> throw new IllegalArgumentException(TYPE_ATTENDU);
        };

        verifierWgs84(polygone);
        return polygone.isValid() ? polygone : corriger(polygone);
    }

    /** Corrige une géométrie invalide ; refusée si la correction la scinde en plusieurs surfaces. */
    private static Polygon corriger(Polygon invalide) {
        Geometry corrigee = GeometryFixer.fix(invalide);

        List<Polygon> parties = new ArrayList<>();
        for (Object partie : PolygonExtracter.getPolygons(corrigee)) {
            if (!((Polygon) partie).isEmpty()) {
                parties.add((Polygon) partie);
            }
        }
        if (parties.isEmpty()) {
            throw new IllegalArgumentException("La carte QGIS ne contient aucune surface");
        }
        if (parties.size() > 1) {
            throw new IllegalArgumentException("Polygone invalide (contour qui se recoupe) : une fois corrigé, il forme "
                    + parties.size() + " parties. Corrigez la géométrie dans QGIS (Vérifier la validité)");
        }
        return parties.get(0);
    }

    private static Polygon lirePolygone(JsonNode anneaux) {
        if (!anneaux.isArray() || anneaux.isEmpty()) {
            throw new IllegalArgumentException("Polygone GeoJSON sans anneau extérieur");
        }

        LinearRing exterieur = lireAnneau(anneaux.get(0));
        LinearRing[] trous = new LinearRing[anneaux.size() - 1];
        for (int i = 1; i < anneaux.size(); i++) {
            trous[i - 1] = lireAnneau(anneaux.get(i));
        }
        return FABRIQUE.createPolygon(exterieur, trous);
    }

    private static LinearRing lireAnneau(JsonNode positions) {
        if (!positions.isArray() || positions.size() < 4) {
            throw new IllegalArgumentException("Un anneau de polygone GeoJSON doit contenir au moins 4 positions");
        }

        Coordinate[] coordonnees = new Coordinate[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            JsonNode position = positions.get(i);
            if (!position.isArray() || position.size() < 2 || !position.get(0).isNumber() || !position.get(1).isNumber()) {
                throw new IllegalArgumentException("Position GeoJSON invalide : [longitude, latitude] attendu");
            }
            coordonnees[i] = new Coordinate(position.get(0).asDouble(), position.get(1).asDouble());
        }
        if (!coordonnees[0].equals2D(coordonnees[coordonnees.length - 1])) {
            throw new IllegalArgumentException("Un anneau de polygone GeoJSON doit être fermé (première position = dernière)");
        }
        return FABRIQUE.createLinearRing(coordonnees);
    }

    /** Rejette les couches exportées dans un autre système de coordonnées (ex. Laborde, EPSG:29702). */
    private static void verifierWgs84(Geometry geometrie) {
        Envelope emprise = geometrie.getEnvelopeInternal();
        if (emprise.getMinX() < -180 || emprise.getMaxX() > 180 || emprise.getMinY() < -90 || emprise.getMaxY() > 90) {
            throw new IllegalArgumentException(
                    "Coordonnées hors WGS 84 : exportez la couche QGIS en EPSG:4326 (longitude, latitude)");
        }
    }

    private static void ecrireAnneau(ArrayNode anneaux, LineString anneau) {
        ArrayNode positions = anneaux.addArray();
        for (Coordinate coordonnee : anneau.getCoordinates()) {
            positions.addArray().add(coordonnee.getX()).add(coordonnee.getY());
        }
    }
}
