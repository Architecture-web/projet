package com.skillquest.gamification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.skillquest.gamification.domain.Certificate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    Optional<Certificate> findByUserIdAndCourseId(UUID userId, String courseId);

    List<Certificate> findAllByUserIdOrderByIssuedAtDesc(UUID userId);
}
