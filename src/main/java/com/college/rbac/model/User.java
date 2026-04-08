package com.college.rbac.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Represents a user in the College Portal RBAC system.
 */
public class User {
    private final String id;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private Role role;
    private boolean active;
    private final LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    public User(String username, String password, String fullName, String email, Role role) {
        this.id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.lastLogin = null;
    }

    // Getters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastLogin() { return lastLogin; }

    // Setters
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(Role role) { this.role = role; }
    public void setActive(boolean active) { this.active = active; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public String getCreatedAtFormatted() {
        return createdAt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
    }

    public String getLastLoginFormatted() {
        return lastLogin != null
            ? lastLogin.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
            : "Never";
    }

    @Override
    public String toString() {
        return String.format("User[id=%s, username=%s, role=%s, active=%s]",
            id, username, role, active);
    }
}
