package com.usth.hospital.exception;

import lombok.Getter;

@Getter
public class InvalidStatisticsFilterException extends RuntimeException {

    private final String field;

    public InvalidStatisticsFilterException(String field, String message) {
        super(message);
        this.field = field;
    }
}
