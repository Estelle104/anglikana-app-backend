package com.anglikana.backoffice.dto;

import java.util.List;

public class ZoneFeatureCollectionDTO {

    private List<ZoneFeatureDTO> features;

    public ZoneFeatureCollectionDTO() {
    }

    public ZoneFeatureCollectionDTO(List<ZoneFeatureDTO> features) {
        this.features = features;
    }

    public String getType() { return "FeatureCollection"; }

    public List<ZoneFeatureDTO> getFeatures() { return features; }
    public void setFeatures(List<ZoneFeatureDTO> features) { this.features = features; }
}
