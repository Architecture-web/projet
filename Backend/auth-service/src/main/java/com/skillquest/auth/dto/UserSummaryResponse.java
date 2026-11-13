package com.skillquest.auth.dto;

import java.util.Set;
import java.util.UUID;

public class UserSummaryResponse {

    private UUID id;
    private String fullName;
    private String email;
    private Set<String> roles;
    private Boolean isActive;
    private String avatarUrl;

    public UserSummaryResponse() {}

    public UserSummaryResponse(UUID id, String fullName, String email, Set<String> roles, Boolean isActive, String avatarUrl) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.roles = roles;
        this.isActive = isActive;
        this.avatarUrl = avatarUrl;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
