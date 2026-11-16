package com.skillquest.gamification.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import com.skillquest.gamification.domain.Badge;
import com.skillquest.gamification.domain.LevelCalculator;
import com.skillquest.gamification.domain.ProcessedEvent;
import com.skillquest.gamification.domain.UserBadge;
import com.skillquest.gamification.domain.UserProfileGamification;
import com.skillquest.gamification.dto.ApiDtos.BadgeResponse;
import com.skillquest.gamification.dto.ApiDtos.LeaderboardEntry;
import com.skillquest.gamification.dto.ApiDtos.ProfileResponse;
import com.skillquest.gamification.dto.ApiDtos.UserBadgeResponse;
import com.skillquest.gamification.exception.BadRequestException;
import com.skillquest.gamification.exception.ConflictException;
import com.skillquest.gamification.exception.ResourceNotFoundException;
import com.skillquest.gamification.messaging.BadgeUnlockedEvent;
import com.skillquest.gamification.repository.BadgeRepository;
import com.skillquest.gamification.repository.ProcessedEventRepository;
import com.skillquest.gamification.repository.UserBadgeRepository;
import com.skillquest.gamification.repository.UserProfileRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cœur métier : comptabilité de l'XP, niveaux, badges et classement.
 * Chaque opération d'écriture est transactionnelle (ACID) et le profil est verrouillé
 * pendant la mise à jour pour empêcher toute double dépense de points en cas de concurrence.
 */
@Service
public class GamificationService {

    public static final int MAX_LEADERBOARD_SIZE = 50;

    private static final Logger log = LoggerFactory.getLogger(GamificationService.class);

    private final UserProfileRepository profileRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    public GamificationService(UserProfileRepository profileRepository,
                               BadgeRepository badgeRepository,
                               UserBadgeRepository userBadgeRepository,
                               ProcessedEventRepository processedEventRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.profileRepository = profileRepository;
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.processedEventRepository = processedEventRepository;
        this.eventPublisher = eventPublisher;
    }

    // ---------------------------------------------------------------- écriture

    @Transactional
    public ProfileResponse createProfile(UUID userId) {
        if (profileRepository.existsById(userId)) {
            throw new ConflictException("Gamification profile already exists for user " + userId);
        }
        try {
            UserProfileGamification profile = profileRepository.saveAndFlush(new UserProfileGamification(userId));
            return toResponse(profile);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Gamification profile already exists for user " + userId);
        }
    }

    /** Crédite l'XP, recalcule le niveau et débloque les badges de palier atteints. */
    @Transactional
    public void addXP(UUID userId, int amount) {
        UserProfileGamification profile = lockOrCreate(userId);
        boolean leveledUp = profile.addXP(amount);
        profile.setGlobalRank((int) profileRepository.countWithMoreXPThan(profile.getTotalXP()) + 1);
        unlockEligibleBadges(profile);
        log.info("user={} +{} XP -> total={} level={}{}", userId, amount, profile.getTotalXP(),
                profile.getCurrentLevel(), leveledUp ? " (level up!)" : "");
    }

    /**
     * Variante idempotente pour les messages RabbitMQ : si {@code eventId} a déjà été traité
     * (re-livraison), l'XP n'est pas recréditée.
     *
     * @return {@code true} si l'XP a été appliquée, {@code false} si l'événement était un doublon
     */
    @Transactional
    public boolean addXPForEvent(UUID eventId, UUID userId, int amount) {
        if (eventId != null) {
            if (processedEventRepository.existsById(eventId)) {
                log.info("Event {} already processed, ignoring", eventId);
                return false;
            }
            processedEventRepository.save(new ProcessedEvent(eventId));
        }
        addXP(userId, amount);
        return true;
    }

    /** Ajustement manuel de l'XP par un administrateur (delta positif ou négatif, jamais sous 0). */
    @Transactional
    public ProfileResponse adjustXP(UUID userId, int delta) {
        if (delta == 0) {
            throw new BadRequestException("amount must not be 0");
        }
        UserProfileGamification profile = profileRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No gamification profile for user " + userId));
        if (delta > 0) {
            addXP(userId, delta);
        } else {
            profile.adjustXP(delta);
            profile.setGlobalRank((int) profileRepository.countWithMoreXPThan(profile.getTotalXP()) + 1);
        }
        log.info("admin adjusted XP of user={} by {} -> total={}", userId, delta, profile.getTotalXP());
        return toResponse(profile);
    }

    /** Attribue un badge « sur événement » (ex. COURSE_GRADUATE). Sans effet s'il est déjà possédé. */
    @Transactional
    public boolean awardBadge(UUID userId, String codeName) {
        UserProfileGamification profile = lockOrCreate(userId);
        Badge badge = badgeRepository.findByCodeName(codeName)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown badge " + codeName));
        if (userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())) {
            return false;
        }
        unlock(profile, badge);
        return true;
    }

    // ----------------------------------------------------------------- lecture

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        return toResponse(requireProfile(userId));
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> leaderboard(int limit) {
        int size = Math.min(Math.max(limit, 1), MAX_LEADERBOARD_SIZE);
        List<UserProfileGamification> top = profileRepository.findTop(PageRequest.of(0, size));
        // Classement « compétition » : deux joueurs à égalité d'XP partagent le même rang.
        int[] ranks = new int[top.size()];
        IntStream.range(0, top.size()).forEach(i ->
                ranks[i] = (i > 0 && top.get(i).getTotalXP() == top.get(i - 1).getTotalXP())
                        ? ranks[i - 1]
                        : i + 1);
        return IntStream.range(0, top.size())
                .mapToObj(i -> new LeaderboardEntry(ranks[i], top.get(i).getUserId(),
                        top.get(i).getTotalXP(), top.get(i).getCurrentLevel()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserBadgeResponse> getUserBadges(UUID userId) {
        requireProfile(userId);
        return userBadgeRepository.findAllWithBadgeByUserId(userId).stream()
                .map(ub -> new UserBadgeResponse(ub.getBadge().getId(), ub.getBadge().getCodeName(),
                        ub.getBadge().getTitle(), ub.getBadge().getDescription(),
                        ub.getBadge().getIconUrl(), ub.getUnlockedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BadgeResponse> badgeCatalog() {
        return badgeRepository.findAllByOrderByRequiredXPAscIdAsc().stream()
                .map(b -> new BadgeResponse(b.getId(), b.getCodeName(), b.getTitle(), b.getDescription(),
                        b.getIconUrl(), b.getRequiredXP()))
                .toList();
    }

    // ----------------------------------------------------------------- interne

    private UserProfileGamification requireProfile(UUID userId) {
        return profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No gamification profile for user " + userId));
    }

    /**
     * Verrouille le profil (SELECT ... FOR UPDATE) ou le crée s'il n'existe pas encore.
     * Si deux créations concurrentes se produisent, l'une échoue sur la clé primaire :
     * le message RabbitMQ est alors rejoué par le mécanisme de retry et réussit.
     */
    private UserProfileGamification lockOrCreate(UUID userId) {
        return profileRepository.findByIdForUpdate(userId)
                .orElseGet(() -> profileRepository.saveAndFlush(new UserProfileGamification(userId)));
    }

    private void unlockEligibleBadges(UserProfileGamification profile) {
        Set<Long> owned = new HashSet<>(userBadgeRepository.findBadgeIdsByUserId(profile.getUserId()));
        badgeRepository.findUnlockableWithXP(profile.getTotalXP()).stream()
                .filter(badge -> !owned.contains(badge.getId()))
                .forEach(badge -> unlock(profile, badge));
    }

    private void unlock(UserProfileGamification profile, Badge badge) {
        userBadgeRepository.save(new UserBadge(profile.getUserId(), badge));
        eventPublisher.publishEvent(new BadgeUnlockedEvent(
                UUID.randomUUID(), profile.getUserId(), badge.getId(), badge.getCodeName(),
                badge.getTitle(), profile.getTotalXP(), Instant.now()));
        log.info("user={} unlocked badge {}", profile.getUserId(), badge.getCodeName());
    }

    private ProfileResponse toResponse(UserProfileGamification p) {
        int rank = (int) profileRepository.countWithMoreXPThan(p.getTotalXP()) + 1;
        return new ProfileResponse(p.getUserId(), p.getTotalXP(), p.getCurrentLevel(),
                LevelCalculator.xpToNextLevel(p.getTotalXP()), rank, p.getUpdatedAt());
    }
}
