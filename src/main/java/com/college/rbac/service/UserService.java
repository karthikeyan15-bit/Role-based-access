package com.college.rbac.service;

import com.college.rbac.model.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages user operations with RBAC enforcement.
 * All operations validate permissions before execution.
 */
public class UserService {

    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final Map<String, User> usersById = new ConcurrentHashMap<>();
    private final RolePermissionService permissionService;
    private final AuditService auditService;

    public UserService(RolePermissionService permissionService, AuditService auditService) {
        this.permissionService = permissionService;
        this.auditService = auditService;
        seedDefaultUsers();
    }

    /**
     * Seeds default users for demonstration purposes.
     */
    private void seedDefaultUsers() {
        createUserInternal("admin", "admin123", "Dr. Rajesh Kumar", "admin@college.edu", Role.ADMIN);
        createUserInternal("faculty1", "faculty123", "Prof. Anitha Devi", "anitha@college.edu", Role.FACULTY);
        createUserInternal("faculty2", "faculty123", "Prof. Suresh Raj", "suresh@college.edu", Role.FACULTY);
        createUserInternal("student1", "student123", "Karthikeyan M", "karthik@college.edu", Role.STUDENT);
        createUserInternal("student2", "student123", "Priya Sharma", "priya@college.edu", Role.STUDENT);
        createUserInternal("student3", "student123", "Rahul Patel", "rahul@college.edu", Role.STUDENT);
    }

    private User createUserInternal(String username, String password, String fullName,
                                     String email, Role role) {
        User user = new User(username, password, fullName, email, role);
        usersByUsername.put(username.toLowerCase(), user);
        usersById.put(user.getId(), user);
        return user;
    }

    /**
     * Authenticate a user by username and password.
     */
    public Optional<User> authenticate(String username, String password) {
        User user = usersByUsername.get(username.toLowerCase());
        if (user != null && user.getPassword().equals(password) && user.isActive()) {
            user.setLastLogin(LocalDateTime.now());
            auditService.log(user.getUsername(), user.getRole(), "LOGIN", "SUCCESS",
                "User logged in successfully");
            return Optional.of(user);
        }
        auditService.log(username, null, "LOGIN", "FAILED",
            "Invalid credentials for username: " + username);
        return Optional.empty();
    }

    /**
     * Create a new user (ADMIN only).
     */
    public Map<String, Object> createUser(User actor, String username, String password,
                                           String fullName, String email, Role role) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!permissionService.hasPermission(actor.getRole(), Permission.CREATE_USER)) {
            auditService.log(actor.getUsername(), actor.getRole(), "CREATE_USER", "DENIED",
                "Attempted to create user: " + username);
            result.put("success", false);
            result.put("message", "Access Denied: " + actor.getRole().getDisplayName() +
                " does not have permission to create users.");
            return result;
        }
        if (usersByUsername.containsKey(username.toLowerCase())) {
            result.put("success", false);
            result.put("message", "Username '" + username + "' already exists.");
            return result;
        }
        User newUser = createUserInternal(username, password, fullName, email, role);
        auditService.log(actor.getUsername(), actor.getRole(), "CREATE_USER", "SUCCESS",
            "Created user: " + username + " with role: " + role.getDisplayName());
        result.put("success", true);
        result.put("message", "User '" + fullName + "' created successfully with role: " + role.getDisplayName());
        result.put("user", newUser);
        return result;
    }

    /**
     * Delete a user (ADMIN only).
     */
    public Map<String, Object> deleteUser(User actor, String targetUserId) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!permissionService.hasPermission(actor.getRole(), Permission.DELETE_USER)) {
            auditService.log(actor.getUsername(), actor.getRole(), "DELETE_USER", "DENIED",
                "Attempted to delete user ID: " + targetUserId);
            result.put("success", false);
            result.put("message", "Access Denied: Only Admin can delete users.");
            return result;
        }
        User target = usersById.get(targetUserId);
        if (target == null) {
            result.put("success", false);
            result.put("message", "User not found with ID: " + targetUserId);
            return result;
        }
        if (target.getRole() == Role.ADMIN && actor.getId().equals(targetUserId)) {
            result.put("success", false);
            result.put("message", "Cannot delete your own admin account.");
            return result;
        }
        usersByUsername.remove(target.getUsername().toLowerCase());
        usersById.remove(targetUserId);
        auditService.log(actor.getUsername(), actor.getRole(), "DELETE_USER", "SUCCESS",
            "Deleted user: " + target.getUsername());
        result.put("success", true);
        result.put("message", "User '" + target.getFullName() + "' has been deleted.");
        return result;
    }

    /**
     * Assign a new role to a user (ADMIN only).
     */
    public Map<String, Object> assignRole(User actor, String targetUserId, Role newRole) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!permissionService.hasPermission(actor.getRole(), Permission.ASSIGN_ROLE)) {
            auditService.log(actor.getUsername(), actor.getRole(), "ASSIGN_ROLE", "DENIED",
                "Attempted to assign role to user: " + targetUserId);
            result.put("success", false);
            result.put("message", "Access Denied: Only Admin can assign roles.");
            return result;
        }
        User target = usersById.get(targetUserId);
        if (target == null) {
            result.put("success", false);
            result.put("message", "User not found.");
            return result;
        }
        Role oldRole = target.getRole();
        target.setRole(newRole);
        auditService.log(actor.getUsername(), actor.getRole(), "ASSIGN_ROLE", "SUCCESS",
            "Changed role of " + target.getUsername() + " from " + oldRole + " to " + newRole);
        result.put("success", true);
        result.put("message", "Role of '" + target.getFullName() + "' changed from "
            + oldRole.getDisplayName() + " to " + newRole.getDisplayName());
        return result;
    }

    /**
     * View all users (ADMIN only).
     */
    public Map<String, Object> getAllUsers(User actor) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!permissionService.hasPermission(actor.getRole(), Permission.VIEW_ALL_USERS)) {
            auditService.log(actor.getUsername(), actor.getRole(), "VIEW_ALL_USERS", "DENIED",
                "Attempted to view all users");
            result.put("success", false);
            result.put("message", "Access Denied: Only Admin can view all users.");
            return result;
        }
        auditService.log(actor.getUsername(), actor.getRole(), "VIEW_ALL_USERS", "GRANTED",
            "Viewed all users list");
        result.put("success", true);
        result.put("users", new ArrayList<>(usersById.values()));
        return result;
    }

    /**
     * Get all student users (for Faculty to pick from when adding marks).
     */
    public List<User> getAllStudents() {
        return usersById.values().stream()
            .filter(u -> u.getRole() == Role.STUDENT)
            .collect(Collectors.toList());
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username.toLowerCase()));
    }

    public Optional<User> findById(String id) {
        return Optional.ofNullable(usersById.get(id));
    }

    public Collection<User> getAllUsersRaw() {
        return usersById.values();
    }
}
