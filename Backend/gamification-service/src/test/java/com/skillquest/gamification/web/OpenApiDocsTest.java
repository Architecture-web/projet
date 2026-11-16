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
 * Seance 5 : la documentation Swagger/OpenAPI est publique et ne presente que le groupe Gamification
 * (springdoc.paths-to-match). Les autres routes restent actives, elles sont seulement masquees de la doc.
 */
@AutoConfigureMockMvc
class OpenApiDocsTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void apiDocsArePublicAndListOnlyTheGamificationGroup() throws Exception {
        String body = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title", containsString("Gamification")))
                .andExpect(jsonPath("$.tags[*].name", hasItem("Gamification")))
                .andExpect(jsonPath("$.tags[*].name", not(hasItems("Administration", "Quiz (etudiant)", "Certifications"))))
                .andReturn().getResponse().getContentAsString();
        JsonNode root = mapper.readTree(body);
        root.get("paths").fieldNames().forEachRemaining(path ->
                org.assertj.core.api.Assertions.assertThat(path).startsWith("/api/gamification"));
        org.assertj.core.api.Assertions.assertThat(root.get("paths").size()).isEqualTo(5);
    }

    @Test
    void gamificationRoutesDocumentTheirResponseCodes() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/gamification/profiles'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/gamification/profiles'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/gamification/profiles/{userId}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/gamification/leaderboard'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/gamification/badges/user/{userId}'].get.responses['404']").exists());
    }

    @Test
    void swaggerUiIsReachableWithoutLogin() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
