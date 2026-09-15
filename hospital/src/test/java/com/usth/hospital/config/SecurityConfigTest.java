package com.usth.hospital.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.usth.hospital.security.JwtTokenService;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwsHeader;

class SecurityConfigTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void decoder_acceptsOnlyTheHospitalIssuer() {
        SecurityConfig config = new SecurityConfig();
        SecretKey key = config.jwtSecretKey(new JwtProperties(SECRET, Duration.ofMinutes(30)));
        JwtEncoder encoder = config.jwtEncoder(key);
        JwtDecoder decoder = config.jwtDecoder(key);

        String valid = token(encoder, JwtTokenService.ISSUER);
        String wrongIssuer = token(encoder, "another-system");

        assertThat(decoder.decode(valid).getClaimAsString("iss")).isEqualTo(JwtTokenService.ISSUER);
        assertThatThrownBy(() -> decoder.decode(wrongIssuer))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void configuredSecretMustContainAtLeast32Utf8Bytes() {
        SecurityConfig config = new SecurityConfig();

        assertThatThrownBy(() -> config.jwtSecretKey(
                new JwtProperties("too-short", Duration.ofMinutes(30))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32");
    }

    private static String token(JwtEncoder encoder, String issuer) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("doctor")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(1800))
                .claim("token_version", 0L)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
