package com.skillquest.auth.service;

import com.skillquest.auth.client.GamificationClient;
import com.skillquest.auth.dto.*;
import com.skillquest.auth.event.UserRegisteredEvent;
import com.skillquest.auth.event.UserReportedAbuseEvent;
import com.skillquest.auth.messaging.EventBusManager;
import com.skillquest.auth.model.*;
import com.skillquest.auth.repository.PasswordResetTokenRepository;
import com.skillquest.auth.repository.RefreshTokenRepository;
import com.skillquest.auth.repository.RoleRepository;
import com.skillquest.auth.repository.UserRepository;
import com.skillquest.auth.security.JwtProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final GamificationClient gamificationClient;
    private final EventBusManager eventBusManager;
    private final long refreshTokenExpirationMs;

    public AuthService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtProvider jwtProvider,
        GamificationClient gamificationClient,
        EventBusManager eventBusManager,
        @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshTokenExpirationMs
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.gamificationClient = gamificationClient;
        this.eventBusManager = eventBusManager;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte existe déjà avec cette adresse e-mail.");
        }

        User user = new User(
            request.getEmail().toLowerCase().trim(),
            passwordEncoder.encode(request.getPassword()),
            request.getFirstName().trim(),
            request.getLastName().trim()
        );
        user.setAvatarUrl("https://api.dicebear.com/7.x/bottts/svg?seed=" + user.getEmail());

        RoleCode requestedRoleCode = request.getRole() != null ? request.getRole() : RoleCode.ROLE_STUDENT;
        Role role = roleRepository.findByCode(requestedRoleCode)
            .orElseGet(() -> roleRepository.save(new Role(requestedRoleCode, "Rôle " + requestedRoleCode.name())));
        user.addRole(role);

        User savedUser = userRepository.save(user);
        log.info("Utilisateur enregistré avec succès : id={}, email={}", savedUser.getId(), savedUser.getEmail());

        // 1. Interaction Synchrone REST (AWD Exigence 1)
        gamificationClient.createPlayerProfile(savedUser.getId());

        // 2. Publication d'Événement Métier RabbitMQ (AWD Exigence 2)
        UserRegisteredEvent event = new UserRegisteredEvent(
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getFullName(),
            requestedRoleCode.name()
        );
        eventBusManager.publishUserRegistered(event);

        // 3. Génération des tokens JWT & Redirection par rôle
        return generateTokensForUser(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte suspendu suite à des signalements d'abus.");
        }

        if (user.getPasswordHash() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Ce compte a été créé avec " + user.getProvider() + ". Veuillez vous connecter via ce service.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
        }

        return generateTokensForUser(user);
    }

    /**
     * Authentification / Inscription Sociale OAuth (Google, GitHub, Microsoft)
     */
    @Transactional
    public AuthResponse socialLogin(SocialAuthRequest request) {
        String cleanEmail = request.getEmail().toLowerCase().trim();
        Optional<User> existingUserOpt = userRepository.findByEmail(cleanEmail);

        User user;
        boolean isNewUser = false;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce compte est actuellement suspendu.");
            }
            if (user.getAvatarUrl() == null && request.getAvatarUrl() != null) {
                user.setAvatarUrl(request.getAvatarUrl());
                userRepository.save(user);
            }
        } else {
            // Création automatique à la première connexion OAuth
            user = new User(
                cleanEmail,
                request.getFirstName().trim(),
                request.getLastName().trim(),
                request.getProvider(),
                request.getProviderId(),
                request.getAvatarUrl() != null ? request.getAvatarUrl() : "https://api.dicebear.com/7.x/bottts/svg?seed=" + cleanEmail
            );

            RoleCode roleCode = request.getRole() != null ? request.getRole() : RoleCode.ROLE_STUDENT;
            Role role = roleRepository.findByCode(roleCode)
                .orElseGet(() -> roleRepository.save(new Role(roleCode, "Rôle " + roleCode.name())));
            user.addRole(role);

            try {
                user = userRepository.saveAndFlush(user);
                isNewUser = true;

                // Déclencheurs inter-services pour le nouvel inscrit
                gamificationClient.createPlayerProfile(user.getId());
                eventBusManager.publishUserRegistered(new UserRegisteredEvent(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    roleCode.name()
                ));
            } catch (org.springframework.dao.DataIntegrityViolationException dive) {
                log.info("Utilisateur {} déjà inséré (concurrence gérée), chargement du compte existant", cleanEmail);
                user = userRepository.findByEmail(cleanEmail)
                    .orElseThrow(() -> dive);
                isNewUser = false;
            }
        }

        log.info("Connexion sociale réussie : provider={}, email={}, isNew={}", request.getProvider(), user.getEmail(), isNewUser);
        return generateTokensForUser(user);
    }

    /**
     * Mot de passe oublié : Génère un token temporaire de réinitialisation
     */
    @Transactional
    public Map<String, Object> forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            // Sécurité : message générique pour ne pas divulguer si l'e-mail existe
            return Map.of(
                "success", true,
                "message", "Si un compte est associé à cette adresse, un lien de réinitialisation a été envoyé."
            );
        }

        User user = userOpt.get();
        if (user.getPasswordHash() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Ce compte utilise la connexion " + user.getProvider() + ". Aucune réinitialisation de mot de passe nécessaire.");
        }

        // Nettoie les anciens tokens
        passwordResetTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        Instant expiryDate = Instant.now().plusSeconds(3600); // Valide 1 heure
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
        passwordResetTokenRepository.save(resetToken);

        log.info("[Password Reset] Token généré pour {} : token={}", user.getEmail(), token);

        return Map.of(
            "success", true,
            "message", "Le code de réinitialisation a été généré.",
            "resetToken", token, // Renvoyé pour faciliter les tests dans l'UI
            "expiresInSeconds", 3600
        );
    }

    /**
     * Validation du reset token et application du nouveau mot de passe
     */
    @Transactional
    public Map<String, Object> resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de réinitialisation invalide ou introuvable."));

        if (resetToken.isExpired() || Boolean.TRUE.equals(resetToken.getIsUsed())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce token a expiré ou a déjà été utilisé.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setIsUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Mot de passe mis à jour avec succès pour l'utilisateur {}", user.getEmail());
        return Map.of(
            "success", true,
            "message", "Votre mot de passe a été modifié avec succès. Vous pouvez maintenant vous connecter."
        );
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh token invalide"));

        if (refreshToken.isExpired() || Boolean.TRUE.equals(refreshToken.getIsRevoked())) {
            refreshTokenRepository.delete(refreshToken);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh token expiré ou révoqué");
        }

        User user = refreshToken.getUser();
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte suspendu");
        }

        Set<String> roleCodes = user.getRoles().stream()
            .map(r -> r.getCode().name())
            .collect(Collectors.toSet());

        String newAccessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail(), roleCodes);
        String redirectUrl = determineRedirectUrl(roleCodes);

        return new AuthResponse(
            newAccessToken,
            refreshToken.getToken(),
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getAvatarUrl(),
            user.getProvider().name(),
            roleCodes,
            redirectUrl
        );
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getUserSummary(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        Set<String> roleCodes = user.getRoles().stream()
            .map(r -> r.getCode().name())
            .collect(Collectors.toSet());

        String avatar = user.getAvatarUrl() != null ? user.getAvatarUrl() : "https://api.dicebear.com/7.x/bottts/svg?seed=" + user.getEmail();
        return new UserSummaryResponse(user.getId(), user.getFullName(), user.getEmail(), roleCodes, user.getIsActive(), avatar);
    }

    @Transactional
    public void handleUserReportedAbuse(UserReportedAbuseEvent event) {
        log.warn("[RabbitMQ Asynchrone Consommé] Signalement d'abus reçu pour la cible {} par {} (Motif: {})",
            event.getTargetUserId(), event.getReporterId(), event.getReason());

        userRepository.findById(event.getTargetUserId()).ifPresent(user -> {
            user.incrementReportCount();
            userRepository.save(user);
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                log.error("Compte utilisateur {} désactivé automatiquement suite à {} signalements d'abus vérifiés !",
                    user.getId(), user.getReportCount());
            }
        });
    }

    private AuthResponse generateTokensForUser(User user) {
        Set<String> roleCodes = user.getRoles().stream()
            .map(r -> r.getCode().name())
            .collect(Collectors.toSet());

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail(), roleCodes);

        String rawRefreshToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        Instant expiryDate = Instant.now().plusMillis(refreshTokenExpirationMs);
        RefreshToken refreshToken = new RefreshToken(rawRefreshToken, user, expiryDate);
        refreshTokenRepository.save(refreshToken);

        String redirectUrl = determineRedirectUrl(roleCodes);
        String avatar = user.getAvatarUrl() != null ? user.getAvatarUrl() : "https://api.dicebear.com/7.x/bottts/svg?seed=" + user.getEmail();

        return new AuthResponse(
            accessToken,
            rawRefreshToken,
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            avatar,
            user.getProvider().name(),
            roleCodes,
            redirectUrl
        );
    }

    /**
     * Redirection intelligente selon le rôle RBAC du compte
     */
    private String determineRedirectUrl(Set<String> roles) {
        if (roles.contains(RoleCode.ROLE_RECRUITER.name())) {
            return "/dashboard/recruiter"; // Espace gestion d'offres et candidatures
        } else if (roles.contains(RoleCode.ROLE_INSTRUCTOR.name())) {
            return "/dashboard/instructor"; // Espace création de cours et exercices
        } else if (roles.contains(RoleCode.ROLE_ADMIN.name())) {
            return "/dashboard/admin"; // Espace administration et métriques
        }
        return "/dashboard/student"; // Espace par défaut : cours, sandbox et badges
    }
}
