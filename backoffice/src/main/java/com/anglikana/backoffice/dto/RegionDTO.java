package com.anglikana.backoffice.dto;

public class RegionDTO {

    private Integer id;
    private String nom;
    private Integer dioceseId;
    private Integer responsableId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public Integer getDioceseId() { return dioceseId; }
    public void setDioceseId(Integer dioceseId) { this.dioceseId = dioceseId; }

    public Integer getResponsableId() { return responsableId; }
    public void setResponsableId(Integer responsableId) { this.responsableId = responsableId; }
}
