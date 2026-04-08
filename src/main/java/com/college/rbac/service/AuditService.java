package com.college.rbac.service;

import com.college.rbac.model.AuditLog;
import com.college.rbac.model.Role;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks all RBAC actions for security auditing.
 */
public class AuditService {

    private final List<AuditLog> logs = new ArrayList<>();

    public synchronized void log(String actor, Role role, String action,
                                  String result, String details) {
        AuditLog entry = new AuditLog(actor, role, action, result, details);
        logs.add(entry);
        System.out.println("[AUDIT] " + entry);
    }

    public List<AuditLog> getAllLogs() {
        return Collections.unmodifiableList(logs);
    }

    public List<AuditLog> getLogsForUser(String username) {
        return logs.stream()
            .filter(l -> l.getActorUsername().equalsIgnoreCase(username))
            .toList();
    }

    public List<AuditLog> getRecentLogs(int count) {
        int size = logs.size();
        return Collections.unmodifiableList(
            logs.subList(Math.max(0, size - count), size)
        );
    }

    public long countDenied() {
        return logs.stream().filter(l -> "DENIED".equalsIgnoreCase(l.getResult())).count();
    }

    public long countGranted() {
        return logs.stream().filter(l -> l.isSuccess()).count();
    }
}
