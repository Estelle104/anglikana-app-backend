package com.anglikana.backoffice.dto;

public class ReferenceEntiteDTO {

    private NiveauHierarchique niveau;
    private Integer id;
    private String nom;

    public ReferenceEntiteDTO() {
    }

    public ReferenceEntiteDTO(NiveauHierarchique niveau, Integer id, String nom) {
        this.niveau = niveau;
        this.id = id;
        this.nom = nom;
    }

    public NiveauHierarchique getNiveau() { return niveau; }
    public void setNiveau(NiveauHierarchique niveau) { this.niveau = niveau; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
}
