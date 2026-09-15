package com.usth.hospital.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangeOwnPasswordRequest(
        @NotBlank(message = "Current password is required")
        @Size(max = 72, message = "Current password must contain at most 72 characters")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Pattern(regexp = "^(?=[\\x20-\\x7E]{8,72}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must contain 8 to 72 printable ASCII characters with uppercase, lowercase and a number")
        String newPassword
) {
}
