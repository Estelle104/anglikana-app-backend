package com.anglikana.backoffice.dto;

public class EgliseDTO {

    private Integer id;
    private String nom;
    private String adresse;
    private Double latitude;
    private Double longitude;
    private String historique;
    private String lienFacebook;
    private Integer paroisseId;
    private Integer responsableId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getHistorique() { return historique; }
    public void setHistorique(String historique) { this.historique = historique; }

    public String getLienFacebook() { return lienFacebook; }
    public void setLienFacebook(String lienFacebook) { this.lienFacebook = lienFacebook; }

    public Integer getParoisseId() { return paroisseId; }
    public void setParoisseId(Integer paroisseId) { this.paroisseId = paroisseId; }

    public Integer getResponsableId() { return responsableId; }
    public void setResponsableId(Integer responsableId) { this.responsableId = responsableId; }
}