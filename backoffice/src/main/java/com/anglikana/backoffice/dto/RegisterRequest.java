package com.anglikana.backoffice.dto;

public class RegisterRequest {

    private String nomUtilisateur;
    private String motDePasse;
    private Integer clergeId;

    public RegisterRequest() {
    }

    public String getNomUtilisateur() { return nomUtilisateur; }
    public void setNomUtilisateur(String nomUtilisateur) { this.nomUtilisateur = nomUtilisateur; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public Integer getClergeId() { return clergeId; }
    public void setClergeId(Integer clergeId) { this.clergeId = clergeId; }
}