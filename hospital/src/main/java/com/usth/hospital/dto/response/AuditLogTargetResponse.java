package com.usth.hospital.dto.response;

public record AuditLogTargetResponse(
        String entityType,
        String entityId,
        String targetLabel,
        int displayOrder
) {
}
