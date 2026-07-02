package com.usth.hospital.dto.response;

import java.time.LocalDate;

public record PatientHistoryResponse(
        String fieldName,
        String oldValue,
        String newValue,
        String changedBy,
        LocalDate changedDate
) {
}
