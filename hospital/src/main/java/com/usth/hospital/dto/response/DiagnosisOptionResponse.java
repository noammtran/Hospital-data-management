package com.usth.hospital.dto.response;

/** A selectable diagnosis filter: the ICD code and a human-readable label. */
public record DiagnosisOptionResponse(String icdCode, String label) {
}
