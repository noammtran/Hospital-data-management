package com.usth.hospital.dto.response;

import com.usth.hospital.security.UserRole;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String identityNo,
        String email,
        String displayName,
        UserRole role,
        boolean active,
        boolean mustChangePassword,
        String createdBy,
        LocalDateTime createdDate,
        String lastUpdatedBy,
        LocalDateTime lastUpdatedDate
) {
}
