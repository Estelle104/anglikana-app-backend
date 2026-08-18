package com.anglikana.backoffice.dto;

public class RessourceDTO {

    private Integer id;
    private String nom;
    private String adresse;
    private Integer typeId;
    private Integer egliseId;
    private Integer responsableId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public Integer getTypeId() { return typeId; }
    public void setTypeId(Integer typeId) { this.typeId = typeId; }

    public Integer getEgliseId() { return egliseId; }
    public void setEgliseId(Integer egliseId) { this.egliseId = egliseId; }

    public Integer getResponsableId() { return responsableId; }
    public void setResponsableId(Integer responsableId) { this.responsableId = responsableId; }
}