package com.usth.hospital.dto.response;

import java.time.LocalDate;

public record AdmissionResponse(
        String admissionId,
        String patientId,
        Long departmentId,
        Long doctorId,
        Long roomId,
        LocalDate admissionDate,
        LocalDate dischargeDate,
        String reason,
        Integer type,
        Integer status,
        String notes
) {
}
