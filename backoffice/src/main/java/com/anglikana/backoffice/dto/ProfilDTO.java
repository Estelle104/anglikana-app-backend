package com.anglikana.backoffice.dto;

import java.util.List;

/**
 * Profil de l'utilisateur connecté (GET /api/moi) : son clergé, le niveau de son rôle et les
 * entités racines de son périmètre, d'où le back-office démarre la navigation.
 */
public class ProfilDTO {

    private String nomUtilisateur;
    private ClergeDTO clerge;
    private String role;
    /** 5 = Évêque ... 1 = Katekista, 0 sans rôle. */
    private int niveau;
    private List<ReferenceEntiteDTO> racines;

    public String getNomUtilisateur() { return nomUtilisateur; }
    public void setNomUtilisateur(String nomUtilisateur) { this.nomUtilisateur = nomUtilisateur; }

    public ClergeDTO getClerge() { return clerge; }
    public void setClerge(ClergeDTO clerge) { this.clerge = clerge; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getNiveau() { return niveau; }
    public void setNiveau(int niveau) { this.niveau = niveau; }

    public List<ReferenceEntiteDTO> getRacines() { return racines; }
    public void setRacines(List<ReferenceEntiteDTO> racines) { this.racines = racines; }
}
