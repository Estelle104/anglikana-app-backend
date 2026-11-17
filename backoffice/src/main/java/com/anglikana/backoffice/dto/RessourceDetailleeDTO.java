package com.anglikana.backoffice.dto;

public class RessourceDetailleeDTO {

    private RessourceDTO ressource;
    private String typeNom;
    private ClergeDTO responsable;

    public RessourceDTO getRessource() { return ressource; }
    public void setRessource(RessourceDTO ressource) { this.ressource = ressource; }

    public String getTypeNom() { return typeNom; }
    public void setTypeNom(String typeNom) { this.typeNom = typeNom; }

    public ClergeDTO getResponsable() { return responsable; }
    public void setResponsable(ClergeDTO responsable) { this.responsable = responsable; }
}
