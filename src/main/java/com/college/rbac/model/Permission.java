package com.college.rbac.model;

/**
 * Defines all permissions available in the College Portal.
 */
public enum Permission {
    // Admin permissions
    CREATE_USER("Create User", "Can create new user accounts"),
    DELETE_USER("Delete User", "Can delete user accounts"),
    VIEW_ALL_USERS("View All Users", "Can view all users in the system"),
    ASSIGN_ROLE("Assign Role", "Can assign roles to users"),

    // Faculty permissions
    ADD_MARKS("Add Marks", "Can add student marks"),
    UPDATE_MARKS("Update Marks", "Can update existing student marks"),
    VIEW_ALL_MARKS("View All Marks", "Can view all students' marks"),

    // Student permissions
    VIEW_OWN_MARKS("View Own Marks", "Can view own marks only");

    private final String displayName;
    private final String description;

    Permission(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    @Override
    public String toString() { return displayName; }
}
