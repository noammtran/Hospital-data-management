package com.usth.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "CARE_VISITS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareVisitEntity {
    @Id
    @Column(name = "care_visit_id", length = 50, nullable = false)
    private String id;

    @Column(name = "care_visit_patient_id", length = 50, nullable = false)
    private String patientId;

    @Column(name = "care_visit_status", length = 30, nullable = false)
    private String status;

    @Column(name = "care_visit_previous_status", nullable = false)
    private Integer previousPatientStatus;

    @Column(name = "care_visit_original_point", nullable = false)
    private Long originalPointId;

    @Column(name = "care_visit_point_id", nullable = false)
    private Long pointId;

    @Column(name = "care_visit_location_point", nullable = true)
    private Long locationPointId;

    @Column(name = "care_visit_owner_id", nullable = true)
    private Long ownerId;

    @Column(name = "care_visit_complaint", length = 500, nullable = false)
    private String complaint;

    @Column(name = "care_visit_priority", nullable = false)
    private Integer priority;

    @Column(name = "care_visit_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "care_visit_created_by", nullable = false)
    private Long createdBy;

    @Column(name = "care_visit_started_at", nullable = true)
    private LocalDateTime startedAt;

    @Column(name = "care_visit_closed_at", nullable = true)
    private LocalDateTime closedAt;

    @Lob
    @Column(name = "care_visit_conclusion", nullable = true)
    private String conclusion;

    @Lob
    @Column(name = "care_visit_plan", nullable = true)
    private String plan;

    @Column(name = "care_visit_outcome", length = 30, nullable = true)
    private String outcome;

    @Column(name = "care_visit_followup_plan", length = 1000, nullable = true)
    private String followupPlan;

    @Column(name = "care_visit_followup_at", nullable = true)
    private LocalDateTime followupAt;

    @Column(name = "care_visit_followup_owner", nullable = true)
    private Long followupOwnerId;

    @Column(name = "care_visit_admission_id", length = 50, nullable = true)
    private String admissionId;

    @Column(name = "care_visit_ward_point", nullable = true)
    private Long wardPointId;

}
