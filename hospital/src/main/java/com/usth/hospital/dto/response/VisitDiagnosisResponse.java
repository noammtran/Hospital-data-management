package com.usth.hospital.dto.response;

/** One diagnosis recorded during a visit. Severity is a lookup code. */
public record VisitDiagnosisResponse(
        String date,
        String icdCode,
        String description,
        Integer severity,
        String doctorName) {
}
