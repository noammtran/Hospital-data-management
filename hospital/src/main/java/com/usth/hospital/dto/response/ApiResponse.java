package com.usth.hospital.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int statusCode,
        String message,
        T data,
        Map<String, String> errors,
        Instant timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, 200, "Success", data, null, Instant.now());
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, 201, "Created", data, null, Instant.now());
    }

    public static ApiResponse<Void> noContent() {
        return new ApiResponse<>(true, 204, "No Content", null, null, Instant.now());
    }

    public static ApiResponse<Void> error(int statusCode, String message) {
        return new ApiResponse<>(false, statusCode, message, null, null, Instant.now());
    }

    public static ApiResponse<Void> validationError(String message, Map<String, String> errors) {
        return new ApiResponse<>(false, 400, message, null, errors, Instant.now());
    }
}
