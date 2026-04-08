package com.college.rbac;

import com.college.rbac.controller.ApiHandler;
import com.college.rbac.service.*;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Main entry point for the College Portal RBAC System.
 * Starts an embedded HTTP server on port 8080.
 */
public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║         COLLEGE PORTAL — ROLE-BASED ACCESS CONTROL           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Initialize services
        AuditService auditService = new AuditService();
        RolePermissionService permissionService = new RolePermissionService();
        UserService userService = new UserService(permissionService, auditService);
        MarksService marksService = new MarksService(permissionService, auditService, userService);

        // Print permission matrix
        System.out.println("📋 PERMISSION MATRIX:");
        for (var role : com.college.rbac.model.Role.values()) {
            System.out.println(permissionService.getRoleSummary(role));
        }

        System.out.println("👤 DEFAULT CREDENTIALS:");
        System.out.println("  Admin   -> username: admin     | password: admin123");
        System.out.println("  Faculty -> username: faculty1  | password: faculty123");
        System.out.println("  Student -> username: student1  | password: student123");
        System.out.println();

        // Create and start HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 50);
        ApiHandler handler = new ApiHandler(userService, marksService, auditService, permissionService);
        server.createContext("/", handler);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        System.out.println("✅ Server started at http://localhost:" + PORT);
        System.out.println("   Press Ctrl+C to stop.\n");
    }
}
