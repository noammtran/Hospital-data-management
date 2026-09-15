package com.usth.hospital.exception;

import lombok.Getter;

@Getter
public class InvalidAdmissionOperationException extends RuntimeException {

    private final String field;

    public InvalidAdmissionOperationException(String field, String message) {
        super(message);
        this.field = field;
    }
}
