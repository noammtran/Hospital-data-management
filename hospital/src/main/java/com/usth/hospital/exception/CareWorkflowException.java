package com.usth.hospital.exception;

public class CareWorkflowException extends RuntimeException {
    private final int status;
    public CareWorkflowException(int status, String message) {
        super(message);
        this.status = status;
    }
    public int getStatus() { return status; }
}
