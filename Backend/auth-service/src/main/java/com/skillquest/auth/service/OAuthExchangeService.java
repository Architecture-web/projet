package com.skillquest.auth.service;

import com.skillquest.auth.dto.AuthResponse;
import com.skillquest.auth.dto.SocialAuthRequest;
import com.skillquest.auth.model.AuthProvider;
import com.skillquest.auth.model.RoleCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Service gérant le flux réel OAuth 2.0 avec Google, GitHub et Microsoft.
 * Échange les codes d'autorisation contre des jetons et récupère les données des profils.
 */
@Service
public class OAuthExchangeService {

    private static final Logger log = LoggerFactory.getLogger(OAuthExchangeService.class);

    private final AuthService authService;
    private final RestTemplate restTemplate;

    @Value("${app.oauth.base-callback-url:http://localhost:8081/api/auth/oauth}")
    private String baseCallbackUrl;

    @Value("${app.oauth.github.client-id:}")
    private String githubClientId;

    @Value("${app.oauth.github.client-secret:}")
    private String githubClientSecret;

    @Value("${app.oauth.google.client-id:}")
    private String googleClientId;

    @Value("${app.oauth.google.client-secret:}")
    private String googleClientSecret;

    public OAuthExchangeService(AuthService authService) {
        this.authService = authService;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Construit l'URL officielle du fournisseur vers laquelle rediriger l'utilisateur
     */
    public String buildAuthorizeUrl(String provider) {
        String prov = provider.toUpperCase().trim();
        String callback = baseCallbackUrl + "/" + prov.toLowerCase() + "/callback";

        switch (prov) {
            case "GITHUB":
                if (githubClientId == null || githubClientId.isBlank() || githubClientId.contains("VOTRE_")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "GitHub Client ID non configuré dans application.properties");
                }
                return "https://github.com/login/oauth/authorize"
                    + "?client_id=" + URLEncoder.encode(githubClientId, StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(callback, StandardCharsets.UTF_8)
                    + "&scope=read:user%20user:email";

            case "GOOGLE":
                if (googleClientId == null || googleClientId.isBlank() || googleClientId.contains("VOTRE_")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Google Client ID non configuré dans application.properties");
                }
                return "https://accounts.google.com/o/oauth2/v2/auth"
                    + "?client_id=" + URLEncoder.encode(googleClientId, StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(callback, StandardCharsets.UTF_8)
                    + "&response_type=code"
                    + "&scope=openid%20profile%20email";

            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fournisseur non supporté : " + provider);
        }
    }

    /**
     * Traitement du callback GitHub réel
     */
    public AuthResponse processGitHubCallback(String code) {
        String tokenUrl = "https://github.com/login/oauth/access_token";
        String callback = baseCallbackUrl + "/github/callback";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", githubClientId);
        body.add("client_secret", githubClientSecret);
        body.add("code", code);
        body.add("redirect_uri", callback);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> tokenResponse = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> tokenBody = tokenResponse.getBody();
            if (tokenBody == null || !tokenBody.containsKey("access_token")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Impossible de récupérer le token GitHub");
            }

            String accessToken = (String) tokenBody.get("access_token");

            HttpHeaders authHeaders = new HttpHeaders();
            authHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> authRequest = new HttpEntity<>(authHeaders);

            ResponseEntity<Map<String, Object>> userResponse = restTemplate.exchange(
                "https://api.github.com/user",
                HttpMethod.GET,
                authRequest,
                new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> userMap = userResponse.getBody();
            if (userMap == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Profil GitHub inaccessible");
            }

            String login = (String) userMap.get("login");
            String name = (String) userMap.get("name");
            String avatarUrl = (String) userMap.get("avatar_url");
            String providerId = String.valueOf(userMap.get("id"));

            String email = (String) userMap.get("email");
            if (email == null || email.isBlank()) {
                try {
                    ResponseEntity<List<Map<String, Object>>> emailsResponse = restTemplate.exchange(
                        "https://api.github.com/user/emails",
                        HttpMethod.GET,
                        authRequest,
                        new ParameterizedTypeReference<>() {}
                    );
                    List<Map<String, Object>> emails = emailsResponse.getBody();
                    if (emails != null) {
                        for (Map<String, Object> em : emails) {
                            if (Boolean.TRUE.equals(em.get("primary"))) {
                                email = (String) em.get("email");
                                break;
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.warn("E-mail principal GitHub introuvable via l'API secondaire : {}", ex.getMessage());
                }
            }

            if (email == null || email.isBlank()) {
                email = login + "@github.user";
            }

            String firstName = (name != null && !name.isBlank()) ? name.split(" ")[0] : login;
            String lastName = (name != null && name.contains(" ")) ? name.substring(name.indexOf(" ") + 1) : "GitHub";

            SocialAuthRequest socialRequest = new SocialAuthRequest(
                AuthProvider.GITHUB,
                providerId,
                email,
                firstName,
                lastName,
                avatarUrl,
                RoleCode.ROLE_STUDENT
            );

            return authService.socialLogin(socialRequest);
        } catch (Exception e) {
            log.error("Échec du flux OAuth GitHub : {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Erreur GitHub OAuth : " + e.getMessage());
        }
    }

    /**
     * Traitement du callback Google réel
     */
    public AuthResponse processGoogleCallback(String code) {
        String tokenUrl = "https://oauth2.googleapis.com/token";
        String callback = baseCallbackUrl + "/google/callback";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("code", code);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", callback);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> tokenResponse = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> tokenBody = tokenResponse.getBody();
            if (tokenBody == null || !tokenBody.containsKey("access_token")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Impossible de récupérer le token Google");
            }

            String accessToken = (String) tokenBody.get("access_token");

            HttpHeaders authHeaders = new HttpHeaders();
            authHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> authRequest = new HttpEntity<>(authHeaders);

            ResponseEntity<Map<String, Object>> userResponse = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v3/userinfo",
                HttpMethod.GET,
                authRequest,
                new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> userMap = userResponse.getBody();
            if (userMap == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Profil Google inaccessible");
            }

            String email = (String) userMap.get("email");
            if (email == null || email.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Aucune adresse e-mail valide renvoyée par Google");
            }

            Object emailVerified = userMap.get("email_verified");
            if (Boolean.FALSE.equals(emailVerified)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "L'adresse e-mail Google doit être vérifiée.");
            }

            String givenName = (String) userMap.get("given_name");
            String familyName = (String) userMap.get("family_name");
            String picture = (String) userMap.get("picture");
            String sub = (String) userMap.get("sub");

            SocialAuthRequest socialRequest = new SocialAuthRequest(
                AuthProvider.GOOGLE,
                sub,
                email,
                givenName != null ? givenName : "GoogleUser",
                familyName != null ? familyName : "User",
                picture,
                RoleCode.ROLE_STUDENT
            );

            return authService.socialLogin(socialRequest);
        } catch (Exception e) {
            log.error("Échec du flux OAuth Google : {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Erreur Google OAuth : " + e.getMessage());
        }
    }
}

