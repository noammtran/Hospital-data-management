package com.usth.hospital.dto.request;

import com.usth.hospital.security.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "Identity number is required")
        @Pattern(regexp = "^[0-9]{12}$", message = "Identity number must contain exactly 12 digits")
        String identityNo,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @NotBlank(message = "Display name is required")
        @Size(min = 2, max = 100, message = "Display name must contain 2 to 100 characters")
        String displayName,

        @NotNull(message = "Role is required")
        UserRole role
) {
}
