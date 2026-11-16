package com.skillquest.gamification.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.skillquest.gamification.exception.CourseServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Appel synchrone REST vers course-service : {@code GET /api/courses/{courseId}}. */
@Component
public class CourseClient {

    private static final Logger log = LoggerFactory.getLogger(CourseClient.class);

    private final RestClient restClient;

    public CourseClient(RestClient courseServiceRestClient) {
        this.restClient = courseServiceRestClient;
    }

    /** Retourne le titre exact du cours, à imprimer sur le certificat. */
    public String fetchCourseTitle(String courseId) {
        try {
            CourseResponse course = restClient.get()
                    .uri("/api/courses/{courseId}", courseId)
                    .retrieve()
                    .body(CourseResponse.class);
            if (course == null || course.title() == null || course.title().isBlank()) {
                throw new CourseServiceException("course-service returned no title for course " + courseId);
            }
            return course.title();
        } catch (RestClientException ex) {
            log.warn("Call to course-service failed for course {}: {}", courseId, ex.getMessage());
            throw new CourseServiceException("Unable to fetch course " + courseId + " from course-service", ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CourseResponse(String title) {
    }
}
