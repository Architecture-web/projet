package com.skillquest.gamification.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.skillquest.gamification.domain.Certificate;
import com.skillquest.gamification.service.CertificateService;
import com.skillquest.gamification.service.GamificationService;
import com.skillquest.gamification.support.IntegrationTestBase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ApiTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private GamificationService gamificationService;
    @Autowired
    private CertificateService certificateService;

    @Test
    void createProfileReturns201ThenConflict409() throws Exception {
        UUID user = UUID.randomUUID();
        String body = "{\"userId\":\"" + user + "\"}";

        mvc.perform(post("/api/gamification/profiles").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/gamification/profiles/" + user))
                .andExpect(jsonPath("$.userId", is(user.toString())))
                .andExpect(jsonPath("$.totalXP", is(0)))
                .andExpect(jsonPath("$.currentLevel", is(1)));

        mvc.perform(post("/api/gamification/profiles").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void createProfileWithoutUserIdIs400() throws Exception {
        mvc.perform(post("/api/gamification/profiles").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void leaderboardReturnsRankedPlayersAndValidatesLimit() throws Exception {
        UUID top = UUID.randomUUID();
        gamificationService.addXP(UUID.randomUUID(), 10);
        gamificationService.addXP(top, 120);

        mvc.perform(get("/api/gamification/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId", is(top.toString())))
                .andExpect(jsonPath("$[0].rank", is(1)))
                .andExpect(jsonPath("$[0].currentLevel", is(2)));

        mvc.perform(get("/api/gamification/leaderboard").param("limit", "1"))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/gamification/leaderboard").param("limit", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/gamification/leaderboard").param("limit", "51")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/gamification/leaderboard").param("limit", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    void userBadgesEndpoint() throws Exception {
        UUID user = UUID.randomUUID();
        gamificationService.addXP(user, 50);

        mvc.perform(get("/api/gamification/badges/user/{id}", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codeName", is("FIRST_STEPS")));

        mvc.perform(get("/api/gamification/badges/user/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/gamification/badges/user/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void certifiedProfileVerificationAndPdf() throws Exception {
        UUID user = UUID.randomUUID();
        when(courseClient.fetchCourseTitle("course-1")).thenReturn("Microservices avec Spring");
        Certificate cert = certificateService.issueCertificate(user, "course-1", "Ada Lovelace");

        mvc.perform(get("/api/certifications/{userId}", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificates", hasSize(1)))
                .andExpect(jsonPath("$.certificates[0].courseTitle", is("Microservices avec Spring")))
                .andExpect(jsonPath("$.badges[0].codeName", is("COURSE_GRADUATE")));

        mvc.perform(get("/api/certifications/{id}/verify", cert.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.studentFullName", is("Ada Lovelace")));

        mvc.perform(get("/api/certifications/{id}/verify", cert.getId()).param("hash", cert.getCertificateHash()))
                .andExpect(jsonPath("$.valid", is(true)));

        mvc.perform(get("/api/certifications/{id}/verify", cert.getId()).param("hash", "0".repeat(64)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)));

        mvc.perform(get("/api/certifications/{id}/verify", UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/certifications/{userId}", UUID.randomUUID())).andExpect(status().isNotFound());

        byte[] pdf = mvc.perform(get("/api/certifications/{id}/pdf", cert.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        org.assertj.core.api.Assertions.assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }
}
