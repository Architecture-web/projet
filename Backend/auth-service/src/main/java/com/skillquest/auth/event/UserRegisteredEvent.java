package com.skillquest.auth.event;

import java.time.Instant;
import java.util.UUID;

public class UserRegisteredEvent {

    private String eventId;
    private String eventType = "USER_REGISTERED";
    private UUID userId;
    private String email;
    private String fullName;
    private String role;
    private Instant timestamp;

    public UserRegisteredEvent() {
        this.eventId = "evt_auth_" + UUID.randomUUID().toString().substring(0, 8);
        this.timestamp = Instant.now();
    }

    public UserRegisteredEvent(UUID userId, String email, String fullName, String role) {
        this();
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
