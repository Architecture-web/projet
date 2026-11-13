package com.skillquest.auth.client;

import com.skillquest.auth.dto.CreateGamificationProfileRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * Communication Synchrone REST (AWD Exigence 1)
 * Appelle gamification-service pour initialiser le profil joueur avec 0 XP à l'inscription.
 */
@Component
public class GamificationClient {

    private static final Logger log = LoggerFactory.getLogger(GamificationClient.class);

    private final RestTemplate restTemplate;
    private final String gamificationServiceUrl;

    public GamificationClient(
        @Value("${app.services.gamification-url:http://localhost:8084}") String gamificationServiceUrl
    ) {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(600); // 600ms fail-fast
        factory.setReadTimeout(600);
        this.restTemplate = new RestTemplate(factory);
        this.gamificationServiceUrl = gamificationServiceUrl;
    }

    public boolean createPlayerProfile(UUID userId) {
        String endpoint = gamificationServiceUrl + "/api/gamification/profiles";
        CreateGamificationProfileRequest request = new CreateGamificationProfileRequest(userId, 0);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateGamificationProfileRequest> entity = new HttpEntity<>(request, headers);

        try {
            log.info("[REST Synchrone] Appel émis vers {} pour le profil utilisateur {}", endpoint, userId);
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);
            log.info("[REST Synchrone] Réponse reçue de gamification-service : Status {}", response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception ex) {
            log.warn("[REST Synchrone] Le service de gamification est temporairement injoignable ({}) : {}. Traitement en mode tolérant aux pannes.",
                endpoint, ex.getMessage());
            // Fallback résilient : ne bloque pas l'inscription de l'élève
            return false;
        }
    }
}
