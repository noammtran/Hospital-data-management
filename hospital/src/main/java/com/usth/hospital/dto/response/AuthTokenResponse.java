package com.usth.hospital.dto.response;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
