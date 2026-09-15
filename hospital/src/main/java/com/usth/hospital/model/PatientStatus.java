package com.usth.hospital.model;

import java.util.Arrays;

public enum PatientStatus {
    WAITING(0, "Waiting"),
    EXAMINING(1, "Examining"),
    ADMITTED(2, "Admitted"),
    DISCHARGED(3, "Discharged");

    private final int code;
    private final String displayName;

    PatientStatus(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public int code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public static PatientStatus fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(status -> Integer.valueOf(status.code).equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown patient status code: " + code));
    }
}
