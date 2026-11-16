package com.skillquest.gamification.exception;

/** course-service injoignable ou réponse inexploitable. */
public class CourseServiceException extends RuntimeException {

    public CourseServiceException(String message) {
        super(message);
    }

    public CourseServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
