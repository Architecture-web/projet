package com.skillquest.gamification.repository;

import java.util.List;
import java.util.Optional;

import com.skillquest.gamification.domain.Badge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    Optional<Badge> findByCodeName(String codeName);

    @Query("select b from Badge b where b.requiredXP is not null and b.requiredXP <= :xp order by b.requiredXP asc")
    List<Badge> findUnlockableWithXP(@Param("xp") int xp);

    List<Badge> findAllByOrderByRequiredXPAscIdAsc();
}
