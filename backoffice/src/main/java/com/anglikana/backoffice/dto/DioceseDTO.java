package com.anglikana.backoffice.dto;

public class DioceseDTO {

    private Integer id;
    private String nom;
    private String carteQgis;
    private String presentation;
    private String coordonnees;
    private Integer responsableId;

    public DioceseDTO() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCarteQgis() { return carteQgis; }
    public void setCarteQgis(String carteQgis) { this.carteQgis = carteQgis; }

    public String getPresentation() { return presentation; }
    public void setPresentation(String presentation) { this.presentation = presentation; }

    public String getCoordonnees() { return coordonnees; }
    public void setCoordonnees(String coordonnees) { this.coordonnees = coordonnees; }

    public Integer getResponsableId() { return responsableId; }
    public void setResponsableId(Integer responsableId) { this.responsableId = responsableId; }
}