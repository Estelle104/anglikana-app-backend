package com.anglikana.backoffice.dto;

import tools.jackson.databind.JsonNode;

public class DioceseDTO {

    private Integer id;
    private String nom;
    /** Limite de la zone en géométrie GeoJSON (Polygon, WGS 84) ; null si aucune carte. */
    private JsonNode carteQgis;
    private String presentation;
    private String coordonnees;
    private Integer responsableId;

    public DioceseDTO() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public JsonNode getCarteQgis() { return carteQgis; }
    public void setCarteQgis(JsonNode carteQgis) { this.carteQgis = carteQgis; }

    public String getPresentation() { return presentation; }
    public void setPresentation(String presentation) { this.presentation = presentation; }

    public String getCoordonnees() { return coordonnees; }
    public void setCoordonnees(String coordonnees) { this.coordonnees = coordonnees; }

    public Integer getResponsableId() { return responsableId; }
    public void setResponsableId(Integer responsableId) { this.responsableId = responsableId; }
}