package com.skillquest.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillquest.auth.dto.AuthResponse;
import com.skillquest.auth.service.OAuthExchangeService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/oauth")
public class OAuthController {

    private final OAuthExchangeService oAuthExchangeService;
    private final ObjectMapper objectMapper;

    @Value("${app.oauth.github.client-id:}")
    private String githubClientId;

    @Value("${app.oauth.google.client-id:}")
    private String googleClientId;

    public OAuthController(OAuthExchangeService oAuthExchangeService, ObjectMapper objectMapper) {
        this.oAuthExchangeService = oAuthExchangeService;
        this.objectMapper = objectMapper;
    }

    /**
     * Vérifie la disponibilité des fournisseurs OAuth configurés
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getOAuthStatus() {
        return ResponseEntity.ok(Map.of(
            "github", githubClientId != null && !githubClientId.isBlank() && !githubClientId.contains("VOTRE_"),
            "google", googleClientId != null && !googleClientId.isBlank() && !googleClientId.contains("VOTRE_")
        ));
    }

    /**
     * Redirige l'utilisateur vers la page officielle du fournisseur (Google ou GitHub)
     */
    @GetMapping("/{provider}/authorize")
    public void redirectToProvider(@PathVariable String provider, HttpServletResponse response) throws IOException {
        String authorizeUrl = oAuthExchangeService.buildAuthorizeUrl(provider);
        response.sendRedirect(authorizeUrl);
    }

    /**
     * Callback officiel GitHub
     */
    @GetMapping("/github/callback")
    public void handleGitHubCallback(@RequestParam String code, HttpServletResponse response) throws IOException {
        AuthResponse authResponse = oAuthExchangeService.processGitHubCallback(code);
        redirectToFrontend(authResponse, response);
    }

    /**
     * Callback officiel Google
     */
    @GetMapping("/google/callback")
    public void handleGoogleCallback(@RequestParam String code, HttpServletResponse response) throws IOException {
        AuthResponse authResponse = oAuthExchangeService.processGoogleCallback(code);
        redirectToFrontend(authResponse, response);
    }

    private void redirectToFrontend(AuthResponse authResponse, HttpServletResponse response) throws IOException {
        String userJson = objectMapper.writeValueAsString(authResponse);
        String targetPage = (authResponse.getRoles() != null && authResponse.getRoles().contains("ROLE_RECRUITER"))
            ? "/dashboard-recruiter.html"
            : "/dashboard-student.html";

        String redirectUrl = targetPage + "?oauth_success=true"
            + "&token=" + URLEncoder.encode(authResponse.getAccessToken(), StandardCharsets.UTF_8)
            + "&refresh=" + URLEncoder.encode(authResponse.getRefreshToken(), StandardCharsets.UTF_8)
            + "&user=" + URLEncoder.encode(userJson, StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}

