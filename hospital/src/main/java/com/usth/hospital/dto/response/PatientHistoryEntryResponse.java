package com.usth.hospital.dto.response;

import java.time.LocalDate;

public record PatientHistoryEntryResponse(
        String patientId,
        String patientName,
        String fieldName,
        String oldValue,
        String newValue,
        String changedBy,
        LocalDate changedDate
) {
}
