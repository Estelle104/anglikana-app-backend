package com.anglikana.backoffice.security;

/**
 * Entité ciblée par un contrôle d'autorisation introuvable ou supprimée (etat = -1).
 * Traduite en 404 par {@link AutorisationExceptionHandler}.
 */
public class EntiteIntrouvableException extends RuntimeException {

    public EntiteIntrouvableException(EntiteProtegee entite, Integer id) {
        super("Entité introuvable : " + entite.getCode() + " " + id);
    }
}
