package com.college.rbac.controller;

import com.college.rbac.model.*;
import com.college.rbac.service.*;
import com.college.rbac.util.JsonBuilder;
import com.sun.net.httpserver.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HTTP Request handler — routes requests to appropriate services.
 * Handles REST API endpoints and serves static web files.
 */
public class ApiHandler implements HttpHandler {

    private final UserService userService;
    private final MarksService marksService;
    private final AuditService auditService;
    private final RolePermissionService permissionService;

    // Session store: sessionToken -> User (in-memory)
    private final Map<String, User> sessions = new java.util.concurrent.ConcurrentHashMap<>();

    public ApiHandler(UserService userService, MarksService marksService,
                      AuditService auditService, RolePermissionService permissionService) {
        this.userService = userService;
        this.marksService = marksService;
        this.auditService = auditService;
        this.permissionService = permissionService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // CORS headers
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        try {
            if (path.startsWith("/api/")) {
                handleApi(exchange, path, method);
            } else {
                serveStaticFile(exchange, path);
            }
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage() != null ? e.getMessage() : "Internal error"));
        }
    }

    private void handleApi(HttpExchange exchange, String path, String method) throws IOException {
        // Routes
        switch (path) {
            case "/api/login" -> handleLogin(exchange);
            case "/api/logout" -> handleLogout(exchange);
            case "/api/me" -> handleGetMe(exchange);
            case "/api/users" -> {
                if ("GET".equals(method)) handleGetUsers(exchange);
                else if ("POST".equals(method)) handleCreateUser(exchange);
                else sendNotFound(exchange);
            }
            case "/api/marks" -> {
                if ("GET".equals(method)) handleGetMarks(exchange);
                else if ("POST".equals(method)) handleAddMarks(exchange);
                else sendNotFound(exchange);
            }
            case "/api/marks/own" -> handleGetOwnMarks(exchange);
            case "/api/permissions" -> handleGetPermissions(exchange);
            case "/api/audit" -> handleGetAudit(exchange);
            case "/api/stats" -> handleGetStats(exchange);
            default -> {
                if (path.startsWith("/api/users/") && "DELETE".equals(method)) {
                    handleDeleteUser(exchange, path);
                } else if (path.startsWith("/api/users/") && "PUT".equals(method)) {
                    handleAssignRole(exchange, path);
                } else if (path.startsWith("/api/marks/") && "PUT".equals(method)) {
                    handleUpdateMarks(exchange, path);
                } else {
                    sendNotFound(exchange);
                }
            }
        }
    }

    // ─────────── AUTH ───────────

    private void handleLogin(HttpExchange ex) throws IOException {
        Map<String, String> body = parseBody(ex);
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");

        Optional<User> userOpt = userService.authenticate(username, password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String token = UUID.randomUUID().toString();
            sessions.put(token, user);
            Set<Permission> perms = permissionService.getPermissionsForRole(user.getRole());
            sendJson(ex, 200, Map.of(
                "success", true,
                "token", token,
                "user", userToMap(user),
                "permissions", perms.stream().map(Permission::name).collect(Collectors.toList())
            ));
        } else {
            sendJson(ex, 401, Map.of("success", false, "message", "Invalid username or password."));
        }
    }

    private void handleLogout(HttpExchange ex) throws IOException {
        String token = getToken(ex);
        if (token != null) sessions.remove(token);
        sendJson(ex, 200, Map.of("success", true, "message", "Logged out successfully."));
    }

    private void handleGetMe(HttpExchange ex) throws IOException {
        User user = getAuthUser(ex);
        if (user == null) { sendUnauthorized(ex); return; }
        Set<Permission> perms = permissionService.getPermissionsForRole(user.getRole());
        sendJson(ex, 200, Map.of(
            "user", userToMap(user),
            "permissions", perms.stream().map(Permission::name).collect(Collectors.toList()),
            "roleDescription", user.getRole().getDescription()
        ));
    }

    // ─────────── USERS ───────────

    private void handleGetUsers(HttpExchange ex) throws IOException {
        User actor = getAuthUser(ex);
        if (actor == null) { sendUnauthorized(ex); return; }
        Map<String, Object> result = userService.getAllUsers(actor);
        if (Boolean.TRUE.equals(result.get("success"))) {
            @SuppressWarnings("unchecked")
            List<User> users = (List<User>) result.get("users");
            sendJson(ex, 200, Map.of(
                "success", true,
                "users", users.stream().map(this::userToMap).collect(Collectors.toList())
            ));
        } else {
            sendJson(ex, 403, result);
        }
    }

    private void handleCreateUser(HttpExchange ex) throws IOException {
        User actor = getAuthUser(ex);
        if (actor == null) { sendUnauthorized(ex); return; }
        Map<String, String> body = parseBody(ex);
        Role role = parseRole(body.getOrDefault("role", "STUDENT"));
        Map<String, Object> res = userService.createUser(actor,
            body.getOrDefault("username", ""),
            body.getOrDefault("password", "pass123"),
            body.getOrDefault("fullName", ""),
            body.getOrDefault("email", ""),
            role
        );
        int code = Boolean.TRUE.equals(res.get("success")) ? 201 : 403;
        if (res.containsKey("user")) {
            User u = (User) res.get("user");
            res.put("user", userToMap(u));
        }
        sendJson(ex, code, res);
    }

    private void handleDeleteUser(HttpExchange ex, String path) throws IOException {
        User actor = getAuthUser(ex);
        if (actor == null) { sendUnauthorized(ex); return; }
        String userId = path.replace("/api/users/", "");
        Map<String, Object> res = userService.deleteUser(actor, userId);
        int code = Boolean.TRUE.equals(res.get("success")) ? 200 : 403;
        sendJson(ex, code, res);
    }

    private void handleAssignRole(HttpExchange ex, String path) throws IOException {
        User actor = getAuthUser(ex);
        if (actor == null) { sendUnauthorized(ex); return; }
        String userId = path.replace("/api/users/", "").replace("/role", "");
        Map<String, String> body = parseBody(ex);
        Role newRole = parseRole(body.getOrDefault("role", "STUDENT"));
        Map<String, Object> res = userService.assignRole(actor, userId, newRole);
        int code = Boolean.TRUE.equals(res.get("success")) ? 200 : 403;
        sendJson(ex, code, res);
    }

    // ─────────── MARKS ───────────

    private void handleGetMarks(HttpExchange ex) throws IOException {
        User actor = getAuthUser(ex);
        if (actor == null) { sendUnauthorized(ex); return; }
        Map<String, Object> result = marksService.getAllMarks(actor);
        if (Boolean.TRUE.equals(result.get("success"))) {
            @SuppressWarnings("unchecked")
            List<Mark> marks = (List<Mark>) result.get("marks");
            sendJson(ex, 200, Map.of(
                "success", true,
                "marks", marks.stream().map(this::markToMap).collect(Collectors.toList())
            ));
        } else {
            sendJson(ex, 403, result);
        }
    }

    private void handleGetOwnMarks(HttpExchange ex) throws IOException {
        User actor = getAuthUser(ex);
        if (actor == null) { sendUnauthorized(ex); return; }
        Map<String, Object> result = marksService.getOwnMarks(actor);
        if (Boolean.TRUE.equals(result.get("success"))) {
            @SuppressWarnings("unchecked")
            List<Mark> marks = (List<Mark>) result.get("marks");
            sendJson(ex, 200, Map.of(
                "success", true,
                "marks", marks.stream().map(this::markToMap).collect(Collectors.toList())
            ));
        } else {
            sendJson(ex, 403, result);
        }
    }

    private void handleAddMarks(HttpExchange ex) throws IOException {
        User actor = getAuthUser(ex);
        if (actor == null) { sendUnauthorized(ex); return; }
        Map<String, String> body = parseBody(ex);
        String studentId = body.getOrDefault("studentId", "");
        String studentName = body.getOrDefault("studentName", "");
        String subject = body.getOrDefault("subject", "");
        int marks = parseInt(body.getOrDefault("marks", "0"));
        int maxMarks = parseInt(body.getOrDefault("maxMarks", "100"));
        String semester = body.getOrDefault("semester", "Semester 1");

        Map<String, Object> res = marksService.addMarks(actor, studentId, studentName,
            subject, marks, maxMarks, semester);
        int code = Boolean.TRUE.equals(res.get("success")) ? 201 : 403;
        if (res.containsKey("mark")) {
            res.put("mark", markToMap((Mark) res.get("mark")));
        }
        sendJson(ex, code, res);
    }

    private void handleUpdateMarks(HttpExchange ex, String path) throws IOException {
        User actor = getAuthUser(ex);
        if (actor == null) { sendUnauthorized(ex); return; }
        String markId = path.replace("/api/marks/", "");
        Map<String, String> body = parseBody(ex);
        int newMarks = parseInt(body.getOrDefault("marks", "0"));
        Map<String, Object> res = marksService.updateMarks(actor, markId, newMarks);
        int code = Boolean.TRUE.equals(res.get("success")) ? 200 : 403;
        if (res.containsKey("mark")) {
            res.put("mark", markToMap((Mark) res.get("mark")));
        }
        sendJson(ex, code, res);
    }

    // ─────────── MISC ───────────

    private void handleGetPermissions(HttpExchange ex) throws IOException {
        List<Map<String, Object>> roleList = new ArrayList<>();
        for (Role role : Role.values()) {
            Set<Permission> perms = permissionService.getPermissionsForRole(role);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("role", role.name());
            entry.put("displayName", role.getDisplayName());
            entry.put("description", role.getDescription());
            entry.put("permissions", perms.stream().map(p -> {
                Map<String, String> pm = new LinkedHashMap<>();
                pm.put("name", p.name());
                pm.put("displayName", p.getDisplayName());
                pm.put("description", p.getDescription());
                return pm;
            }).collect(Collectors.toList()));
            roleList.add(entry);
        }
        sendJson(ex, 200, Map.of("roles", roleList));
    }

    private void handleGetAudit(HttpExchange ex) throws IOException {
        User actor = getAuthUser(ex);
        if (actor == null) { sendUnauthorized(ex); return; }
        if (actor.getRole() != Role.ADMIN) {
            sendJson(ex, 403, Map.of("success", false, "message", "Only Admin can view audit logs."));
            return;
        }
        List<AuditLog> logs = auditService.getAllLogs();
        List<Map<String, Object>> logList = logs.stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("timestamp", l.getTimestampFormatted());
            m.put("actor", l.getActorUsername());
            m.put("role", l.getActorRole() != null ? l.getActorRole().getDisplayName() : "Unknown");
            m.put("action", l.getAction());
            m.put("result", l.getResult());
            m.put("details", l.getDetails());
            m.put("success", l.isSuccess());
            return m;
        }).collect(Collectors.toList());
        Collections.reverse(logList);
        sendJson(ex, 200, Map.of("logs", logList));
    }

    private void handleGetStats(HttpExchange ex) throws IOException {
        User actor = getAuthUser(ex);
        if (actor == null) { sendUnauthorized(ex); return; }
        long totalUsers = userService.getAllUsersRaw().size();
        long adminCount = userService.getAllUsersRaw().stream().filter(u -> u.getRole() == Role.ADMIN).count();
        long facultyCount = userService.getAllUsersRaw().stream().filter(u -> u.getRole() == Role.FACULTY).count();
        long studentCount = userService.getAllUsersRaw().stream().filter(u -> u.getRole() == Role.STUDENT).count();
        long totalMarks = marksService.getAllMarksRaw().size();
        long auditGranted = auditService.countGranted();
        long auditDenied = auditService.countDenied();

        // Also send students list for Faculty to pick from
        List<Map<String, Object>> students = userService.getAllStudents().stream()
            .map(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.getId());
                m.put("fullName", s.getFullName());
                m.put("username", s.getUsername());
                return m;
            }).collect(Collectors.toList());

        sendJson(ex, 200, Map.of(
            "totalUsers", totalUsers,
            "adminCount", adminCount,
            "facultyCount", facultyCount,
            "studentCount", studentCount,
            "totalMarks", totalMarks,
            "auditGranted", auditGranted,
            "auditDenied", auditDenied,
            "students", students
        ));
    }

    // ─────────── STATIC FILES ───────────

    private void serveStaticFile(HttpExchange ex, String path) throws IOException {
        if (path.equals("/") || path.isEmpty()) path = "/index.html";
        String resourcePath = "/web" + path;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                // Fallback: serve index.html for SPA routing
                try (InputStream idx = getClass().getResourceAsStream("/web/index.html")) {
                    if (idx == null) { sendNotFound(ex); return; }
                    byte[] data = idx.readAllBytes();
                    ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                    ex.sendResponseHeaders(200, data.length);
                    ex.getResponseBody().write(data);
                }
                return;
            }
            byte[] data = is.readAllBytes();
            ex.getResponseHeaders().add("Content-Type", getMimeType(path));
            ex.sendResponseHeaders(200, data.length);
            ex.getResponseBody().write(data);
        } finally {
            ex.getResponseBody().close();
        }
    }

    // ─────────── HELPERS ───────────

    private Map<String, Object> userToMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("fullName", u.getFullName());
        m.put("email", u.getEmail());
        m.put("role", u.getRole().name());
        m.put("roleDisplayName", u.getRole().getDisplayName());
        m.put("active", u.isActive());
        m.put("createdAt", u.getCreatedAtFormatted());
        m.put("lastLogin", u.getLastLoginFormatted());
        return m;
    }

    private Map<String, Object> markToMap(Mark mk) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", mk.getId());
        m.put("studentId", mk.getStudentId());
        m.put("studentName", mk.getStudentName());
        m.put("subject", mk.getSubject());
        m.put("marks", mk.getMarks());
        m.put("maxMarks", mk.getMaxMarks());
        m.put("grade", mk.getGrade());
        m.put("percentage", Math.round(mk.getPercentage() * 10.0) / 10.0);
        m.put("semester", mk.getSemester());
        m.put("addedBy", mk.getAddedByFacultyName());
        m.put("addedAt", mk.getAddedAtFormatted());
        m.put("updatedAt", mk.getUpdatedAtFormatted());
        return m;
    }

    private void sendJson(HttpExchange ex, int code, Map<String, Object> data) throws IOException {
        byte[] bytes = JsonBuilder.object(data).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    private void sendNotFound(HttpExchange ex) throws IOException {
        sendJson(ex, 404, Map.of("error", "Not found"));
    }

    private void sendUnauthorized(HttpExchange ex) throws IOException {
        sendJson(ex, 401, Map.of("error", "Unauthorized. Please log in."));
    }

    private String getToken(HttpExchange ex) {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        // Also check query param
        String query = ex.getRequestURI().getQuery();
        if (query != null) {
            for (String part : query.split("&")) {
                if (part.startsWith("token=")) return part.substring(6);
            }
        }
        return null;
    }

    private User getAuthUser(HttpExchange ex) {
        String token = getToken(ex);
        if (token == null) return null;
        return sessions.get(token);
    }

    private Map<String, String> parseBody(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return JsonBuilder.parseFormBody(body);
    }

    private Role parseRole(String roleStr) {
        try { return Role.valueOf(roleStr.toUpperCase()); }
        catch (Exception e) { return Role.STUDENT; }
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { return 0; }
    }

    private String getMimeType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css"))  return "text/css; charset=utf-8";
        if (path.endsWith(".js"))   return "application/javascript; charset=utf-8";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".svg"))  return "image/svg+xml";
        return "application/octet-stream";
    }
}
