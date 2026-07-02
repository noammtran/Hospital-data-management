package com.usth.hospital.dto.response;

/** A selectable medicine filter: the medicine id and its name. */
public record MedicineOptionResponse(long medicineId, String label) {
}
