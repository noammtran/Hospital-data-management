package com.usth.hospital.dto.response;

import java.time.LocalDate;

public record PatientListResponse(
        String id,
        String name,
        Integer gender,
        LocalDate birthDate,
        String phoneNo,
        Integer status,
        boolean hasHistory
) {
}
