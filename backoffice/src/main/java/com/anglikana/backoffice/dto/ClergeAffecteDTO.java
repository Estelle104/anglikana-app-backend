package com.anglikana.backoffice.dto;

import java.util.List;

public class ClergeAffecteDTO {

    private ClergeDTO clerge;
    /** Noms des entités dont ce membre du clergé est responsable dans le périmètre. */
    private List<String> responsabilites;

    public ClergeAffecteDTO() {
    }

    public ClergeAffecteDTO(ClergeDTO clerge, List<String> responsabilites) {
        this.clerge = clerge;
        this.responsabilites = responsabilites;
    }

    public ClergeDTO getClerge() { return clerge; }
    public void setClerge(ClergeDTO clerge) { this.clerge = clerge; }

    public List<String> getResponsabilites() { return responsabilites; }
    public void setResponsabilites(List<String> responsabilites) { this.responsabilites = responsabilites; }
}
