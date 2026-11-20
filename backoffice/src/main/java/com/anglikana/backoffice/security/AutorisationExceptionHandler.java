package com.anglikana.backoffice.security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit {@link EntiteIntrouvableException} en 404 sans corps, comme les routes le font déjà
 * pour une entité absente. La réponse est écrite directement (pas de renvoi vers /error).
 */
@RestControllerAdvice
public class AutorisationExceptionHandler {

    @ExceptionHandler(EntiteIntrouvableException.class)
    public ResponseEntity<Void> entiteIntrouvable(EntiteIntrouvableException exception) {
        return ResponseEntity.notFound().build();
    }
}
