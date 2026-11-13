package com.skillquest.auth.dto;

import java.util.UUID;

public class CreateGamificationProfileRequest {

    private UUID userId;
    private Integer initialXP;

    public CreateGamificationProfileRequest() {}

    public CreateGamificationProfileRequest(UUID userId, Integer initialXP) {
        this.userId = userId;
        this.initialXP = initialXP;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Integer getInitialXP() {
        return initialXP;
    }

    public void setInitialXP(Integer initialXP) {
        this.initialXP = initialXP;
    }
}
