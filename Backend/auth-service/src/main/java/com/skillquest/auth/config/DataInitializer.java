package com.skillquest.auth.config;

import com.skillquest.auth.model.Role;
import com.skillquest.auth.model.RoleCode;
import com.skillquest.auth.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            Arrays.stream(RoleCode.values()).forEach(code -> {
                roleRepository.findByCode(code).orElseGet(() -> {
                    log.info("Initialisation du rôle : {}", code);
                    return roleRepository.save(new Role(code, "Rôle système standard : " + code.name()));
                });
            });
        };
    }
}
