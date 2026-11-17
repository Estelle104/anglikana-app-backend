package com.anglikana.backoffice.dto;

/** Totaux de toute la hiérarchie ; {@code clerges} compte les membres distincts, tous diocèses confondus. */
public class TotauxHierarchieDTO extends StatistiquesEntiteDTO {

    private long dioceses;

    public long getDioceses() { return dioceses; }
    public void setDioceses(long dioceses) { this.dioceses = dioceses; }
}
