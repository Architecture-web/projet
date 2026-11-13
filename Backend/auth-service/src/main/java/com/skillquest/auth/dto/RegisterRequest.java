package com.skillquest.auth.dto;

import com.skillquest.auth.model.RoleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "L'adresse e-mail est obligatoire")
    @Email(message = "Format d'e-mail invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit comporter au moins 6 caractères")
    private String password;

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    private RoleCode role = RoleCode.ROLE_STUDENT;

    public RegisterRequest() {}

    public RegisterRequest(String email, String password, String firstName, String lastName, RoleCode role) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role != null ? role : RoleCode.ROLE_STUDENT;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public RoleCode getRole() {
        return role;
    }

    public void setRole(RoleCode role) {
        this.role = role;
    }
}
