package com.skillquest.gamification.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import com.skillquest.gamification.dto.ApiDtos.CertifiedProfileResponse;
import com.skillquest.gamification.exception.CourseServiceException;
import com.skillquest.gamification.service.CertificateService;
import com.skillquest.gamification.service.GamificationService;
import com.skillquest.gamification.support.IntegrationTestBase;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;

class ListenersTest extends IntegrationTestBase {

    @Autowired
    private CodeSolutionPassedListener codeSolutionListener;
    @Autowired
    private CourseCompletedListener courseCompletedListener;
    @Autowired
    private GamificationService gamificationService;
    @Autowired
    private CertificateService certificateService;
    @Autowired
    private MessageConverter messageConverter;

    @Test
    void codeSolutionPassedAddsFiftyXp() {
        UUID user = UUID.randomUUID();

        codeSolutionListener.onCodeSolutionPassed(
                new CodeSolutionPassedEvent(UUID.randomUUID(), user, "challenge-7", Instant.now()));

        var profile = gamificationService.getProfile(user);
        assertThat(profile.totalXP()).isEqualTo(50);
        assertThat(gamificationService.getUserBadges(user)).extracting("codeName").containsExactly("FIRST_STEPS");
    }

    @Test
    void duplicateDeliveryOfSameEventIsIgnored() {
        UUID user = UUID.randomUUID();
        var event = new CodeSolutionPassedEvent(UUID.randomUUID(), user, "challenge-7", Instant.now());

        codeSolutionListener.onCodeSolutionPassed(event);
        codeSolutionListener.onCodeSolutionPassed(event);

        assertThat(gamificationService.getProfile(user).totalXP()).isEqualTo(50);
    }

    @Test
    void invalidCodeSolutionEventIsRejectedWithoutRequeue() {
        assertThatThrownBy(() -> codeSolutionListener.onCodeSolutionPassed(
                new CodeSolutionPassedEvent(null, null, "c", Instant.now())))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void courseCompletedIssuesCertificateWithTitleFromCourseService() {
        UUID user = UUID.randomUUID();
        when(courseClient.fetchCourseTitle("course-42")).thenReturn("Spring Boot Avancé");

        courseCompletedListener.onCourseCompleted(
                new CourseCompletedEvent(UUID.randomUUID(), user, "course-42", "Ada Lovelace", Instant.now()));

        CertifiedProfileResponse profile = certificateService.getCertifiedProfile(user);
        assertThat(profile.certificates()).hasSize(1);
        var cert = profile.certificates().get(0);
        assertThat(cert.courseTitle()).isEqualTo("Spring Boot Avancé");
        assertThat(cert.studentFullName()).isEqualTo("Ada Lovelace");
        assertThat(cert.certificateHash()).hasSize(64);
        assertThat(profile.badges()).extracting("codeName").contains("COURSE_GRADUATE");
        assertThat(certificateService.verify(cert.id(), cert.certificateHash()).valid()).isTrue();
    }

    @Test
    void courseCompletedTwiceIssuesOnlyOneCertificate() {
        UUID user = UUID.randomUUID();
        when(courseClient.fetchCourseTitle("course-42")).thenReturn("Spring Boot Avancé");
        var event = new CourseCompletedEvent(UUID.randomUUID(), user, "course-42", "Ada Lovelace", Instant.now());

        courseCompletedListener.onCourseCompleted(event);
        courseCompletedListener.onCourseCompleted(event);

        assertThat(certificateService.getCertifiedProfile(user).certificates()).hasSize(1);
        verify(courseClient, times(1)).fetchCourseTitle("course-42");
    }

    @Test
    void courseServiceFailureLeavesNoCertificate() {
        UUID user = UUID.randomUUID();
        when(courseClient.fetchCourseTitle("course-42")).thenThrow(new CourseServiceException("down"));

        assertThatThrownBy(() -> courseCompletedListener.onCourseCompleted(
                new CourseCompletedEvent(null, user, "course-42", "Ada", Instant.now())))
                .isInstanceOf(CourseServiceException.class);

        // Le profil n'existe pas : rien n'a été écrit.
        assertThatThrownBy(() -> certificateService.getCertifiedProfile(user))
                .isInstanceOf(com.skillquest.gamification.exception.ResourceNotFoundException.class);
    }

    @Test
    void jsonFromOtherServicesIsDeserializedIntoLocalEventTypes() {
        UUID user = UUID.randomUUID();
        String json = """
                {"userId":"%s","courseId":"c-1","studentFullName":"Ada","unknownField":123}
                """.formatted(user);
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        // Simule l'en-tête d'un producteur dont la classe Java n'existe pas dans ce service.
        props.setHeader("__TypeId__", "com.other.service.CourseCompletedEvent");
        props.setInferredArgumentType(CourseCompletedEvent.class);
        Message message = MessageBuilder.withBody(json.getBytes()).andProperties(props).build();

        Object converted = messageConverter.fromMessage(message);

        assertThat(converted).isInstanceOf(CourseCompletedEvent.class);
        assertThat(((CourseCompletedEvent) converted).userId()).isEqualTo(user);
        assertThat(((CourseCompletedEvent) converted).courseId()).isEqualTo("c-1");
    }
}
