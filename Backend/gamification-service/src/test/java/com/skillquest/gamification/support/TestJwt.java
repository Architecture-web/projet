package com.skillquest.gamification.support;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/** Fabrique des jetons au meme format que auth-service : HS512, sub = userId, claim "roles". */
public final class TestJwt {

    public static final String DEFAULT_SECRET =
            "skillquest_super_secret_jwt_key_2026_at_least_256_bits_long_and_robust_security";

    private TestJwt() {
    }

    public static String token(UUID userId, String secret, Instant expiresAt, String... roles) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .claim("email", "test@esprit.tn")
                    .claim("roles", List.of(roles))
                    .issueTime(new Date())
                    .expirationTime(Date.from(expiresAt))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
            jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static String token(UUID userId, String... roles) {
        return token(userId, DEFAULT_SECRET, Instant.now().plusSeconds(600), roles);
    }
}
