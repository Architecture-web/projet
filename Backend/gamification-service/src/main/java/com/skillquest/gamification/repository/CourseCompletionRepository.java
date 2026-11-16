package com.skillquest.gamification.repository;

import java.util.Optional;
import java.util.UUID;

import com.skillquest.gamification.domain.CourseCompletion;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseCompletionRepository extends JpaRepository<CourseCompletion, UUID> {

    Optional<CourseCompletion> findByUserIdAndCourseId(UUID userId, String courseId);
}
