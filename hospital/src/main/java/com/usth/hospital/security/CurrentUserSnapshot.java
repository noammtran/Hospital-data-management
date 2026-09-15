package com.usth.hospital.security;

public record CurrentUserSnapshot(
        String username,
        String displayName,
        String role
) {
}
