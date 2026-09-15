package com.usth.hospital.service;

import com.usth.hospital.model.AuditLogEntityType;

public record AuditLogTarget(
        AuditLogEntityType entityType,
        String entityId,
        String targetLabel,
        int displayOrder
) {
}
