package com.anglikana.backoffice.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exécute un test avec un {@link UtilisateurConnecte} authentifié (à la place de
 * {@code @WithMockUser}, dont le principal ne porte ni clergé ni niveau).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithUtilisateurConnecteFactory.class)
public @interface WithUtilisateurConnecte {

    String nomUtilisateur() default "responsable";

    /** Clergé lié au compte ; une valeur négative signifie « sans clergé ». */
    int clergeId() default 1;

    /** 5 = Évêque ... 1 = Katekista, 0 sans rôle. */
    int niveau() default 5;
}
