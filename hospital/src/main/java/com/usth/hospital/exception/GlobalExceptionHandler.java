package com.usth.hospital.exception;

import com.usth.hospital.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CareWorkflowException.class)
    public ResponseEntity<ApiResponse<Void>> handleCareWorkflow(CareWorkflowException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getStatus(), ex.getMessage()));
    }

    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(PatientNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage()));
    }

    @ExceptionHandler(AdmissionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAdmissionNotFound(AdmissionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage()));
    }

    @ExceptionHandler(AuditLogNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuditLogNotFound(AuditLogNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage()));
    }

    @ExceptionHandler(AdmissionConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleAdmissionConflict(AdmissionConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409, ex.getMessage()));
    }

    @ExceptionHandler(PatientStatusConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handlePatientStatusConflict(
            PatientStatusConflictException ex
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409, ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, ex.getMessage()));
    }

    @ExceptionHandler(DuplicateFieldException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateFieldException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.validationError("Invalid data", Map.of(ex.getField(), ex.getMessage())));
    }

    @ExceptionHandler(InvalidUserOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidUserOperation(InvalidUserOperationException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.validationError("Invalid data", Map.of(ex.getField(), ex.getMessage())));
    }

    @ExceptionHandler(InvalidAdmissionOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidAdmissionOperation(
            InvalidAdmissionOperationException ex
    ) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.validationError("Invalid data", Map.of(ex.getField(), ex.getMessage())));
    }

    @ExceptionHandler(InvalidAuditLogFilterException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidAuditLogFilter(InvalidAuditLogFilterException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.validationError("Invalid data", Map.of(ex.getField(), ex.getMessage())));
    }

    @ExceptionHandler(InvalidStatisticsFilterException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStatisticsFilter(InvalidStatisticsFilterException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.validationError("Invalid data", Map.of(ex.getField(), ex.getMessage())));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, "You do not have permission for this action"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "Request body is invalid or missing"));
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequestParameter(Exception ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "Request parameter is invalid"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                errors.put(err.getField(), err.getDefaultMessage())
        );
        return ResponseEntity.badRequest()
                .body(ApiResponse.validationError("Invalid data", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Internal server error"));
    }
}
