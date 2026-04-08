package com.college.rbac.model;

/**
 * Defines the available roles in the College Portal RBAC system.
 */
public enum Role {
    ADMIN("Admin", "Manages users, assigns roles, and maintains the system"),
    FACULTY("Faculty", "Adds and updates student marks and academic data"),
    STUDENT("Student", "Views own marks and academic information");

    private final String displayName;
    private final String description;

    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    @Override
    public String toString() { return displayName; }
}
