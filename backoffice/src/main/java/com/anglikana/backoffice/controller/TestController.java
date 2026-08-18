package com.anglikana.backoffice.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/protected")
    public String routeProtegee(Authentication authentication) {
        return "Accès autorisé pour : " + authentication.getName()
                + " | rôles : " + authentication.getAuthorities();
    }

    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public String routeAdminUniquement(Authentication authentication) {
        return "Bienvenue admin : " + authentication.getName();
    }
}