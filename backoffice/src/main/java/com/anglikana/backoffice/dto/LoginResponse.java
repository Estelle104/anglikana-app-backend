package com.anglikana.backoffice.dto;

public class LoginResponse {

    private String token;
    private String nomUtilisateur;
    private String role;

    public LoginResponse() {
    }

    public LoginResponse(String token, String nomUtilisateur, String role) {
        this.token = token;
        this.nomUtilisateur = nomUtilisateur;
        this.role = role;
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
}