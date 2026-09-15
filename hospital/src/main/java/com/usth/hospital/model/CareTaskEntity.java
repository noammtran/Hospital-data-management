package com.usth.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "CARE_TASKS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareTaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "care_task_seq")
    @SequenceGenerator(name = "care_task_seq", sequenceName = "care_task_seq", allocationSize = 1)
    @Column(name = "care_task_id", nullable = false)
    private Long id;

    @Column(name = "care_task_visit_id", length = 50, nullable = false)
    private String visitId;

    @Column(name = "care_task_point_id", nullable = false)
    private Long pointId;

    @Column(name = "care_task_kind", length = 20, nullable = false)
    private String kind;

    @Column(name = "care_task_service_id", nullable = true)
    private Long serviceId;

    @Column(name = "care_task_status", length = 30, nullable = false)
    private String status;

    @Column(name = "care_task_arrival", length = 20, nullable = false)
    private String arrival;

    @Column(name = "care_task_reason", length = 1000, nullable = true)
    private String reason;

    @Column(name = "care_task_created_by", nullable = false)
    private Long createdBy;

    @Column(name = "care_task_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "care_task_arrived_at", nullable = true)
    private LocalDateTime arrivedAt;

    @Column(name = "care_task_started_at", nullable = true)
    private LocalDateTime startedAt;

    @Column(name = "care_task_performed_by", nullable = true)
    private Long performedBy;

    @Column(name = "care_task_closed_at", nullable = true)
    private LocalDateTime closedAt;

    @Column(name = "care_task_resolution", length = 1000, nullable = true)
    private String resolution;

    @Column(name = "care_task_resolved_by", nullable = true)
    private Long resolvedBy;

    @Column(name = "care_task_resolved_at", nullable = true)
    private LocalDateTime resolvedAt;

    @Column(name = "care_task_request_key", length = 80, nullable = true)
    private String requestKey;

}
