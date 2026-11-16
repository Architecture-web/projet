package com.skillquest.gamification.web;

import java.util.UUID;

import com.skillquest.gamification.exception.BadRequestException;

import org.springframework.security.oauth2.jwt.Jwt;

final class UserIds {

    private UserIds() {
    }

    /** L'identifiant utilisateur est le champ {@code sub} du JWT emis par auth-service. */
    static UUID from(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BadRequestException("The token subject is not a valid user id");
        }
    }
}
