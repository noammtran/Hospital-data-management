package com.usth.hospital.exception;

public class AuditLogNotFoundException extends RuntimeException {

    public AuditLogNotFoundException(Long id) {
        super("Audit event not found: " + id);
    }
}
