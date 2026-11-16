package com.skillquest.gamification.repository;

import java.util.List;
import java.util.UUID;

import com.skillquest.gamification.domain.QuizAttempt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    @Query("select max(a.scorePercent) from QuizAttempt a where a.userId = :userId and a.quiz.id = :quizId")
    Integer findBestScore(@Param("userId") UUID userId, @Param("quizId") UUID quizId);

    /** Meilleur score par quiz (pour un cours) : lignes {quizId, bestScore}. */
    @Query("select a.quiz.id, max(a.scorePercent) from QuizAttempt a "
            + "where a.userId = :userId and a.quiz.courseId = :courseId group by a.quiz.id")
    List<Object[]> findBestScoresByCourse(@Param("userId") UUID userId, @Param("courseId") String courseId);

    /** Meilleur score par quiz (tous les cours) : lignes {quizId, bestScore}. */
    @Query("select a.quiz.id, max(a.scorePercent) from QuizAttempt a where a.userId = :userId group by a.quiz.id")
    List<Object[]> findBestScoresByUser(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from QuizAttempt a where a.quiz.id = :quizId")
    void deleteByQuizId(@Param("quizId") UUID quizId);
}
