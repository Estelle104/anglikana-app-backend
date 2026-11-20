package com.anglikana.backoffice.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithUtilisateurConnecteFactory implements WithSecurityContextFactory<WithUtilisateurConnecte> {

    @Override
    public SecurityContext createSecurityContext(WithUtilisateurConnecte annotation) {
        Integer clergeId = annotation.clergeId() >= 0 ? annotation.clergeId() : null;
        UtilisateurConnecte utilisateur = new UtilisateurConnecte(annotation.nomUtilisateur(), "", true,
                clergeId, annotation.niveau(), annotation.niveau() > 0 ? "Rôle " + annotation.niveau() : null);

        SecurityContext contexte = SecurityContextHolder.createEmptyContext();
        contexte.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(utilisateur, null, utilisateur.getAuthorities()));
        return contexte;
    }
}
