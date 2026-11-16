package com.skillquest.gamification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import com.skillquest.gamification.config.GamificationProperties;
import com.skillquest.gamification.domain.Certificate;

import org.junit.jupiter.api.Test;

class CertificateHasherTest {

    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant WHEN = Instant.parse("2026-01-15T10:00:00Z");

    private static CertificateHasher hasherWithSecret(String secret) {
        return new CertificateHasher(new GamificationProperties(null, null,
                new GamificationProperties.CertificateSettings(secret, "http://localhost"), null));
    }

    private static Certificate certificate(String name, String title) {
        return new Certificate(USER, "course-1", name, title, WHEN);
    }

    @Test
    void hashIsDeterministicSha256SizedHex() {
        CertificateHasher hasher = hasherWithSecret("secret");
        String hash = hasher.generateHash(certificate("Ada Lovelace", "Java 101"));

        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
        assertThat(hasher.generateHash(certificate("Ada Lovelace", "Java 101"))).isEqualTo(hash);
    }

    @Test
    void hashChangesWhenAnyFieldOrSecretChanges() {
        String base = hasherWithSecret("secret").generateHash(certificate("Ada Lovelace", "Java 101"));

        assertThat(hasherWithSecret("secret").generateHash(certificate("Ada Lovelac3", "Java 101"))).isNotEqualTo(base);
        assertThat(hasherWithSecret("secret").generateHash(certificate("Ada Lovelace", "Java 102"))).isNotEqualTo(base);
        assertThat(hasherWithSecret("other").generateHash(certificate("Ada Lovelace", "Java 101"))).isNotEqualTo(base);
    }

    @Test
    void fieldBoundariesCannotBeShifted() {
        CertificateHasher hasher = hasherWithSecret("secret");
        assertThat(hasher.generateHash(certificate("Ada;1:x", "Y")))
                .isNotEqualTo(hasher.generateHash(certificate("Ada", "1:x;Y")));
    }

    @Test
    void isIntactDetectsTampering() {
        CertificateHasher hasher = hasherWithSecret("secret");
        Certificate cert = certificate("Ada Lovelace", "Java 101");
        cert.setCertificateHash(hasher.generateHash(cert));
        assertThat(hasher.isIntact(cert)).isTrue();
        assertThat(hasher.verifyHash(cert, cert.getCertificateHash().toUpperCase())).isTrue();
        assertThat(hasher.verifyHash(cert, "deadbeef")).isFalse();

        Certificate forged = certificate("Grace Hopper", "Java 101");
        forged.setCertificateHash(cert.getCertificateHash());
        assertThat(hasher.isIntact(forged)).isFalse();
    }
}
