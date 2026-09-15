package com.usth.hospital.exception;

public class PatientStatusConflictException extends RuntimeException {

    public PatientStatusConflictException(String message) {
        super(message);
    }
}
