package com.college.rbac.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Represents student marks stored in the system.
 */
public class Mark {
    private final String id;
    private final String studentId;
    private final String studentName;
    private String subject;
    private int marks;
    private int maxMarks;
    private String grade;
    private String semester;
    private final String addedByFacultyId;
    private final String addedByFacultyName;
    private final LocalDateTime addedAt;
    private LocalDateTime updatedAt;

    public Mark(String studentId, String studentName, String subject,
                int marks, int maxMarks, String semester,
                String facultyId, String facultyName) {
        this.id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.studentId = studentId;
        this.studentName = studentName;
        this.subject = subject;
        this.marks = marks;
        this.maxMarks = maxMarks;
        this.semester = semester;
        this.addedByFacultyId = facultyId;
        this.addedByFacultyName = facultyName;
        this.addedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.grade = computeGrade(marks, maxMarks);
    }

    private String computeGrade(int marks, int maxMarks) {
        double pct = (marks * 100.0) / maxMarks;
        if (pct >= 90) return "O";
        if (pct >= 80) return "A+";
        if (pct >= 70) return "A";
        if (pct >= 60) return "B+";
        if (pct >= 50) return "B";
        if (pct >= 40) return "C";
        return "F";
    }

    // Getters
    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getSubject() { return subject; }
    public int getMarks() { return marks; }
    public int getMaxMarks() { return maxMarks; }
    public String getGrade() { return grade; }
    public String getSemester() { return semester; }
    public String getAddedByFacultyId() { return addedByFacultyId; }
    public String getAddedByFacultyName() { return addedByFacultyName; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public double getPercentage() { return (marks * 100.0) / maxMarks; }

    // Setters for update operations
    public void setMarks(int marks) {
        this.marks = marks;
        this.grade = computeGrade(marks, this.maxMarks);
        this.updatedAt = LocalDateTime.now();
    }

    public void setSubject(String subject) { this.subject = subject; }
    public void setSemester(String semester) { this.semester = semester; }
    public void setMaxMarks(int maxMarks) {
        this.maxMarks = maxMarks;
        this.grade = computeGrade(this.marks, maxMarks);
        this.updatedAt = LocalDateTime.now();
    }

    public String getAddedAtFormatted() {
        return addedAt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
    }

    public String getUpdatedAtFormatted() {
        return updatedAt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
    }

    @Override
    public String toString() {
        return String.format("Mark[id=%s, student=%s, subject=%s, marks=%d/%d, grade=%s]",
            id, studentName, subject, marks, maxMarks, grade);
    }
}
