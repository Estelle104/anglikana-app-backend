package com.anglikana.backoffice.security;

import com.anglikana.backoffice.dto.NiveauHierarchique;

import java.util.Arrays;
import java.util.Optional;

/**
 * Entités dont l'accès est contrôlé par le responsable ({@code responsable_id}), de la plus basse
 * à la plus haute. Chaque valeur connaît sa table et la colonne qui la rattache à son parent :
 * c'est la liste blanche des noms injectés dans le SQL de {@code PerimetreRepository}.
 */
public enum EntiteProtegee {

    RESSOURCE("ressource", "ressource", "eglise_id"),
    EGLISE("eglise", "eglise", "paroisse_id"),
    PAROISSE("paroisse", "paroisse", "district_id"),
    DISTRICT("district", "district", "region_id"),
    REGION("region", "region", "diocese_id"),
    DIOCESE("diocese", "diocese", null);

    private final String code;
    private final String table;
    private final String colonneParent;

    EntiteProtegee(String code, String table, String colonneParent) {
        this.code = code;
        this.table = table;
        this.colonneParent = colonneParent;
    }

    public String getCode() { return code; }

    public String getTable() { return table; }

    /** Colonne de rattachement au parent ; null pour le diocèse. */
    public String getColonneParent() { return colonneParent; }

    /** Parent dans la hiérarchie ; vide pour le diocèse. */
    public Optional<EntiteProtegee> parent() {
        return colonneParent == null ? Optional.empty() : Optional.of(values()[ordinal() + 1]);
    }

    /** Insensible à la casse ; vide si le code est inconnu. */
    public static Optional<EntiteProtegee> depuisCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String recherche = code.trim();
        return Arrays.stream(values())
                .filter(entite -> entite.code.equalsIgnoreCase(recherche))
                .findFirst();
    }

    public static EntiteProtegee depuisNiveau(NiveauHierarchique niveau) {
        return switch (niveau) {
            case DIOCESE -> DIOCESE;
            case REGION -> REGION;
            case DISTRICT -> DISTRICT;
            case PAROISSE -> PAROISSE;
            case EGLISE -> EGLISE;
        };
    }
}
