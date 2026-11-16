package com.skillquest.gamification.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

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

@Tag(name = "Gamification", description = "Profils joueurs, classement et badges (routes publiques)")
@RestController
@RequestMapping("/api/gamification")
public class GamificationController {

    private final GamificationService gamificationService;

    public GamificationController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    /** Appelé par auth-service à l'inscription d'un utilisateur. */
    @Operation(summary = "Initialise le profil de points d'un utilisateur (appele par auth-service a l'inscription)")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Profil cree"), @ApiResponse(responseCode = "400", description = "userId manquant ou invalide"), @ApiResponse(responseCode = "409", description = "Le profil existe deja")})
    @PostMapping("/profiles")
    public ResponseEntity<ProfileResponse> createProfile(@Valid @RequestBody CreateProfileRequest request) {
        ProfileResponse profile = gamificationService.createProfile(request.userId());
        return ResponseEntity.created(URI.create("/api/gamification/profiles/" + profile.userId())).body(profile);
    }

    @Operation(summary = "XP, niveau, rang et XP restante avant le niveau suivant")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Profil trouve"), @ApiResponse(responseCode = "404", description = "Aucun profil pour cet utilisateur")})
    @GetMapping("/profiles/{userId}")
    public ProfileResponse getProfile(@PathVariable UUID userId) {
        return gamificationService.getProfile(userId);
    }

    /** Top des meilleurs codeurs (50 maximum). */
    @Operation(summary = "Classement des meilleurs codeurs (50 maximum, egalite = meme rang)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Classement"), @ApiResponse(responseCode = "400", description = "limit hors de 1..50")})
    @GetMapping("/leaderboard")
    public List<LeaderboardEntry> leaderboard(
            @RequestParam(defaultValue = "50") @Min(1) @Max(GamificationService.MAX_LEADERBOARD_SIZE) int limit) {
        return gamificationService.leaderboard(limit);
    }

    @Operation(summary = "Badges debloques par un etudiant")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Liste des badges"), @ApiResponse(responseCode = "404", description = "Aucun profil pour cet utilisateur")})
    @GetMapping("/badges/user/{userId}")
    public List<UserBadgeResponse> userBadges(@PathVariable UUID userId) {
        return gamificationService.getUserBadges(userId);
    }

    @Operation(summary = "Catalogue complet des badges")
    @GetMapping("/badges")
    public List<BadgeResponse> badgeCatalog() {
        return gamificationService.badgeCatalog();
    }
}
