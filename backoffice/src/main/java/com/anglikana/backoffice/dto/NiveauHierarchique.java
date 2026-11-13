package com.anglikana.backoffice.dto;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Niveaux de la hiérarchie ecclésiastique, du plus haut au plus bas.
 * Sérialisé en JSON par son code ("diocese", "region"...), aligné sur le frontend Angular.
 */
public enum NiveauHierarchique {

    DIOCESE("diocese", "Diocèse"),
    REGION("region", "Région"),
    DISTRICT("district", "District"),
    PAROISSE("paroisse", "Paroisse"),
    EGLISE("eglise", "Église");

    private final String code;
    private final String libelle;

    NiveauHierarchique(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
    }

    @JsonValue
    public String getCode() { return code; }

    public String getLibelle() { return libelle; }

    public static Optional<NiveauHierarchique> depuisCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String recherche = code.trim();
        return Arrays.stream(values())
                .filter(niveau -> niveau.code.equalsIgnoreCase(recherche))
                .findFirst();
    }

    /** Niveaux inférieurs, du plus proche au plus éloigné. */
    public List<NiveauHierarchique> inferieurs() {
        NiveauHierarchique[] niveaux = values();
        return Arrays.asList(niveaux).subList(ordinal() + 1, niveaux.length);
    }

    public Optional<NiveauHierarchique> enfant() {
        List<NiveauHierarchique> inferieurs = inferieurs();
        return inferieurs.isEmpty() ? Optional.empty() : Optional.of(inferieurs.get(0));
    }

    public boolean estSuperieurA(NiveauHierarchique autre) {
        return ordinal() < autre.ordinal();
    }

    /** Seuls les diocèses, régions et districts possèdent des limites géographiques (couches QGIS). */
    public boolean estGeographique() {
        return this == DIOCESE || this == REGION || this == DISTRICT;
    }

    /** Niveau géographique directement inférieur (ex. régions d'un diocèse). */
    public Optional<NiveauHierarchique> enfantGeographique() {
        return enfant().filter(NiveauHierarchique::estGeographique);
    }
}
