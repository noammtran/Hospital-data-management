package com.usth.hospital.exception;

public class AdmissionNotFoundException extends RuntimeException {

    public AdmissionNotFoundException(String admissionId, String patientId) {
        super("Admission not found with id " + admissionId + " for patient " + patientId);
    }
}
