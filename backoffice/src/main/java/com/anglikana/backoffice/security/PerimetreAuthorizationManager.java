package com.anglikana.backoffice.security;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Applique {@link LevelRequired}, {@link NeedAuthorisation} et {@link CascadeAuthorised} avant
 * l'exécution d'une méthode (enregistré par {@code config/MethodSecurityConfig}).
 *
 * <p>Toutes les conditions de la méthode doivent être remplies ; sinon la décision est négative
 * et Spring Security répond 403. Une entité ciblée introuvable lève
 * {@link EntiteIntrouvableException} (404).</p>
 */
public class PerimetreAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NOMS_PARAMETRES = new DefaultParameterNameDiscoverer();

    private static final AuthorizationDecision ACCORDE = new AuthorizationDecision(true);
    private static final AuthorizationDecision REFUSE = new AuthorizationDecision(false);

    /** Résolu à la première utilisation : le manager est créé avant les repositories JPA. */
    private final ObjectProvider<AutorisationService> autorisationService;

    public PerimetreAuthorizationManager(ObjectProvider<AutorisationService> autorisationService) {
        this.autorisationService = autorisationService;
    }

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication, MethodInvocation invocation) {
        Optional<UtilisateurConnecte> utilisateurOptionnel = AutorisationService.utilisateur(authentication.get());
        if (utilisateurOptionnel.isEmpty()) {
            return REFUSE;
        }
        UtilisateurConnecte utilisateur = utilisateurOptionnel.get();
        AutorisationService service = autorisationService.getObject();
        Method methode = methodeCible(invocation);

        LevelRequired niveauRequis = AnnotatedElementUtils.findMergedAnnotation(methode, LevelRequired.class);
        if (niveauRequis != null && !service.aLeNiveau(utilisateur, niveauRequis.value())) {
            return REFUSE;
        }

        Set<NeedAuthorisation> autorisations =
                AnnotatedElementUtils.findMergedRepeatableAnnotations(methode, NeedAuthorisation.class, NeedAuthorisations.class);
        if (autorisations.isEmpty()) {
            return ACCORDE;
        }

        boolean cascade = AnnotatedElementUtils.hasAnnotation(methode, CascadeAuthorised.class);
        EvaluationContext contexte = new MethodBasedEvaluationContext(null, methode, invocation.getArguments(), NOMS_PARAMETRES);
        for (NeedAuthorisation autorisation : autorisations) {
            Optional<EntiteProtegee> entite = entiteCiblee(autorisation, contexte);
            if (entite.isEmpty()) {
                return REFUSE;
            }
            Integer id = identifiant(autorisation, contexte);
            if (!service.estResponsable(utilisateur, entite.get(), id, cascade)) {
                return REFUSE;
            }
        }
        return ACCORDE;
    }

    private static Method methodeCible(MethodInvocation invocation) {
        Object cible = invocation.getThis();
        return cible != null
                ? AopUtils.getMostSpecificMethod(invocation.getMethod(), AopUtils.getTargetClass(cible))
                : invocation.getMethod();
    }

    private static Optional<EntiteProtegee> entiteCiblee(NeedAuthorisation autorisation, EvaluationContext contexte) {
        boolean fixe = autorisation.entity().length > 0;
        boolean calculee = !autorisation.entityExpression().isBlank();
        if (fixe == calculee || autorisation.entity().length > 1) {
            throw new IllegalStateException("@NeedAuthorisation : renseigner soit un seul « entity », soit « entityExpression »");
        }
        if (fixe) {
            return Optional.of(autorisation.entity()[0]);
        }
        Object code = PARSER.parseExpression(autorisation.entityExpression()).getValue(contexte);
        return code instanceof String texte ? EntiteProtegee.depuisCode(texte) : Optional.empty();
    }

    /** Identifiant évalué ; null s'il est absent ou n'est pas un nombre entier. */
    private static Integer identifiant(NeedAuthorisation autorisation, EvaluationContext contexte) {
        Object valeur = PARSER.parseExpression(autorisation.id()).getValue(contexte);
        return valeur instanceof Integer entier ? entier : null;
    }
}
