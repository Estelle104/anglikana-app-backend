package com.anglikana.backoffice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Description générale de l'API exposée par springdoc (Swagger UI : /swagger-ui.html, spec : /v3/api-docs).
 *
 * <p>Aucune exigence de sécurité globale : les lectures GET sont publiques. Les méthodes protégées
 * déclarent elles-mêmes {@code @SecurityRequirement(name = OpenApiConfig.BEARER_SECURITY_SCHEME)},
 * ce qui affiche le cadenas uniquement là où un jeton est nécessaire.</p>
 */
@Configuration
public class OpenApiConfig {

    /** Nom du schéma de sécurité JWT, référencé par les annotations des contrôleurs. */
    public static final String BEARER_SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI backofficeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Back-office A.C.A")
                        .description("Endpoints REST du back-office de l'Anglican Church Application : hiérarchie "
                                + "ecclésiastique, clergé, ressources, géographie et tableau de bord. "
                                + "Les lectures sont publiques ; les écritures exigent un jeton JWT d'un compte ADMIN "
                                + "(obtenu par POST /api/auth/login, à saisir dans « Authorize »).")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SECURITY_SCHEME, new SecurityScheme()
                                .name(BEARER_SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
