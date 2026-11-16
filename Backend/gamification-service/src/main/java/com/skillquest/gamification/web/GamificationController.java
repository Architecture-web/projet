package com.skillquest.gamification.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.skillquest.gamification.dto.ApiDtos.BadgeResponse;
import com.skillquest.gamification.dto.ApiDtos.CreateProfileRequest;
import com.skillquest.gamification.dto.ApiDtos.LeaderboardEntry;
import com.skillquest.gamification.dto.ApiDtos.ProfileResponse;
import com.skillquest.gamification.dto.ApiDtos.UserBadgeResponse;
import com.skillquest.gamification.service.GamificationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gamification")
public class GamificationController {

    private final GamificationService gamificationService;

    public GamificationController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    /** Appelé par auth-service à l'inscription d'un utilisateur. */
    @PostMapping("/profiles")
    public ResponseEntity<ProfileResponse> createProfile(@Valid @RequestBody CreateProfileRequest request) {
        ProfileResponse profile = gamificationService.createProfile(request.userId());
        return ResponseEntity.created(URI.create("/api/gamification/profiles/" + profile.userId())).body(profile);
    }

    @GetMapping("/profiles/{userId}")
    public ProfileResponse getProfile(@PathVariable UUID userId) {
        return gamificationService.getProfile(userId);
    }

    /** Top des meilleurs codeurs (50 maximum). */
    @GetMapping("/leaderboard")
    public List<LeaderboardEntry> leaderboard(
            @RequestParam(defaultValue = "50") @Min(1) @Max(GamificationService.MAX_LEADERBOARD_SIZE) int limit) {
        return gamificationService.leaderboard(limit);
    }

    @GetMapping("/badges/user/{userId}")
    public List<UserBadgeResponse> userBadges(@PathVariable UUID userId) {
        return gamificationService.getUserBadges(userId);
    }

    @GetMapping("/badges")
    public List<BadgeResponse> badgeCatalog() {
        return gamificationService.badgeCatalog();
    }
}
