package com.skillquest.gamification.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.skillquest.gamification.support.IntegrationTestBase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Seance 5 : la documentation Swagger/OpenAPI est publique, complete et expose l'authentification Bearer. */
@AutoConfigureMockMvc
class OpenApiDocsTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mvc;

    @Test
    void apiDocsArePublicAndDescribeTheServiceAndItsSecurity() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title", containsString("Gamification")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.tags[*].name", hasItems("Gamification", "Certifications", "Quiz (etudiant)", "Administration")));
    }

    @Test
    void responseCodesAreDocumentedAndProtectedRoutesRequireBearer() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/gamification/profiles'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/gamification/profiles'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/gamification/profiles/{userId}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/admin/quizzes'].post.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/admin/quizzes'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/quizzes'].get.security[0].bearerAuth").exists());
    }

    @Test
    void swaggerUiIsReachableWithoutLogin() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
