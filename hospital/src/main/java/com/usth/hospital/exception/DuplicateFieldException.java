package com.usth.hospital.exception;

/** Raised when a request would create a duplicate value on a field that must be unique. */
public class DuplicateFieldException extends RuntimeException {

    private final String field;

    public DuplicateFieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
