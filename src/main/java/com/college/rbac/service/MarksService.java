package com.college.rbac.service;

import com.college.rbac.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages student marks with RBAC enforcement.
 * Faculty can add/update; Students can only view their own; Admin can view all.
 */
public class MarksService {

    private final Map<String, Mark> marksById = new ConcurrentHashMap<>();
    private final RolePermissionService permissionService;
    private final AuditService auditService;

    public MarksService(RolePermissionService permissionService, AuditService auditService,
                        UserService userService) {
        this.permissionService = permissionService;
        this.auditService = auditService;
        seedMarks(userService);
    }

    /**
     * Seeds sample marks data for demonstration.
     */
    private void seedMarks(UserService userService) {
        List<User> students = userService.getAllStudents();
        if (students.isEmpty()) return;

        String[] subjects = {"Mathematics", "Physics", "Chemistry", "Computer Science", "English"};
        int[][] markData = {
            {88, 76, 91, 95, 82},
            {72, 68, 79, 85, 74},
            {65, 71, 58, 90, 63}
        };

        for (int i = 0; i < Math.min(students.size(), 3); i++) {
            User student = students.get(i);
            for (int j = 0; j < subjects.length; j++) {
                addMarkInternal(student.getId(), student.getFullName(),
                    subjects[j], markData[i][j], 100, "Semester 1",
                    "SYSTEM", "System Seeder");
            }
        }
    }

    private Mark addMarkInternal(String studentId, String studentName, String subject,
                                  int marks, int maxMarks, String semester,
                                  String facultyId, String facultyName) {
        Mark mark = new Mark(studentId, studentName, subject, marks, maxMarks,
            semester, facultyId, facultyName);
        marksById.put(mark.getId(), mark);
        return mark;
    }

    /**
     * Add marks for a student (FACULTY only).
     */
    public Map<String, Object> addMarks(User actor, String studentId, String studentName,
                                         String subject, int marks, int maxMarks,
                                         String semester) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!permissionService.hasPermission(actor.getRole(), Permission.ADD_MARKS)) {
            auditService.log(actor.getUsername(), actor.getRole(), "ADD_MARKS", "DENIED",
                "Attempted to add marks for student: " + studentName);
            result.put("success", false);
            result.put("message", "Access Denied: Only Faculty can add student marks. " +
                "Your role '" + actor.getRole().getDisplayName() + "' is not authorized.");
            return result;
        }
        if (marks < 0 || marks > maxMarks) {
            result.put("success", false);
            result.put("message", "Invalid marks: " + marks + " (must be between 0 and " + maxMarks + ")");
            return result;
        }
        Mark mark = addMarkInternal(studentId, studentName, subject, marks, maxMarks,
            semester, actor.getId(), actor.getFullName());
        auditService.log(actor.getUsername(), actor.getRole(), "ADD_MARKS", "SUCCESS",
            "Added " + subject + " marks (" + marks + "/" + maxMarks + ") for " + studentName);
        result.put("success", true);
        result.put("message", "Marks added for " + studentName + " in " + subject +
            ": " + marks + "/" + maxMarks + " (Grade: " + mark.getGrade() + ")");
        result.put("mark", mark);
        return result;
    }

    /**
     * Update existing marks (FACULTY only).
     */
    public Map<String, Object> updateMarks(User actor, String markId, int newMarks) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!permissionService.hasPermission(actor.getRole(), Permission.UPDATE_MARKS)) {
            auditService.log(actor.getUsername(), actor.getRole(), "UPDATE_MARKS", "DENIED",
                "Attempted to update marks ID: " + markId);
            result.put("success", false);
            result.put("message", "Access Denied: Only Faculty can update marks.");
            return result;
        }
        Mark mark = marksById.get(markId);
        if (mark == null) {
            result.put("success", false);
            result.put("message", "Mark record not found with ID: " + markId);
            return result;
        }
        int oldMarks = mark.getMarks();
        mark.setMarks(newMarks);
        auditService.log(actor.getUsername(), actor.getRole(), "UPDATE_MARKS", "SUCCESS",
            "Updated " + mark.getSubject() + " marks for " + mark.getStudentName() +
                " from " + oldMarks + " to " + newMarks);
        result.put("success", true);
        result.put("message", "Updated " + mark.getSubject() + " marks for " +
            mark.getStudentName() + ": " + oldMarks + " → " + newMarks +
            " (New Grade: " + mark.getGrade() + ")");
        result.put("mark", mark);
        return result;
    }

    /**
     * View all marks (ADMIN or FACULTY only).
     */
    public Map<String, Object> getAllMarks(User actor) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!permissionService.hasPermission(actor.getRole(), Permission.VIEW_ALL_MARKS)) {
            auditService.log(actor.getUsername(), actor.getRole(), "VIEW_ALL_MARKS", "DENIED",
                "Attempted to view all marks");
            result.put("success", false);
            result.put("message", "Access Denied: Only Admin and Faculty can view all marks.");
            return result;
        }
        auditService.log(actor.getUsername(), actor.getRole(), "VIEW_ALL_MARKS", "GRANTED",
            "Viewed all marks");
        result.put("success", true);
        result.put("marks", new ArrayList<>(marksById.values()));
        return result;
    }

    /**
     * A student can only view their own marks.
     */
    public Map<String, Object> getOwnMarks(User actor) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!permissionService.hasPermission(actor.getRole(), Permission.VIEW_OWN_MARKS)) {
            auditService.log(actor.getUsername(), actor.getRole(), "VIEW_OWN_MARKS", "DENIED",
                "Attempted to view own marks");
            result.put("success", false);
            result.put("message", "Access Denied.");
            return result;
        }
        List<Mark> ownMarks = marksById.values().stream()
            .filter(m -> m.getStudentId().equals(actor.getId()))
            .collect(Collectors.toList());
        auditService.log(actor.getUsername(), actor.getRole(), "VIEW_OWN_MARKS", "GRANTED",
            "Viewed own marks (" + ownMarks.size() + " records)");
        result.put("success", true);
        result.put("marks", ownMarks);
        return result;
    }

    public Optional<Mark> findById(String id) {
        return Optional.ofNullable(marksById.get(id));
    }

    public List<Mark> getMarksByStudentId(String studentId) {
        return marksById.values().stream()
            .filter(m -> m.getStudentId().equals(studentId))
            .collect(Collectors.toList());
    }

    public Collection<Mark> getAllMarksRaw() {
        return marksById.values();
    }
}
