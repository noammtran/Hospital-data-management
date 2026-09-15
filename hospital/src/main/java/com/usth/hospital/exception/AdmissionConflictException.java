package com.usth.hospital.exception;

public class AdmissionConflictException extends RuntimeException {

    public AdmissionConflictException(String message) {
        super(message);
    }

    public AdmissionConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
