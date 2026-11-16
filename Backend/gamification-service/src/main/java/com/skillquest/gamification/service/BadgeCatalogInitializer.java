package com.skillquest.gamification.service;

import java.util.List;

import com.skillquest.gamification.domain.Badge;
import com.skillquest.gamification.repository.BadgeRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Insère au démarrage le catalogue de badges par défaut (sans toucher à ceux déjà présents). */
@Component
public class BadgeCatalogInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BadgeCatalogInitializer.class);

    private final BadgeRepository badgeRepository;

    public BadgeCatalogInitializer(BadgeRepository badgeRepository) {
        this.badgeRepository = badgeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Badge> defaults = List.of(
                new Badge("FIRST_STEPS", "Premiers pas",
                        "Premiers 50 XP gagnés sur SkillQuest", "/icons/first-steps.svg", 50),
                new Badge("CODE_APPRENTICE", "Apprenti codeur",
                        "Atteindre 200 XP", "/icons/code-apprentice.svg", 200),
                new Badge("CODE_WARRIOR", "Guerrier du code",
                        "Atteindre 500 XP", "/icons/code-warrior.svg", 500),
                new Badge("CODE_MASTER", "Maître du code",
                        "Atteindre 1000 XP", "/icons/code-master.svg", 1000),
                new Badge(CertificateService.COURSE_GRADUATE_BADGE, "Diplômé",
                        "Terminer un cours et recevoir son certificat", "/icons/course-graduate.svg", null));

        int created = 0;
        for (Badge badge : defaults) {
            if (badgeRepository.findByCodeName(badge.getCodeName()).isEmpty()) {
                badgeRepository.save(badge);
                created++;
            }
        }
        if (created > 0) {
            log.info("Seeded {} default badges", created);
        }
    }
}
