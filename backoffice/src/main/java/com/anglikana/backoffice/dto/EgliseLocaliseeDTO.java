package com.anglikana.backoffice.dto;

public class EgliseLocaliseeDTO {

    private Integer id;
    private String nom;
    private String adresse;
    private Double latitude;
    private Double longitude;
    private String paroisseNom;

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

    public String getParoisseNom() { return paroisseNom; }
    public void setParoisseNom(String paroisseNom) { this.paroisseNom = paroisseNom; }
}
