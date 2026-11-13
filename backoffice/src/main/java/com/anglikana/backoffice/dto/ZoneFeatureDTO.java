package com.anglikana.backoffice.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;

/** Feature GeoJSON (RFC 7946) d'une zone : propriétés {id, nom, niveau} et polygone QGIS. */
public class ZoneFeatureDTO {

    private ReferenceEntiteDTO properties;
    /** Géométrie GeoJSON produite par PostGIS (ST_AsGeoJSON), transmise telle quelle ; null si aucune limite. */
    private String geometry;

    public ZoneFeatureDTO() {
    }

    public ZoneFeatureDTO(ReferenceEntiteDTO properties, String geometry) {
        this.properties = properties;
        this.geometry = geometry;
    }

    public String getType() { return "Feature"; }

    public ReferenceEntiteDTO getProperties() { return properties; }
    public void setProperties(ReferenceEntiteDTO properties) { this.properties = properties; }

    @JsonRawValue
    public String getGeometry() { return geometry; }
    public void setGeometry(String geometry) { this.geometry = geometry; }
}
