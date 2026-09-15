package com.usth.hospital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.type.NumericBooleanConverter;

@Entity
@Table(name = "AUDIT_LOGS")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_seq")
    @SequenceGenerator(name = "audit_log_seq", sequenceName = "audit_log_seq", allocationSize = 1)
    @Column(name = "audit_log_id", nullable = false)
    private Long id;

    @Column(name = "audit_log_occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "audit_log_actor", length = 30, nullable = false)
    private String actor;

    @Column(name = "audit_log_actor_display_name", length = 100)
    private String actorDisplayName;

    @Column(name = "audit_log_actor_role", length = 20)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_log_action", length = 30, nullable = false)
    private AuditLogAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_log_entity_type", length = 30, nullable = false)
    private AuditLogEntityType entityType;

    @Column(name = "audit_log_entity_id", length = 100)
    private String entityId;

    @Column(name = "audit_log_target_label", length = 200)
    private String targetLabel;

    @Column(name = "audit_log_details", length = 1000)
    private String details;

    @Builder.Default
    @Column(name = "audit_log_event_version", nullable = false)
    private Integer eventVersion = 1;

    @Builder.Default
    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "audit_log_sample_flag", nullable = false)
    private boolean sampleData = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_log_subject_type", length = 30)
    private AuditLogEntityType subjectType;

    @Column(name = "audit_log_subject_id", length = 100)
    private String subjectId;

    @Column(name = "audit_log_subject_label", length = 200)
    private String subjectLabel;

    @Column(name = "audit_log_summary", length = 1000)
    private String summary;

    @Builder.Default
    @Column(name = "audit_log_change_count", nullable = false)
    private Integer changeCount = 0;

    @Builder.Default
    @Column(name = "audit_log_target_count", nullable = false)
    private Integer affectedTargetCount = 0;
}
