package com.usth.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "CARE_RESULTS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "care_result_seq")
    @SequenceGenerator(name = "care_result_seq", sequenceName = "care_result_seq", allocationSize = 1)
    @Column(name = "care_result_id", nullable = false)
    private Long id;

    @Column(name = "care_result_task_id", nullable = false)
    private Long taskId;

    @Column(name = "care_result_revision", nullable = false)
    private Integer revision;

    @Lob
    @Column(name = "care_result_body", nullable = false)
    private String body;

    @Column(name = "care_result_critical", nullable = false)
    private Integer critical;

    @Column(name = "care_result_correction", length = 1000, nullable = true)
    private String correctionReason;

    @Column(name = "care_result_created_by", nullable = false)
    private Long createdBy;

    @Column(name = "care_result_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "care_result_published_by", nullable = true)
    private Long publishedBy;

    @Column(name = "care_result_published_at", nullable = true)
    private LocalDateTime publishedAt;

    @Column(name = "care_result_read_by", nullable = true)
    private Long readBy;

    @Column(name = "care_result_read_at", nullable = true)
    private LocalDateTime readAt;

}
