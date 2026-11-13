package com.skillquest.auth.repository;

import com.skillquest.auth.model.Role;
import com.skillquest.auth.model.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(RoleCode code);
}
