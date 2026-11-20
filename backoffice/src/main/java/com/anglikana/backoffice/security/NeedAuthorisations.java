package com.anglikana.backoffice.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Conteneur de {@link NeedAuthorisation} répétées sur une même méthode. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NeedAuthorisations {

    NeedAuthorisation[] value();
}
