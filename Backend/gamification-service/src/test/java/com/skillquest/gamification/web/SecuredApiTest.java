package com.skillquest.gamification.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillquest.gamification.domain.Certificate;
import com.skillquest.gamification.service.CertificateService;
import com.skillquest.gamification.service.GamificationService;
import com.skillquest.gamification.support.IntegrationTestBase;
import com.skillquest.gamification.support.TestJwt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Securite (vrais JWT signes comme ceux d'auth-service) et CRUD d'administration. */
@AutoConfigureMockMvc
class SecuredApiTest extends IntegrationTestBase {

    private static final String QUIZ_JSON = """
            {"courseId":"course-1","title":"Quiz Java","description":"Bases","xpReward":40,
             "questions":[
               {"text":"2+2 ?","options":[{"text":"4","correct":true},{"text":"5","correct":false}]},
               {"text":"Capitale de la Tunisie ?","options":[{"text":"Tunis","correct":true},{"text":"Sfax","correct":false},{"text":"Sousse","correct":false}]}
             ]}""";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private GamificationService gamification;
    @Autowired
    private CertificateService certificateService;

    private static String bearer(String... roles) {
        return "Bearer " + TestJwt.token(UUID.randomUUID(), roles);
    }

    private JsonNode createQuiz(String admin) throws Exception {
        String body = mvc.perform(post("/api/admin/quizzes").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(QUIZ_JSON))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    // ---------------------------------------------------------------- securite

    @Test
    void adminApiRejectsAnonymousStudentsAndBadTokens() throws Exception {
        mvc.perform(get("/api/admin/quizzes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/quizzes").header("Authorization", bearer("ROLE_STUDENT"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/quizzes").header("Authorization", bearer("ROLE_RECRUITER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/quizzes").header("Authorization", bearer("ROLE_ADMIN"))).andExpect(status().isOk());

        String forged = "Bearer " + TestJwt.token(UUID.randomUUID(), "x".repeat(80), Instant.now().plusSeconds(600), "ROLE_ADMIN");
        mvc.perform(get("/api/admin/quizzes").header("Authorization", forged)).andExpect(status().isUnauthorized());
        String expired = "Bearer " + TestJwt.token(UUID.randomUUID(), TestJwt.DEFAULT_SECRET, Instant.now().minusSeconds(60), "ROLE_ADMIN");
        mvc.perform(get("/api/admin/quizzes").header("Authorization", expired)).andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpointsStayOpenForOtherServices() throws Exception {
        mvc.perform(get("/api/gamification/leaderboard")).andExpect(status().isOk());
        mvc.perform(get("/api/gamification/badges")).andExpect(status().isOk());
        mvc.perform(post("/api/gamification/profiles").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"" + UUID.randomUUID() + "\"}")).andExpect(status().isCreated());
    }

    // ---------------------------------------------------------------- jeu de quiz

    @Test
    void studentPlaysAQuizAndTheAnswersAreNeverLeaked() throws Exception {
        String admin = bearer("ROLE_ADMIN");
        JsonNode created = createQuiz(admin);
        UUID student = UUID.randomUUID();
        String studentAuth = "Bearer " + TestJwt.token(student, "ROLE_STUDENT");
        String quizId = created.get("id").asText();

        mvc.perform(get("/api/quizzes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/quizzes").header("Authorization", studentAuth).param("courseId", "course-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].questionCount", is(2))).andExpect(jsonPath("$[0].bestScore").doesNotExist());

        String detail = mvc.perform(get("/api/quizzes/" + quizId).header("Authorization", studentAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].options[0].correct").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(detail).doesNotContain("correct");

        // reponses : la 1re question juste, la 2e fausse
        StringBuilder answers = new StringBuilder("{\"answers\":[");
        for (int q = 0; q < 2; q++) {
            JsonNode adminQ = created.get("questions").get(q);
            boolean wantCorrect = q == 0;
            for (JsonNode o : adminQ.get("options")) {
                if (o.get("correct").asBoolean() == wantCorrect) {
                    answers.append(q == 0 ? "" : ",").append("{\"questionId\":\"").append(adminQ.get("id").asText())
                            .append("\",\"optionId\":\"").append(o.get("id").asText()).append("\"}");
                    break;
                }
            }
        }
        answers.append("]}");

        mvc.perform(post("/api/quizzes/" + quizId + "/attempts").header("Authorization", studentAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(answers.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scorePercent", is(50)))
                .andExpect(jsonPath("$.passed", is(false)))
                .andExpect(jsonPath("$.xpGained", is(20)))
                .andExpect(jsonPath("$.review", hasSize(2)))
                .andExpect(jsonPath("$.certification.requiredAverage", is(80)));

        // l'XP est bien visible sur le profil du joueur (utilise par le dashboard)
        mvc.perform(get("/api/gamification/profiles/" + student)).andExpect(jsonPath("$.totalXP", is(20)));
        mvc.perform(get("/api/quizzes").header("Authorization", studentAuth))
                .andExpect(jsonPath("$[0].bestScore", is(50)));
    }

    // ---------------------------------------------------------------- CRUD admin : quiz

    @Test
    void adminCanCreateUpdateAndDeleteQuizzes() throws Exception {
        String admin = bearer("ROLE_ADMIN");
        JsonNode created = createQuiz(admin);
        String id = created.get("id").asText();
        mvc.perform(get("/api/admin/quizzes/" + id).header("Authorization", admin))
                .andExpect(jsonPath("$.questions[0].options[0].correct", is(true)));

        // validation : une question sans bonne reponse -> 400
        String noCorrect = QUIZ_JSON.replace("\"correct\":true", "\"correct\":false");
        mvc.perform(post("/api/admin/quizzes").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(noCorrect)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/quizzes").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":\"\",\"title\":\"\",\"questions\":[]}"))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/admin/quizzes/" + id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(QUIZ_JSON.replace("Quiz Java", "Quiz Java v2")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title", is("Quiz Java v2")))
                .andExpect(jsonPath("$.questions", hasSize(2)));

        mvc.perform(delete("/api/admin/quizzes/" + id).header("Authorization", admin)).andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/quizzes/" + id).header("Authorization", admin)).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- CRUD admin : badges

    @Test
    void adminCanManageBadgesButNotTheSystemBadge() throws Exception {
        String admin = bearer("ROLE_ADMIN");
        String body = "{\"codeName\":\"QUIZ_MASTER\",\"title\":\"Maitre des quiz\",\"description\":\"d\",\"iconUrl\":\"/i.svg\",\"requiredXP\":300}";
        String created = mvc.perform(post("/api/admin/badges").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).get("id").asLong();

        mvc.perform(post("/api/admin/badges").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        mvc.perform(post("/api/admin/badges").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(body.replace("QUIZ_MASTER", "bad code"))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/badges/" + id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("300", "350")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requiredXP", is(350)));

        // un joueur qui atteint le palier debloque ce badge, puis la suppression le retire aussi aux joueurs
        UUID player = UUID.randomUUID();
        gamification.addXP(player, 360);
        mvc.perform(get("/api/gamification/badges/user/" + player)).andExpect(jsonPath("$[?(@.codeName=='QUIZ_MASTER')]", hasSize(1)));
        mvc.perform(delete("/api/admin/badges/" + id).header("Authorization", admin)).andExpect(status().isNoContent());
        mvc.perform(get("/api/gamification/badges/user/" + player)).andExpect(jsonPath("$[?(@.codeName=='QUIZ_MASTER')]", hasSize(0)));

        long systemId = mapper.readTree(mvc.perform(get("/api/gamification/badges")).andReturn().getResponse().getContentAsString())
                .findParents("codeName").stream().filter(n -> "COURSE_GRADUATE".equals(n.get("codeName").asText()))
                .findFirst().orElseThrow().get("id").asLong();
        mvc.perform(delete("/api/admin/badges/" + systemId).header("Authorization", admin)).andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- admin : joueurs et certificats

    @Test
    void adminCanListPlayersAndAdjustTheirXp() throws Exception {
        String admin = bearer("ROLE_ADMIN");
        UUID player = UUID.randomUUID();
        gamification.addXP(player, 120);

        mvc.perform(get("/api/admin/players").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].userId", is(player.toString())))
                .andExpect(jsonPath("$[0].totalXP", is(120))).andExpect(jsonPath("$[0].badgeCount", is(1)));

        mvc.perform(post("/api/admin/players/" + player + "/xp").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":-50,\"reason\":\"correction\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalXP", is(70))).andExpect(jsonPath("$.currentLevel", is(1)));
        mvc.perform(post("/api/admin/players/" + player + "/xp").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":-9999}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalXP", is(0)));
        mvc.perform(post("/api/admin/players/" + player + "/xp").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":0}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/players/" + UUID.randomUUID() + "/xp").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":10}")).andExpect(status().isNotFound());
    }

    @Test
    void revokedCertificateIsNoLongerValidNorListedForRecruiters() throws Exception {
        String admin = bearer("ROLE_ADMIN");
        UUID student = UUID.randomUUID();
        when(courseClient.fetchCourseTitle("course-9")).thenReturn("Cours 9");
        Certificate cert = certificateService.issueCertificate(student, "course-9", "Ada Lovelace");

        mvc.perform(get("/api/admin/certificates").header("Authorization", admin))
                .andExpect(jsonPath("$[0].id", is(cert.getId().toString()))).andExpect(jsonPath("$[0].revokedAt").doesNotExist());
        mvc.perform(get("/api/certifications/" + cert.getId() + "/verify")).andExpect(jsonPath("$.valid", is(true)));

        mvc.perform(post("/api/admin/certificates/" + cert.getId() + "/revoke").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.revokedAt").exists());

        mvc.perform(get("/api/certifications/" + cert.getId() + "/verify")).andExpect(jsonPath("$.valid", is(false)));
        mvc.perform(get("/api/certifications/" + cert.getId() + "/pdf")).andExpect(status().isNotFound());
        mvc.perform(get("/api/certifications/" + student)).andExpect(jsonPath("$.certificates", hasSize(0)));
        // la revocation est definitive : un nouveau quiz reussi ne re-emet pas le certificat
        org.assertj.core.api.Assertions.assertThat(certificateService.progress(student, "course-9").certificateIssued()).isFalse();
    }
}
