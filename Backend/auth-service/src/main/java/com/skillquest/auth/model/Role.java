package com.skillquest.auth.model;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 32)
    private RoleCode code;

    @Column(length = 255)
    private String description;

    public Role() {}

    public Role(RoleCode code, String description) {
        this.code = code;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public RoleCode getCode() {
        return code;
    }

    public void setCode(RoleCode code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return code == role.code;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
