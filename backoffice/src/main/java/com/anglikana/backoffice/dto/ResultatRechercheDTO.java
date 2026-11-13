package com.anglikana.backoffice.dto;

public class ResultatRechercheDTO extends ReferenceEntiteDTO {

    /** Rattachement hiérarchique lisible, ex. « Région X · Diocèse Y ». */
    private String contexte;

    public ResultatRechercheDTO() {
    }

    public ResultatRechercheDTO(NiveauHierarchique niveau, Integer id, String nom, String contexte) {
        super(niveau, id, nom);
        this.contexte = contexte;
    }

    public String getContexte() { return contexte; }
    public void setContexte(String contexte) { this.contexte = contexte; }
}
