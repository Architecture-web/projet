package com.skillquest.auth.event;

import java.time.Instant;
import java.util.UUID;

public class UserReportedAbuseEvent {

    private String eventId;
    private String eventType;
    private UUID targetUserId;
    private String reason;
    private String reporterId;
    private Instant timestamp;

    public UserReportedAbuseEvent() {
        this.timestamp = Instant.now();
    }

    public UserReportedAbuseEvent(UUID targetUserId, String reason, String reporterId) {
        this.eventId = "evt_abuse_" + UUID.randomUUID().toString().substring(0, 8);
        this.eventType = "USER_REPORTED_ABUSE";
        this.targetUserId = targetUserId;
        this.reason = reason;
        this.reporterId = reporterId;
        this.timestamp = Instant.now();
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

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(UUID targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReporterId() {
        return reporterId;
    }

    public void setReporterId(String reporterId) {
        this.reporterId = reporterId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
