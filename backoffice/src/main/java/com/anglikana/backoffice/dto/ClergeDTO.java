package com.anglikana.backoffice.dto;

import java.time.LocalDate;

public class ClergeDTO {

    private Integer id;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String hierarchie;
    private String telephone;
    private String email;
    private Integer roleId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getHierarchie() { return hierarchie; }
    public void setHierarchie(String hierarchie) { this.hierarchie = hierarchie; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }
}