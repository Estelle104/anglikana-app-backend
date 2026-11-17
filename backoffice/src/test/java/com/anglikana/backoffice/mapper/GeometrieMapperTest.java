package com.anglikana.backoffice.mapper;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeometrieMapperTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final String ANNEAU_CARRE = "[[47,-19],[48,-19],[48,-18],[47,-18],[47,-19]]";
    private static final String CARRE = "{\"type\":\"Polygon\",\"coordinates\":[" + ANNEAU_CARRE + "]}";

    @Test
    void polygoneEnregistreEnWgs84() {
        Polygon carte = GeometrieMapper.depuisGeoJson(JSON.readTree(CARRE));

        assertThat(carte.getSRID()).isEqualTo(4326);
        assertThat(carte.isValid()).isTrue();
        assertThat(carte.getArea()).isEqualTo(1.0);
    }

    @Test
    void allerRetourGeoJsonAvecTrou() {
        String avecTrou = "{\"type\":\"Polygon\",\"coordinates\":[" + ANNEAU_CARRE + ","
                + "[[47.4,-18.6],[47.6,-18.6],[47.6,-18.4],[47.4,-18.4],[47.4,-18.6]]]}";

        JsonNode geojson = GeometrieMapper.versGeoJson(GeometrieMapper.depuisGeoJson(JSON.readTree(avecTrou)));

        assertThat(geojson.get("type").asString()).isEqualTo("Polygon");
        assertThat(geojson.get("coordinates").size()).isEqualTo(2);
        assertThat(geojson.get("coordinates").get(0).get(1).get(0).asDouble()).isEqualTo(48.0);
        assertThat(GeometrieMapper.depuisGeoJson(geojson).getArea()).isCloseTo(0.96, Offset.offset(1e-9));
    }

    @Test
    void multiPolygoneDUneSeulePartieConvertiEnPolygone() {
        // Forme habituelle des exports QGIS, même pour une zone d'un seul tenant.
        String multi = "{\"type\":\"MultiPolygon\",\"coordinates\":[[" + ANNEAU_CARRE + "]]}";

        Polygon carte = GeometrieMapper.depuisGeoJson(JSON.readTree(multi));

        assertThat(carte.getArea()).isEqualTo(1.0);
        assertThat(GeometrieMapper.versGeoJson(carte).get("type").asString()).isEqualTo("Polygon");
    }

    @Test
    void zoneEnPlusieursPartiesRefusee() {
        String deuxParties = "{\"type\":\"MultiPolygon\",\"coordinates\":[[" + ANNEAU_CARRE + "],"
                + "[[[49,-19],[50,-19],[50,-18],[49,-19]]]]}";

        assertThatThrownBy(() -> GeometrieMapper.depuisGeoJson(JSON.readTree(deuxParties)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seul tenant")
                .hasMessageContaining("2 parties");
    }

    @Test
    void polygoneInvalideCorrigeSIlResteDUnSeulTenant() {
        // Sommet dupliqué en « aller-retour » (pointe nulle) : invalide, corrigé en un seul carré.
        String avecPointe = "{\"type\":\"Polygon\",\"coordinates\":[[[47,-19],[48,-19],[48.5,-19],[48,-19],"
                + "[48,-18],[47,-18],[47,-19]]]}";

        Polygon carte = GeometrieMapper.depuisGeoJson(JSON.readTree(avecPointe));

        assertThat(carte.isValid()).isTrue();
        assertThat(carte.getArea()).isEqualTo(1.0);
    }

    @Test
    void polygoneInvalideScindeParLaCorrectionRefuse() {
        // « Nœud papillon » : la correction produit deux triangles, la zone ne serait plus d'un seul tenant.
        String papillon = "{\"type\":\"Polygon\",\"coordinates\":[[[47,-19],[48,-18],[48,-19],[47,-18],[47,-19]]]}";

        assertThatThrownBy(() -> GeometrieMapper.depuisGeoJson(JSON.readTree(papillon)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 parties")
                .hasMessageContaining("QGIS");
    }

    @Test
    void absenceDeCarte() {
        assertThat(GeometrieMapper.depuisGeoJson(null)).isNull();
        assertThat(GeometrieMapper.depuisGeoJson(NullNode.getInstance())).isNull();
        assertThat(GeometrieMapper.depuisGeoJson(StringNode.valueOf(" "))).isNull();
        assertThat(GeometrieMapper.versGeoJson(null)).isNull();
    }

    @Test
    void geometrieNonSurfaciqueRefusee() {
        assertThatThrownBy(() -> GeometrieMapper.depuisGeoJson(JSON.readTree("{\"type\":\"Point\",\"coordinates\":[47,-18]}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GeoJSON Polygon");
        // Ancien format : chemin d'un fichier .qgz
        assertThatThrownBy(() -> GeometrieMapper.depuisGeoJson(StringNode.valueOf("maps/diocese_tana.qgz")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anneauMalFormeRefuse() {
        String ouvert = "{\"type\":\"Polygon\",\"coordinates\":[[[47,-19],[48,-19],[48,-18],[47,-18]]]}";
        assertThatThrownBy(() -> GeometrieMapper.depuisGeoJson(JSON.readTree(ouvert)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fermé");

        String tropCourt = "{\"type\":\"Polygon\",\"coordinates\":[[[47,-19],[48,-19],[47,-19]]]}";
        assertThatThrownBy(() -> GeometrieMapper.depuisGeoJson(JSON.readTree(tropCourt)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4 positions");
    }

    @Test
    void coucheExporteeHorsWgs84Refusee() {
        // Coordonnées métriques (projection Laborde, EPSG:29702) au lieu de degrés.
        String laborde = "{\"type\":\"Polygon\",\"coordinates\":[[[500000,800000],[510000,800000],"
                + "[510000,810000],[500000,810000],[500000,800000]]]}";

        assertThatThrownBy(() -> GeometrieMapper.depuisGeoJson(JSON.readTree(laborde)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EPSG:4326");
    }
}
