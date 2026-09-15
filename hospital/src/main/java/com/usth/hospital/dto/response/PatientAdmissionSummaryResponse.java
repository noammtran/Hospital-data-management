package com.usth.hospital.dto.response;

public record PatientAdmissionSummaryResponse(
        String admissionId,
        String admissionDate,
        String dischargeDate,
        String departmentName,
        String doctorName,
        String roomName,
        String reason,
        Integer type,
        Integer status,
        String notes
) {
}
