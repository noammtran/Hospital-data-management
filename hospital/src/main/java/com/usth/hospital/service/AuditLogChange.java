package com.usth.hospital.service;

import com.usth.hospital.model.AuditLogSensitivity;
import com.usth.hospital.model.AuditLogValueType;

public record AuditLogChange(
        String fieldKey,
        AuditLogValueType valueType,
        String oldValue,
        String newValue,
        String oldDisplayValue,
        String newDisplayValue,
        AuditLogSensitivity sensitivity,
        int displayOrder
) {

    public static AuditLogChange general(
            String fieldKey,
            AuditLogValueType valueType,
            Object oldValue,
            Object newValue,
            int displayOrder
    ) {
        return generalReference(fieldKey, valueType, oldValue, newValue, oldValue, newValue, displayOrder);
    }

    public static AuditLogChange generalReference(
            String fieldKey,
            AuditLogValueType valueType,
            Object oldValue,
            Object newValue,
            Object oldDisplayValue,
            Object newDisplayValue,
            int displayOrder
    ) {
        return new AuditLogChange(
                fieldKey,
                valueType,
                text(oldValue),
                text(newValue),
                text(oldDisplayValue),
                text(newDisplayValue),
                AuditLogSensitivity.GENERAL,
                displayOrder
        );
    }

    public static AuditLogChange personal(
            String fieldKey,
            AuditLogValueType valueType,
            Object oldValue,
            Object newValue,
            int displayOrder
    ) {
        return classified(
                fieldKey, valueType, oldValue, newValue, AuditLogSensitivity.PERSONAL, displayOrder);
    }

    public static AuditLogChange medical(
            String fieldKey,
            AuditLogValueType valueType,
            Object oldValue,
            Object newValue,
            int displayOrder
    ) {
        return classified(
                fieldKey, valueType, oldValue, newValue, AuditLogSensitivity.MEDICAL, displayOrder);
    }

    public static AuditLogChange secret(String fieldKey, int displayOrder) {
        return new AuditLogChange(
                fieldKey,
                AuditLogValueType.TEXT,
                null,
                null,
                null,
                null,
                AuditLogSensitivity.SECRET,
                displayOrder
        );
    }

    private static AuditLogChange classified(
            String fieldKey,
            AuditLogValueType valueType,
            Object oldValue,
            Object newValue,
            AuditLogSensitivity sensitivity,
            int displayOrder
    ) {
        return new AuditLogChange(
                fieldKey,
                valueType,
                text(oldValue),
                text(newValue),
                text(oldValue),
                text(newValue),
                sensitivity,
                displayOrder
        );
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
