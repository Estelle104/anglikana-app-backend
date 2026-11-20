package com.anglikana.backoffice.dto;

public class LoginResponse {

    private String token;
    private String nomUtilisateur;
    private String role;
    /** Niveau du rôle : 5 = Évêque ... 1 = Katekista, 0 sans rôle. */
    private int niveau;

    public LoginResponse() {
    }

    public LoginResponse(String token, String nomUtilisateur, String role, int niveau) {
        this.token = token;
        this.nomUtilisateur = nomUtilisateur;
        this.role = role;
        this.niveau = niveau;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNomUtilisateur() {
        return nomUtilisateur;
    }

    public void setNomUtilisateur(String nomUtilisateur) {
        this.nomUtilisateur = nomUtilisateur;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getNiveau() {
        return niveau;
    }

    public void setNiveau(int niveau) {
        this.niveau = niveau;
    }
}
