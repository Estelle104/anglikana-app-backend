package com.anglikana.backoffice.config;

import com.anglikana.backoffice.security.AutorisationService;
import com.anglikana.backoffice.security.CascadeAuthorised;
import com.anglikana.backoffice.security.LevelRequired;
import com.anglikana.backoffice.security.NeedAuthorisation;
import com.anglikana.backoffice.security.NeedAuthorisations;
import com.anglikana.backoffice.security.PerimetreAuthorizationManager;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.security.authorization.method.AuthorizationInterceptorsOrder;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;

/**
 * Contrôle d'accès par niveau et par périmètre : toute méthode portant {@link LevelRequired},
 * {@link NeedAuthorisation} ou {@link CascadeAuthorised} passe par
 * {@link PerimetreAuthorizationManager} avant son exécution.
 */
@Configuration
public class MethodSecurityConfig {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static Advisor perimetreAuthorizationAdvisor(ObjectProvider<AutorisationService> autorisationService) {
        ComposablePointcut pointcut = new ComposablePointcut(AnnotationMatchingPointcut.forMethodAnnotation(LevelRequired.class))
                .union(AnnotationMatchingPointcut.forMethodAnnotation(NeedAuthorisation.class))
                .union(AnnotationMatchingPointcut.forMethodAnnotation(NeedAuthorisations.class))
                .union(AnnotationMatchingPointcut.forMethodAnnotation(CascadeAuthorised.class));

        AuthorizationManagerBeforeMethodInterceptor interceptor =
                new AuthorizationManagerBeforeMethodInterceptor(pointcut, new PerimetreAuthorizationManager(autorisationService));
        interceptor.setOrder(AuthorizationInterceptorsOrder.PRE_AUTHORIZE.getOrder() + 1);
        return interceptor;
    }
}
