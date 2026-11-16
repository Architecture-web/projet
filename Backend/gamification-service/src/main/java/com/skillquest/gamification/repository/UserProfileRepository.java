package com.skillquest.gamification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.skillquest.gamification.domain.UserProfileGamification;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProfileRepository extends JpaRepository<UserProfileGamification, UUID> {

    /** Verrou pessimiste (SELECT ... FOR UPDATE) : sérialise les ajouts d'XP concurrents d'un même joueur. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from UserProfileGamification p where p.userId = :userId")
    Optional<UserProfileGamification> findByIdForUpdate(@Param("userId") UUID userId);

    @Query("select p from UserProfileGamification p order by p.totalXP desc, p.updatedAt asc")
    List<UserProfileGamification> findTop(Pageable pageable);

    @Query("select count(p) from UserProfileGamification p where p.totalXP > :xp")
    long countWithMoreXPThan(@Param("xp") int xp);
}
