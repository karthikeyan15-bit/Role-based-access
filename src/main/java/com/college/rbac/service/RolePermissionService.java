package com.college.rbac.service;

import com.college.rbac.model.Permission;
import com.college.rbac.model.Role;

import java.util.*;

/**
 * Manages role-to-permission mappings.
 * This is the core of the RBAC engine — defines what each role can do.
 */
public class RolePermissionService {

    private static final Map<Role, Set<Permission>> ROLE_PERMISSIONS = new EnumMap<>(Role.class);

    static {
        // ADMIN has full user management capabilities
        ROLE_PERMISSIONS.put(Role.ADMIN, EnumSet.of(
            Permission.CREATE_USER,
            Permission.DELETE_USER,
            Permission.VIEW_ALL_USERS,
            Permission.ASSIGN_ROLE,
            Permission.VIEW_ALL_MARKS
        ));

        // FACULTY can manage academic marks
        ROLE_PERMISSIONS.put(Role.FACULTY, EnumSet.of(
            Permission.ADD_MARKS,
            Permission.UPDATE_MARKS,
            Permission.VIEW_ALL_MARKS
        ));

        // STUDENT can only view own marks
        ROLE_PERMISSIONS.put(Role.STUDENT, EnumSet.of(
            Permission.VIEW_OWN_MARKS
        ));
    }

    /**
     * Check if a role has a specific permission.
     */
    public boolean hasPermission(Role role, Permission permission) {
        Set<Permission> perms = ROLE_PERMISSIONS.getOrDefault(role, Collections.emptySet());
        return perms.contains(permission);
    }

    /**
     * Get all permissions for a role.
     */
    public Set<Permission> getPermissionsForRole(Role role) {
        return Collections.unmodifiableSet(
            ROLE_PERMISSIONS.getOrDefault(role, Collections.emptySet())
        );
    }

    /**
     * Get all role-permission mappings.
     */
    public Map<Role, Set<Permission>> getAllMappings() {
        return Collections.unmodifiableMap(ROLE_PERMISSIONS);
    }

    /**
     * Returns a human-readable description of role capabilities.
     */
    public String getRoleSummary(Role role) {
        Set<Permission> perms = getPermissionsForRole(role);
        StringBuilder sb = new StringBuilder();
        sb.append(role.getDisplayName()).append(" can:\n");
        perms.forEach(p -> sb.append("  ✔ ").append(p.getDisplayName()).append("\n"));
        return sb.toString();
    }
}
