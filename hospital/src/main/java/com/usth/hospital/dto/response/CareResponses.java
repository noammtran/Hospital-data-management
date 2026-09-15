package com.usth.hospital.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public final class CareResponses {
    private CareResponses() {}
    public record Point(Long id, String code, String name, String kind, Long departmentId, boolean active) {}
    public record Service(Long id, String code, String name, Long pointId, boolean active) {}
    public record Staff(Long id, String name, String role, Long doctorId, boolean active) {}
    public record Assignment(Long id, Long userId, Long pointId, boolean perform, boolean verify, boolean active) {}
    public record Reference(Long id, String name) {}
    public record Room(Long id, String name, Long departmentId, int availableBeds) {}
    public record Options(Long currentUserId, List<Point> points, List<Service> services,
                          List<Staff> staff, List<Assignment> assignments, List<Reference> departments,
                          List<Reference> doctors, List<Room> rooms) {}
    public record Inpatient(String admissionId, String patientId, String patientName, int patientStatus,
                            String departmentName, String doctorName, String roomName,
                            Long departmentId, String visitId) {}
    public record Visit(String id, String patientId, String patientName, int patientStatus,
                        String status, String progress, Long pointId, String pointName,
                        Long locationPointId, String locationName, Long ownerId, String ownerName,
                        int priority, LocalDateTime createdAt, LocalDateTime closedAt,
                        int orderCount, int publishedCount, int unreadCount, boolean critical,
                        String admissionId, Long wardPointId) {}
    public record Task(Long id, String visitId, String patientId, String patientName, int patientStatus,
                       Long pointId, String pointName, String kind, Long serviceId, String serviceName,
                       String status, String arrival, String reason, Long createdBy, String orderedBy,
                       LocalDateTime createdAt, LocalDateTime arrivedAt, LocalDateTime startedAt,
                       String performedBy, String resolution, LocalDateTime resolvedAt,
                       List<Result> results, List<String> actions) {}
    public record Result(Long id, int revision, String body, boolean critical, String correctionReason,
                         String createdBy, LocalDateTime createdAt, String publishedBy,
                         LocalDateTime publishedAt, String readBy, LocalDateTime readAt) {}
    public record Note(Long id, String kind, String body, String createdBy, LocalDateTime createdAt) {}
    public record Handoff(Long id, String kind, String status, String fromUser, Long toUserId,
                          String toUser, Long pointId, String pointName, String reason, String response,
                          LocalDateTime createdAt, LocalDateTime respondedAt, boolean canRespond) {}
    public record Prescription(String id, String medicineName, String dosage, String frequency,
                               String duration, String instructions) {}
    public record Diagnosis(String id, String description, String icdCode, String doctor,
                            List<Prescription> prescriptions) {}
    public record Detail(Visit visit, String complaint, String conclusion, String plan, String outcome,
                         String followupPlan, LocalDateTime followupAt, String followupOwner,
                         List<Task> tasks, List<Note> notes, List<Handoff> handoffs,
                         List<Diagnosis> diagnoses, List<String> actions) {}
}
