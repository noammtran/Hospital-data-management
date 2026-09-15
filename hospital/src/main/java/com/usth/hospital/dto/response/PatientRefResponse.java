package com.usth.hospital.dto.response;

/** Compact patient context used by the statistics cohort and drill-down list. */
public record PatientRefResponse(
        String id,
        String name,
        String identityNo,
        Integer age,
        Integer gender,
        int status,
        long admissionCount
) {
}
