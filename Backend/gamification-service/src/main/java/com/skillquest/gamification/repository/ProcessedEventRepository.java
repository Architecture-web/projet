package com.skillquest.gamification.repository;

import java.util.UUID;

import com.skillquest.gamification.domain.ProcessedEvent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
