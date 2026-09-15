package com.usth.hospital.dto.response;

public record AuditLogChangeResponse(
        String fieldKey,
        String valueType,
        String oldValue,
        String newValue,
        String oldDisplayValue,
        String newDisplayValue,
        String sensitivity,
        int displayOrder
) {
}
