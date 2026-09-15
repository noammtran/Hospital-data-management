package com.usth.hospital.dto.response;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        LocalDateTime occurredAt,
        String actor,
        String action,
        String entityType,
        String entityId,
        String targetLabel,
        String details,
        int eventVersion,
        boolean legacy,
        String actorDisplayName,
        String actorRole,
        String subjectType,
        String subjectId,
        String subjectLabel,
        String summary,
        int changeCount,
        int affectedTargetCount,
        boolean sampleData
) {

    public AuditLogResponse(
            Long id,
            LocalDateTime occurredAt,
            String actor,
            String action,
            String entityType,
            String entityId,
            String targetLabel,
            String details
    ) {
        this(
                id, occurredAt, actor, action, entityType, entityId, targetLabel, details,
                1, true, null, null, null, null, null, details, 0, 0, false
        );
    }
}
