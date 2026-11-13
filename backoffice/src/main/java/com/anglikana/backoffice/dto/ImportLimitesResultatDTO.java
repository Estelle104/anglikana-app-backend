package com.anglikana.backoffice.dto;

import java.util.ArrayList;
import java.util.List;

public class ImportLimitesResultatDTO {

    private NiveauHierarchique niveau;
    private int zonesMisesAJour;
    /** Identifiants présents dans le fichier mais absents de la base. */
    private List<Integer> idsInconnus = new ArrayList<>();
    /** Features ignorées (identifiant manquant, géométrie non surfacique...). */
    private List<String> erreurs = new ArrayList<>();

    public NiveauHierarchique getNiveau() { return niveau; }
    public void setNiveau(NiveauHierarchique niveau) { this.niveau = niveau; }

    public int getZonesMisesAJour() { return zonesMisesAJour; }
    public void setZonesMisesAJour(int zonesMisesAJour) { this.zonesMisesAJour = zonesMisesAJour; }

    public List<Integer> getIdsInconnus() { return idsInconnus; }
    public void setIdsInconnus(List<Integer> idsInconnus) { this.idsInconnus = idsInconnus; }

    public List<String> getErreurs() { return erreurs; }
    public void setErreurs(List<String> erreurs) { this.erreurs = erreurs; }
}
