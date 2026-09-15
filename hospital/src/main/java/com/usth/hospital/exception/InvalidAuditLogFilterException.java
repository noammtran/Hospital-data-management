package com.usth.hospital.exception;

import lombok.Getter;

@Getter
public class InvalidAuditLogFilterException extends RuntimeException {

    private final String field;

    public InvalidAuditLogFilterException(String field, String message) {
        super(message);
        this.field = field;
    }
}
