package com.anglikana.backoffice.dto;

import java.util.List;

public class ApercuHierarchieDTO {

    private TotauxHierarchieDTO totaux;
    private List<NoeudHierarchiqueDTO> dioceses;

    public TotauxHierarchieDTO getTotaux() { return totaux; }
    public void setTotaux(TotauxHierarchieDTO totaux) { this.totaux = totaux; }

    public List<NoeudHierarchiqueDTO> getDioceses() { return dioceses; }
    public void setDioceses(List<NoeudHierarchiqueDTO> dioceses) { this.dioceses = dioceses; }
}
