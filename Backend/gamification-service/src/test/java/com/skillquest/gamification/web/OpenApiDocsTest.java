package com.skillquest.gamification.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillquest.gamification.support.IntegrationTestBase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Seance 5 : la documentation Swagger/OpenAPI est publique et presente les groupes Gamification, Certifications et Quiz
 * (springdoc.paths-to-match). Le groupe Administration reste actif mais est masque de la doc.
 */
@AutoConfigureMockMvc
class OpenApiDocsTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void apiDocsArePublicAndHideTheAdminGroup() throws Exception {
        String body = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title", containsString("Gamification")))
                .andExpect(jsonPath("$.tags[*].name", hasItems("Gamification", "Certifications", "Quiz (etudiant)")))
                .andExpect(jsonPath("$.tags[*].name", not(hasItem("Administration"))))
                .andReturn().getResponse().getContentAsString();
        JsonNode root = mapper.readTree(body);
        root.get("paths").fieldNames().forEachRemaining(path ->
                org.assertj.core.api.Assertions.assertThat(path).matches("/api/(gamification|certifications|quizzes).*"));
        org.assertj.core.api.Assertions.assertThat(root.get("paths").size()).isEqualTo(12);
    }

    @Test
    void gamificationRoutesDocumentTheirResponseCodes() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/gamification/profiles'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/gamification/profiles'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/gamification/profiles/{userId}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/gamification/leaderboard'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/gamification/badges/user/{userId}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/certifications/{id}/verify'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/certifications/{id}/pdf'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/quizzes'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/quizzes/{id}/attempts'].post.responses['400']").exists());
    }

    @Test
    void swaggerUiIsReachableWithoutLogin() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
