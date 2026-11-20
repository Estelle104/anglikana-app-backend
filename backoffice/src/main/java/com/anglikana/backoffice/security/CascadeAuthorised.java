package com.anglikana.backoffice.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Étend les {@link NeedAuthorisation} de la méthode aux éléments filles : le responsable d'un
 * ancêtre de l'entité ciblée (son diocèse, sa région...) est accepté au même titre que le
 * responsable de l'entité elle-même. Sans effet sans {@link NeedAuthorisation}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CascadeAuthorised {
}
