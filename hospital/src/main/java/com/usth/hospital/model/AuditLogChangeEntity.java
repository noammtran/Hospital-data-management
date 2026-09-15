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
@Table(name = "AUDIT_LOG_CHANGES")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogChangeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_change_seq")
    @SequenceGenerator(
            name = "audit_log_change_seq",
            sequenceName = "audit_log_change_seq",
            allocationSize = 1
    )
    @Column(name = "audit_log_change_id", nullable = false)
    private Long id;

    @Column(name = "audit_log_id", nullable = false)
    private Long auditLogId;

    @Column(name = "audit_log_change_field_key", length = 80, nullable = false)
    private String fieldKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_log_change_value_type", length = 20, nullable = false)
    private AuditLogValueType valueType;

    @Column(name = "audit_log_change_old_value", length = 2000)
    private String oldValue;

    @Column(name = "audit_log_change_new_value", length = 2000)
    private String newValue;

    @Column(name = "audit_log_change_old_display", length = 500)
    private String oldDisplayValue;

    @Column(name = "audit_log_change_new_display", length = 500)
    private String newDisplayValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_log_change_sensitivity", length = 20, nullable = false)
    private AuditLogSensitivity sensitivity;

    @Column(name = "audit_log_change_sort_order", nullable = false)
    private Integer displayOrder;
}
