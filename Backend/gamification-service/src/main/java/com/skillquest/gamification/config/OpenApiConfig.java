package com.skillquest.gamification.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation OpenAPI (Swagger UI : /swagger-ui.html, JSON : /v3/api-docs).
 * Le schema "bearerAuth" ajoute le bouton Authorize : coller le JWT obtenu aupres de auth-service
 * (POST /api/auth/login, champ accessToken) pour tester les routes protegees.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI gamificationOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SkillQuest - Gamification & Certification Service")
                        .version("1.0.0")
                        .description("""
                                XP, niveaux, badges, classement, mini-jeu quiz et certificats PDF verifiables.

                                - Routes publiques : profils, classement, badges, verification de certificat.
                                - Routes protegees : /api/quizzes/** (utilisateur connecte) et /api/admin/** (ROLE_ADMIN).
                                - Un certificat est delivre quand le cours est termine ET que la moyenne des quiz est >= 80 %.""")
                        .contact(new Contact().name("Gamification service - SkillQuest")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT emis par auth-service (POST /api/auth/login)")));
    }
}
