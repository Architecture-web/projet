package com.skillquest.gamification.repository;

import java.util.List;
import java.util.UUID;

import com.skillquest.gamification.domain.UserBadge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {

    @Query("select ub from UserBadge ub join fetch ub.badge where ub.userId = :userId order by ub.unlockedAt asc")
    List<UserBadge> findAllWithBadgeByUserId(@Param("userId") UUID userId);

    @Query("select ub.badge.id from UserBadge ub where ub.userId = :userId")
    List<Long> findBadgeIdsByUserId(@Param("userId") UUID userId);

    boolean existsByUserIdAndBadgeId(UUID userId, Long badgeId);

    /** Nombre de badges par joueur : lignes {userId, count}. */
    @Query("select ub.userId, count(ub) from UserBadge ub group by ub.userId")
    List<Object[]> countBadgesPerUser();

    @Modifying
    @Query("delete from UserBadge ub where ub.badge.id = :badgeId")
    void deleteByBadgeId(@Param("badgeId") Long badgeId);
}
