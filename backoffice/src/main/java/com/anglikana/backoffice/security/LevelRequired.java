package com.anglikana.backoffice.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Niveau minimal du rôle de l'utilisateur connecté ({@code role.niveau} de son clergé) :
 * 5 = Évêque, 4 = Archidiacre, 3 = Doyen, 2 = Presbytère, 1 = Katekista.
 *
 * <p>Refus (403) si l'utilisateur est anonyme ou si son niveau est inférieur à {@link #value()}.
 * Se combine avec {@link NeedAuthorisation} : toutes les conditions doivent être remplies.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LevelRequired {

    int value();
}
