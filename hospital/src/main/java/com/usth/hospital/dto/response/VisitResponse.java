package com.usth.hospital.dto.response;

import java.util.List;

/** One treatment episode (visit) with the diagnoses and prescriptions it holds. */
public record VisitResponse(
        String admissionId,
        String admissionDate,
        String dischargeDate,
        String departmentName,
        String doctorName,
        String roomName,
        String reason,
        Integer type,
        Integer status,
        String notes,
        List<VisitDiagnosisResponse> diagnoses,
        List<VisitPrescriptionResponse> prescriptions) {
}
