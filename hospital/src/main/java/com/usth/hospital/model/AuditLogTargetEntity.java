package com.usth.hospital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AUDIT_LOG_TARGETS")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogTargetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_target_seq")
    @SequenceGenerator(
            name = "audit_log_target_seq",
            sequenceName = "audit_log_target_seq",
            allocationSize = 1
    )
    @Column(name = "audit_log_target_id", nullable = false)
    private Long id;

    @Column(name = "audit_log_id", nullable = false)
    private Long auditLogId;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_log_target_entity_type", length = 30, nullable = false)
    private AuditLogEntityType entityType;

    @Column(name = "audit_log_target_entity_id", length = 100)
    private String entityId;

    @Column(name = "audit_log_target_label", length = 200)
    private String targetLabel;

    @Column(name = "audit_log_target_sort_order", nullable = false)
    private Integer displayOrder;
}
