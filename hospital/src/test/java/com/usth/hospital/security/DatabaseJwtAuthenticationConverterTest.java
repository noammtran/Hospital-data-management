package com.usth.hospital.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.usth.hospital.model.AppUserEntity;
import com.usth.hospital.repository.AppUserRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class DatabaseJwtAuthenticationConverterTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Test
    void convert_reloadsCurrentRoleAndPermissionsFromDatabase() {
        AppUserEntity user = AppUserEntity.builder()
                .username("doctor")
                .fullName("Doctor Test")
                .role(UserRole.ADMIN)
                .activeFlag(1)
                .tokenVersion(0L)
                .build();
        when(appUserRepository.findByUsernameIgnoreCase("doctor")).thenReturn(Optional.of(user));

        var authentication = new DatabaseJwtAuthenticationConverter(appUserRepository).convert(jwt("doctor"));

        assertThat(authentication.getName()).isEqualTo("doctor");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_ADMIN", "MANAGE_USERS", "DELETE_PATIENT");
        assertThat(authentication.getDetails()).isEqualTo(
                new CurrentUserSnapshot("doctor", "Doctor Test", "ADMIN"));
    }

    @Test
    void convert_rejectsAccountLockedAfterTokenWasIssued() {
        AppUserEntity locked = AppUserEntity.builder()
                .username("doctor")
                .role(UserRole.DOCTOR)
                .activeFlag(0)
                .tokenVersion(0L)
                .build();
        when(appUserRepository.findByUsernameIgnoreCase("doctor")).thenReturn(Optional.of(locked));

        var converter = new DatabaseJwtAuthenticationConverter(appUserRepository);

        assertThatThrownBy(() -> converter.convert(jwt("doctor")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Account is unavailable");
    }

    @Test
    void convert_rejectsTokenIssuedBeforePasswordReset() {
        AppUserEntity user = AppUserEntity.builder()
                .username("doctor")
                .role(UserRole.DOCTOR)
                .activeFlag(1)
                .tokenVersion(2L)
                .build();
        when(appUserRepository.findByUsernameIgnoreCase("doctor")).thenReturn(Optional.of(user));

        var converter = new DatabaseJwtAuthenticationConverter(appUserRepository);

        assertThatThrownBy(() -> converter.convert(jwt("doctor", 1L)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Token is no longer valid");
    }

    @Test
    void convert_temporaryPatientPasswordCannotReadAnyPatientRecord() {
        AppUserEntity user = AppUserEntity.builder()
                .username("079123456789")
                .fullName("Patient Test")
                .role(UserRole.PATIENT)
                .activeFlag(1)
                .passwordChangeRequired(1)
                .tokenVersion(0L)
                .build();
        when(appUserRepository.findByUsernameIgnoreCase("079123456789"))
                .thenReturn(Optional.of(user));

        var authentication = new DatabaseJwtAuthenticationConverter(appUserRepository)
                .convert(jwt("079123456789"));

        assertThat(authentication.getAuthorities()).extracting("authority")
                .contains("ROLE_PATIENT", "CHANGE_OWN_PASSWORD")
                .doesNotContain("VIEW_OWN_RECORD", "VIEW_PATIENT", "VIEW_MEDICAL_RECORD");
    }

    private static Jwt jwt(String subject) {
        return jwt(subject, 0L);
    }

    private static Jwt jwt(String subject, long tokenVersion) {
        Instant now = Instant.now();
        return new Jwt(
                "token",
                now,
                now.plusSeconds(1800),
                Map.of("alg", "HS256"),
                Map.of("sub", subject, "token_version", tokenVersion)
        );
    }
}
