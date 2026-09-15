package com.usth.hospital.dto.response;

import com.usth.hospital.security.UserRole;
import java.util.List;

public record CurrentUserResponse(
        Long id,
        String username,
        String email,
        String displayName,
        UserRole role,
        List<String> permissions,
        boolean mustChangePassword
) {
}
