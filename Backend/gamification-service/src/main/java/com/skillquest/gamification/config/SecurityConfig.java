package com.skillquest.gamification.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Securite : les jetons JWT sont emis par auth-service (HS512, secret partage).
 * <ul>
 *   <li>{@code /api/admin/**} : reserve a ROLE_ADMIN ;</li>
 *   <li>{@code /api/quizzes/**} : tout utilisateur connecte (l'etudiant est identifie par le champ {@code sub}) ;</li>
 *   <li>le reste (classement, badges, profils, verification de certificat...) reste public, car appele
 *       par les autres services (auth-service, job-board-service) sans jeton.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/quizzes/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter())));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${gamification.security.jwt-secret}") String secret) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS512).build();
    }

    /** Les roles sont dans le champ {@code roles} du jeton (ex. ["ROLE_ADMIN"]), deja prefixes. */
    private JwtAuthenticationConverter jwtConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
