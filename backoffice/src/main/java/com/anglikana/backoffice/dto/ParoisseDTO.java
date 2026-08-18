package com.anglikana.backoffice.dto;

public class ParoisseDTO {

    private Integer id;
    private String nom;
    private String carteQgis;
    private Integer districtId;
    private Integer responsableId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCarteQgis() { return carteQgis; }
    public void setCarteQgis(String carteQgis) { this.carteQgis = carteQgis; }

    public Integer getDistrictId() { return districtId; }
    public void setDistrictId(Integer districtId) { this.districtId = districtId; }

    public Integer getResponsableId() { return responsableId; }
    public void setResponsableId(Integer responsableId) { this.responsableId = responsableId; }
}