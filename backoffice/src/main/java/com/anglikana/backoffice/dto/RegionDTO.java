package com.anglikana.backoffice.dto;

import tools.jackson.databind.JsonNode;

public class RegionDTO {

    private Integer id;
    private String nom;
    /** Limite de la zone en géométrie GeoJSON (Polygon, WGS 84) ; null si aucune carte. */
    private JsonNode carteQgis;
    private Integer dioceseId;
    private Integer responsableId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public JsonNode getCarteQgis() { return carteQgis; }
    public void setCarteQgis(JsonNode carteQgis) { this.carteQgis = carteQgis; }

    public Integer getDioceseId() { return dioceseId; }
    public void setDioceseId(Integer dioceseId) { this.dioceseId = dioceseId; }

    public Integer getResponsableId() { return responsableId; }
    public void setResponsableId(Integer responsableId) { this.responsableId = responsableId; }
}
