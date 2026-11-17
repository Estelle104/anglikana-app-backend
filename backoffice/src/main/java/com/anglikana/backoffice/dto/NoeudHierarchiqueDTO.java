package com.anglikana.backoffice.dto;

public class NoeudHierarchiqueDTO extends ReferenceEntiteDTO {

    private StatistiquesEntiteDTO statistiques;
    private ClergeDTO responsable;

    public StatistiquesEntiteDTO getStatistiques() { return statistiques; }
    public void setStatistiques(StatistiquesEntiteDTO statistiques) { this.statistiques = statistiques; }

    public ClergeDTO getResponsable() { return responsable; }
    public void setResponsable(ClergeDTO responsable) { this.responsable = responsable; }
}
