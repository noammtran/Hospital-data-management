package com.usth.hospital.exception;

import lombok.Getter;

@Getter
public class InvalidUserOperationException extends RuntimeException {

    private final String field;

    public InvalidUserOperationException(String field, String message) {
        super(message);
        this.field = field;
    }
}
