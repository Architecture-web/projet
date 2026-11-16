package com.skillquest.gamification.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.skillquest.gamification.config.GamificationProperties;
import com.skillquest.gamification.domain.Certificate;

import org.springframework.stereotype.Component;

/**
 * Empreinte infalsifiable d'un certificat : HMAC-SHA256 (clé secrète du service) calculé sur
 * les champs du certificat. Sans la clé, il est impossible de forger une empreinte valide,
 * et toute modification de la base est détectée à la vérification.
 */
@Component
public class CertificateHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] secret;

    public CertificateHasher(GamificationProperties props) {
        this.secret = props.certificate().secret().getBytes(StandardCharsets.UTF_8);
    }

    public String generateHash(Certificate c) {
        String canonical = canonical(
                c.getUserId().toString(),
                c.getCourseId(),
                c.getStudentFullName(),
                c.getCourseTitle(),
                c.getIssuedAt().truncatedTo(ChronoUnit.SECONDS).toString());
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", ex);
        }
    }

    /** Vrai si l'empreinte stockée correspond bien au contenu actuel du certificat. */
    public boolean isIntact(Certificate c) {
        return constantTimeEquals(c.getCertificateHash(), generateHash(c));
    }

    /** Vrai si {@code candidate} est l'empreinte stockée du certificat. */
    public boolean verifyHash(Certificate c, String candidate) {
        return candidate != null
                && constantTimeEquals(c.getCertificateHash(), candidate.trim().toLowerCase(Locale.ROOT));
    }

    /** Sérialisation préfixée par la longueur : évite les collisions entre champs (« a|b » + « c » vs « a » + « b|c »). */
    private static String canonical(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(part.length()).append(':').append(part).append(';');
        }
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
