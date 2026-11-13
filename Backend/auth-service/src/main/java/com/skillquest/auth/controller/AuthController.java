package com.skillquest.auth.controller;

import com.skillquest.auth.dto.*;
import com.skillquest.auth.event.UserReportedAbuseEvent;
import com.skillquest.auth.messaging.AbuseReportListener;
import com.skillquest.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AbuseReportListener abuseReportListener;

    public AuthController(AuthService authService, AbuseReportListener abuseReportListener) {
        this.authService = authService;
        this.abuseReportListener = abuseReportListener;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Authentification Sociale OAuth (Google, GitHub, Microsoft)
     */
    @PostMapping("/social")
    public ResponseEntity<AuthResponse> socialAuth(@Valid @RequestBody SocialAuthRequest request) {
        AuthResponse response = authService.socialLogin(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Mot de passe oublié : demande d'envoi de lien
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        Map<String, Object> response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Réinitialisation de mot de passe avec le token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        Map<String, Object> response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint Synchrone REST appelé par course-service et community-service
     */
    @GetMapping("/users/{id}/summary")
    public ResponseEntity<UserSummaryResponse> getUserSummary(@PathVariable("id") UUID id) {
        UserSummaryResponse response = authService.getUserSummary(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint utilitaire de test de simulation pour l'événement RabbitMQ
     */
    @PostMapping("/simulate/abuse-reported")
    public ResponseEntity<Map<String, Object>> simulateAbuseReport(@RequestBody UserReportedAbuseEvent event) {
        abuseReportListener.onAbuseReported(event);
        return ResponseEntity.ok(Map.of(
            "status", "EVENT_CONSUMED",
            "message", "Événement UserReportedAbuseEvent consommé avec succès",
            "targetUserId", event.getTargetUserId()
        ));
    }
}
