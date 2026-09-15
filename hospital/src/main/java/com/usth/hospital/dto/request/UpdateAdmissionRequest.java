package com.usth.hospital.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateAdmissionRequest(
        @NotNull(message = "Department is required")
        @Positive(message = "Department is invalid")
        Long departmentId,

        @NotNull(message = "Doctor is required")
        @Positive(message = "Doctor is invalid")
        Long doctorId,

        @Positive(message = "Room is invalid")
        Long roomId,

        @NotNull(message = "Admission date is required")
        @PastOrPresent(message = "Admission date cannot be in the future")
        LocalDate admissionDate,

        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason,

        @Min(value = 1, message = "Admission type is invalid")
        @Max(value = 3, message = "Admission type is invalid")
        Integer type,

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        String notes
) {
}
