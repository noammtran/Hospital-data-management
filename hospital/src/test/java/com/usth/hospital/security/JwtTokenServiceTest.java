package com.usth.hospital.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.usth.hospital.config.JwtProperties;
import com.usth.hospital.dto.response.AuthTokenResponse;
import com.usth.hospital.model.AppUserEntity;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenServiceTest {

    @Test
    void issue_containsSubjectRolePermissionsAndExpiry() {
        SecretKey key = new SecretKeySpec(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        JwtTokenService service = new JwtTokenService(
                new NimbusJwtEncoder(new ImmutableSecret<>(key)),
                new JwtProperties("unused-in-direct-construction", Duration.ofMinutes(30))
        );
        AppUserEntity doctor = AppUserEntity.builder()
                .username("doctor")
                .role(UserRole.DOCTOR)
                .build();

        AuthTokenResponse response = service.issue(doctor);

        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        Jwt jwt = decoder.decode(response.accessToken());
        assertThat(jwt.getSubject()).isEqualTo("doctor");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("hospital-api");
        assertThat(((Number) jwt.getClaim("token_version")).longValue()).isEqualTo(0L);
        assertThat(jwt.getClaimAsString("role")).isEqualTo("DOCTOR");
        assertThat(jwt.getClaimAsStringList("permissions"))
                .contains(
                        "VIEW_PATIENT",
                        "VIEW_MEDICAL_RECORD",
                        "VIEW_STATISTICS",
                        "PATIENT_START_EXAMINATION",
                        "PATIENT_COMPLETE_EXAMINATION",
                        "ADMISSION_CREATE",
                        "ADMISSION_UPDATE",
                        "ADMISSION_DISCHARGE")
                .doesNotContain("MANAGE_USERS", "VIEW_AUDIT_LOG");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(1800);
    }
}
