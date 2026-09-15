package com.usth.hospital.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hospital.security.jwt")
public record JwtProperties(
        String secret,
        Duration accessTokenTtl
) {
    public JwtProperties {
        if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException("JWT access-token TTL must be positive");
        }
    }
}
