package com.anglikana.backoffice.dto;

import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * FeatureCollection GeoJSON exportée depuis QGIS (Exporter → GeoJSON).
 * Chaque feature doit porter l'identifiant de l'entité dans {@code properties.id}.
 */
public class ImportLimitesRequest {

    private String type;
    private List<Feature> features;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<Feature> getFeatures() { return features; }
    public void setFeatures(List<Feature> features) { this.features = features; }

    public static class Feature {

        private Map<String, Object> properties;
        private JsonNode geometry;

        public Map<String, Object> getProperties() { return properties; }
        public void setProperties(Map<String, Object> properties) { this.properties = properties; }

        public JsonNode getGeometry() { return geometry; }
        public void setGeometry(JsonNode geometry) { this.geometry = geometry; }
    }
}
