package com.skillquest.gamification.repository;

import java.util.List;
import java.util.UUID;

import com.skillquest.gamification.domain.Quiz;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    List<Quiz> findByCourseIdOrderByCreatedAtAsc(String courseId);

    List<Quiz> findAllByOrderByCourseIdAscCreatedAtAsc();
}
