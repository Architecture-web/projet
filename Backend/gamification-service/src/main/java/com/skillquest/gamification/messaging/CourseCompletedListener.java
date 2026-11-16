package com.skillquest.gamification.messaging;

import com.skillquest.gamification.service.CertificateService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consomme {@code CourseCompletedEvent} : génère le certificat signé de fin de parcours. */
@Component
public class CourseCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(CourseCompletedListener.class);

    private final CertificateService certificateService;

    public CourseCompletedListener(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @RabbitListener(queues = "${gamification.messaging.course-completed-queue}")
    public void onCourseCompleted(CourseCompletedEvent event) {
        if (event == null || event.userId() == null || event.courseId() == null || event.courseId().isBlank()) {
            throw new AmqpRejectAndDontRequeueException("CourseCompletedEvent without userId/courseId");
        }
        certificateService.onCourseCompleted(event.userId(), event.courseId(), event.studentFullName());
        log.info("CourseCompletedEvent user={} course={} processed", event.userId(), event.courseId());
    }
}
