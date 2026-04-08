package com.college.rbac.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents an audit log entry for tracking all RBAC-related actions.
 */
public class AuditLog {
    private final String actorUsername;
    private final Role actorRole;
    private final String action;
    private final String result;
    private final String details;
    private final LocalDateTime timestamp;

    public AuditLog(String actorUsername, Role actorRole, String action,
                    String result, String details) {
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.action = action;
        this.result = result;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public String getActorUsername() { return actorUsername; }
    public Role getActorRole() { return actorRole; }
    public String getAction() { return action; }
    public String getResult() { return result; }
    public String getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public String getTimestampFormatted() {
        return timestamp.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    }

    public boolean isSuccess() {
        return "GRANTED".equalsIgnoreCase(result) || "SUCCESS".equalsIgnoreCase(result);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) -> %s : %s | %s",
            getTimestampFormatted(), actorUsername, actorRole, action, result, details);
    }
}
