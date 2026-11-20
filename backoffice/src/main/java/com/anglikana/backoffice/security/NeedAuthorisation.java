package com.anglikana.backoffice.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Le clergé de l'utilisateur connecté doit être le responsable ({@code responsable_id}) de
 * l'entité ciblée. Avec {@link CascadeAuthorised}, le responsable d'un ancêtre de l'entité est
 * aussi accepté.
 *
 * <p>L'entité ciblée est donnée par {@link #entity()} (fixe) ou par {@link #entityExpression()}
 * (SpEL évalué sur les paramètres de la méthode, valeur = code : {@code "region"}...). Exactement
 * l'un des deux doit être renseigné. {@link #id()} est une expression SpEL : {@code "#id"},
 * {@code "#dto.paroisseId"}...</p>
 *
 * <p>Réponses : 403 si l'utilisateur est anonyme, si l'identifiant vaut null, si le code d'entité
 * est inconnu ou si le clergé n'est pas responsable ; 404 si l'entité n'existe pas (ou est
 * supprimée, etat = -1). Répétable : toutes les annotations doivent être satisfaites.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(NeedAuthorisations.class)
public @interface NeedAuthorisation {

    /** Entité ciblée (un seul élément) ; laisser vide si {@link #entityExpression()} est utilisé. */
    EntiteProtegee[] entity() default {};

    /** Expression SpEL donnant le code de l'entité ciblée, par exemple {@code "#niveau"}. */
    String entityExpression() default "";

    /** Expression SpEL donnant l'identifiant de l'entité ciblée. */
    String id();
}
