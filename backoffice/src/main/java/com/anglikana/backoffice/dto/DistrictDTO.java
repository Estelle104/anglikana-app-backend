package com.anglikana.backoffice.dto;

public class DistrictDTO {

    private Integer id;
    private String nom;
    private String carteQgis;
    private Integer regionId;
    private Integer responsableId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCarteQgis() { return carteQgis; }
    public void setCarteQgis(String carteQgis) { this.carteQgis = carteQgis; }

    public Integer getRegionId() { return regionId; }
    public void setRegionId(Integer regionId) { this.regionId = regionId; }

    public Integer getResponsableId() { return responsableId; }
    public void setResponsableId(Integer responsableId) { this.responsableId = responsableId; }
}