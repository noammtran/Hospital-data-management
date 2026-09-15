package com.usth.hospital.dto.request;

import jakarta.validation.constraints.*;

public record CreateStaffDoctorRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9_-]{1,20}$",
                message = "Use up to 20 uppercase letters, digits, underscores or hyphens") String code,
        @NotBlank @Size(max = 100) String name,
        @NotNull @Positive Long departmentId,
        @Size(max = 100) String specialization,
        @Size(max = 20) String phoneNo,
        @Email @Size(max = 50) String email
) {}
