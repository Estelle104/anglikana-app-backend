package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.config.OpenApiConfig;
import com.anglikana.backoffice.security.LevelRequired;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Test", description = "Routes de démonstration du sprint 1 pour vérifier le jeton et le niveau")
public class TestController {

    @GetMapping("/protected")
    @Operation(summary = "Afficher l'utilisateur courant",
            description = "Renvoie le nom et l'autorité (NIVEAU_<n>) de l'utilisateur authentifié. Exige un jeton valide : la route est publique "
                    + "(règle GET /api/**), mais sans jeton l'appel échoue (Authentication null) et la réponse est 403.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "200", description = "Nom et autorité de l'utilisateur")
    @ApiResponse(responseCode = "403", description = "Jeton absent ou invalide", content = @Content)
    public String routeProtegee(@Parameter(hidden = true) Authentication authentication) {
        return "Accès autorisé pour : " + authentication.getName()
                + " | rôles : " + authentication.getAuthorities();
    }

    @GetMapping("/admin-only")
    @LevelRequired(5)
    @Operation(summary = "Vérifier le niveau 5", description = "Réservé au niveau 5 (Évêque).")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)
    @ApiResponse(responseCode = "200", description = "Message de bienvenue")
    @ApiResponse(responseCode = "403", description = "Jeton absent ou niveau 5 requis", content = @Content)
    public String routeAdminUniquement(@Parameter(hidden = true) Authentication authentication) {
        return "Bienvenue niveau 5 : " + authentication.getName();
    }
}