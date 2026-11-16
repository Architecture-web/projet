package com.skillquest.gamification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.skillquest.gamification.dto.QuizDtos.AdminQuizView;
import com.skillquest.gamification.dto.QuizDtos.AnswerRequest;
import com.skillquest.gamification.dto.QuizDtos.AttemptResult;
import com.skillquest.gamification.dto.QuizDtos.CertificationProgress;
import com.skillquest.gamification.dto.QuizDtos.OptionUpsert;
import com.skillquest.gamification.dto.QuizDtos.QuestionUpsert;
import com.skillquest.gamification.dto.QuizDtos.QuizUpsert;
import com.skillquest.gamification.exception.BadRequestException;
import com.skillquest.gamification.support.IntegrationTestBase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class QuizFlowTest extends IntegrationTestBase {

    @Autowired
    private AdminService admin;
    @Autowired
    private QuizService quizService;
    @Autowired
    private CertificateService certificateService;
    @Autowired
    private GamificationService gamification;

    /** Quiz de 2 questions (A est la bonne reponse). */
    private AdminQuizView quiz(String courseId, int xpReward) {
        return admin.createQuiz(new QuizUpsert(courseId, "Quiz " + courseId, "desc", xpReward, List.of(
                new QuestionUpsert("Q1 ?", List.of(new OptionUpsert("A", true), new OptionUpsert("B", false))),
                new QuestionUpsert("Q2 ?", List.of(new OptionUpsert("A", true), new OptionUpsert("B", false), new OptionUpsert("C", false))))));
    }

    /** Reponses : les {@code correctCount} premieres questions sont justes, les autres fausses. */
    private List<AnswerRequest> answers(AdminQuizView quiz, int correctCount) {
        return java.util.stream.IntStream.range(0, quiz.questions().size()).mapToObj(i -> {
            var question = quiz.questions().get(i);
            boolean wantCorrect = i < correctCount;
            var option = question.options().stream().filter(o -> o.correct() == wantCorrect).findFirst().orElseThrow();
            return new AnswerRequest(question.id(), option.id());
        }).toList();
    }

    @Test
    void xpRewardsProgressionOnlyAndReplayCannotFarm() {
        UUID user = UUID.randomUUID();
        AdminQuizView q = quiz("c1", 40);

        AttemptResult half = quizService.submit(user, q.id(), answers(q, 1));
        assertThat(half.scorePercent()).isEqualTo(50);
        assertThat(half.xpGained()).isEqualTo(20);
        assertThat(half.passed()).isFalse();

        AttemptResult full = quizService.submit(user, q.id(), answers(q, 2));
        assertThat(full.scorePercent()).isEqualTo(100);
        assertThat(full.xpGained()).isEqualTo(20); // seulement le gain de 50 -> 100
        assertThat(full.passed()).isTrue();

        AttemptResult replay = quizService.submit(user, q.id(), answers(q, 2));
        assertThat(replay.xpGained()).isZero();
        assertThat(gamification.getProfile(user).totalXP()).isEqualTo(40);
    }

    @Test
    void certificateNeedsCompletedCourseAndAverageOfEightyPercent() {
        UUID user = UUID.randomUUID();
        when(courseClient.fetchCourseTitle("c1")).thenReturn("Cours 1");
        AdminQuizView a = quiz("c1", 20);
        AdminQuizView b = quiz("c1", 20);

        // cours termine mais aucun quiz fait : moyenne 0 % -> pas de certificat
        certificateService.onCourseCompleted(user, "c1", "Ada Lovelace");
        CertificationProgress p = certificateService.progress(user, "c1");
        assertThat(p.courseCompleted()).isTrue();
        assertThat(p.eligible()).isFalse();
        assertThat(p.certificateIssued()).isFalse();

        // A = 100 %, B = 50 % -> moyenne 75 % : toujours rien
        quizService.submit(user, a.id(), answers(a, 2));
        AttemptResult second = quizService.submit(user, b.id(), answers(b, 1));
        assertThat(second.certification().averageBestScore()).isEqualTo(75);
        assertThat(second.certification().certificateIssued()).isFalse();

        // B rejoue a 100 % -> moyenne 100 % : le certificat est delivre automatiquement
        AttemptResult third = quizService.submit(user, b.id(), answers(b, 2));
        assertThat(third.certification().certificateIssued()).isTrue();
        assertThat(third.certification().certificateId()).isNotNull();
        assertThat(certificateService.getCertifiedProfile(user).certificates()).hasSize(1);
    }

    @Test
    void averageExactlyEightyPercentIsEnough() {
        UUID user = UUID.randomUUID();
        when(courseClient.fetchCourseTitle("c2")).thenReturn("Cours 2");
        // 5 questions : 4 justes = 80 %
        AdminQuizView q = admin.createQuiz(new QuizUpsert("c2", "Quiz 5Q", null, 10, java.util.stream.IntStream.rangeClosed(1, 5)
                .mapToObj(i -> new QuestionUpsert("Q" + i, List.of(new OptionUpsert("A", true), new OptionUpsert("B", false)))).toList()));
        certificateService.onCourseCompleted(user, "c2", "Ada");

        AttemptResult result = quizService.submit(user, q.id(), answers(q, 4));

        assertThat(result.scorePercent()).isEqualTo(80);
        assertThat(result.certification().certificateIssued()).isTrue();
    }

    @Test
    void passingQuizzesWithoutCompletingTheCourseGivesNoCertificate() {
        UUID user = UUID.randomUUID();
        AdminQuizView q = quiz("c3", 20);

        AttemptResult result = quizService.submit(user, q.id(), answers(q, 2));

        assertThat(result.scorePercent()).isEqualTo(100);
        assertThat(result.certification().courseCompleted()).isFalse();
        assertThat(result.certification().eligible()).isFalse();
        assertThat(result.certification().certificateIssued()).isFalse();
    }

    @Test
    void courseCompletedLaterStillIssuesCertificateWhenQuizzesAlreadyPassed() {
        UUID user = UUID.randomUUID();
        when(courseClient.fetchCourseTitle("c4")).thenReturn("Cours 4");
        AdminQuizView q = quiz("c4", 20);
        quizService.submit(user, q.id(), answers(q, 2));

        certificateService.onCourseCompleted(user, "c4", "Ada Lovelace");

        assertThat(certificateService.progress(user, "c4").certificateIssued()).isTrue();
    }

    @Test
    void courseServiceOutageDoesNotLoseTheQuizAttempt() {
        UUID user = UUID.randomUUID();
        when(courseClient.fetchCourseTitle("c5"))
                .thenThrow(new com.skillquest.gamification.exception.CourseServiceException("down"));
        AdminQuizView q = quiz("c5", 20);
        // cours termine d'abord : moyenne 0 %, donc pas encore de certificat a emettre (aucune erreur)
        certificateService.onCourseCompleted(user, "c5", "Ada");

        AttemptResult result = quizService.submit(user, q.id(), answers(q, 2));

        assertThat(result.xpGained()).isEqualTo(20);
        assertThat(result.certification().eligible()).isTrue();
        assertThat(result.certification().certificateIssued()).isFalse();
    }

    @Test
    void answerForAnotherQuizQuestionIsRejected() {
        UUID user = UUID.randomUUID();
        AdminQuizView q = quiz("c6", 20);

        assertThatThrownBy(() -> quizService.submit(user, q.id(), List.of(new AnswerRequest(UUID.randomUUID(), UUID.randomUUID()))))
                .isInstanceOf(BadRequestException.class);
    }
}
