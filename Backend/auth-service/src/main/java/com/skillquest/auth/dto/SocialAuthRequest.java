package com.skillquest.auth.dto;

import com.skillquest.auth.model.AuthProvider;
import com.skillquest.auth.model.RoleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SocialAuthRequest {

    @NotNull(message = "Le fournisseur OAuth est obligatoire")
    private AuthProvider provider;

    @NotBlank(message = "L'ID du fournisseur est obligatoire")
    private String providerId;

    @NotBlank(message = "L'e-mail est obligatoire")
    @Email(message = "Format d'e-mail invalide")
    private String email;

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    private String avatarUrl;

    private RoleCode role = RoleCode.ROLE_STUDENT;

    public SocialAuthRequest() {}

    public SocialAuthRequest(AuthProvider provider, String providerId, String email, String firstName, String lastName, String avatarUrl, RoleCode role) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.avatarUrl = avatarUrl;
        this.role = role != null ? role : RoleCode.ROLE_STUDENT;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public void setProvider(AuthProvider provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public RoleCode getRole() {
        return role;
    }

    public void setRole(RoleCode role) {
        this.role = role;
    }
}
