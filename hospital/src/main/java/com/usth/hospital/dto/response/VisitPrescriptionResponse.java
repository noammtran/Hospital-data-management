package com.usth.hospital.dto.response;

/** One prescription written during a visit. */
public record VisitPrescriptionResponse(
        String medicineName,
        String dosage,
        String frequency,
        String duration,
        String instructions) {
}
