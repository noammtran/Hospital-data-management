package com.usth.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.usth.hospital.dto.request.ChangeOwnPasswordRequest;
import com.usth.hospital.dto.request.LoginRequest;
import com.usth.hospital.dto.response.AuthTokenResponse;
import com.usth.hospital.dto.response.CurrentUserResponse;
import com.usth.hospital.exception.InvalidCredentialsException;
import com.usth.hospital.model.AppUserEntity;
import com.usth.hospital.repository.AppUserRepository;
import com.usth.hospital.security.CurrentUserProvider;
import com.usth.hospital.security.JwtTokenService;
import com.usth.hospital.security.UserRole;
import com.usth.hospital.service.impl.AuthenticationServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationServiceImpl service;

    @Test
    void login_validCredentials_returnsAccessToken() {
        AppUserEntity user = user(UserRole.DOCTOR, true);
        AuthTokenResponse token = new AuthTokenResponse("jwt", "Bearer", 1800);
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated("doctor", null, userAuthorities())
        );
        when(appUserRepository.findByUsernameIgnoreCase("doctor")).thenReturn(Optional.of(user));
        when(jwtTokenService.issue(user)).thenReturn(token);

        AuthTokenResponse result = service.login(new LoginRequest(" doctor ", "Doctor@123"));

        assertThat(result).isEqualTo(token);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_invalidCredentials_returnsGenericFailure() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("internal provider detail"));

        assertThatThrownBy(() -> service.login(new LoginRequest("doctor", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void currentUser_returnsDoctorWorkflowPermissions() {
        AppUserEntity user = user(UserRole.DOCTOR, true);
        when(currentUserProvider.username()).thenReturn("doctor");
        when(appUserRepository.findByUsernameIgnoreCase("doctor")).thenReturn(Optional.of(user));

        CurrentUserResponse result = service.currentUser();

        assertThat(result.role()).isEqualTo(UserRole.DOCTOR);
        assertThat(result.permissions()).contains(
                "VIEW_PATIENT",
                "VIEW_MEDICAL_RECORD",
                "VIEW_STATISTICS",
                "PATIENT_START_EXAMINATION",
                "PATIENT_COMPLETE_EXAMINATION",
                "ADMISSION_CREATE",
                "ADMISSION_UPDATE",
                "ADMISSION_DISCHARGE"
        );
        assertThat(result.permissions()).doesNotContain(
                "MANAGE_USERS", "DELETE_PATIENT", "VIEW_AUDIT_LOG");
        assertThat(result.mustChangePassword()).isFalse();
    }

    @Test
    void currentUser_patientWithTemporaryPassword_onlyReceivesPasswordChangePermission() {
        AppUserEntity user = user(UserRole.PATIENT, true);
        user.setPasswordChangeRequired(true);
        when(currentUserProvider.username()).thenReturn("doctor");
        when(appUserRepository.findByUsernameIgnoreCase("doctor")).thenReturn(Optional.of(user));

        CurrentUserResponse result = service.currentUser();

        assertThat(result.permissions()).containsExactly("CHANGE_OWN_PASSWORD");
        assertThat(result.mustChangePassword()).isTrue();
    }

    @Test
    void changePassword_replacesTemporaryPasswordAndRevokesCurrentToken() {
        AppUserEntity user = user(UserRole.PATIENT, true);
        user.setPasswordHash("old-hash");
        user.setPasswordChangeRequired(true);
        when(currentUserProvider.username()).thenReturn("doctor");
        when(appUserRepository.findByUsernameIgnoreCase("doctor")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("079123456789", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("Patient@123", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("Patient@123")).thenReturn("new-hash");

        service.changePassword(new ChangeOwnPasswordRequest("079123456789", "Patient@123"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.isPasswordChangeRequired()).isFalse();
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        verify(appUserRepository).save(user);
    }

    private static AppUserEntity user(UserRole role, boolean active) {
        AppUserEntity user = AppUserEntity.builder()
                .id(2L)
                .username("doctor")
                .email("doctor@hospital.example")
                .fullName("Trần Quốc Bảo")
                .role(role)
                .activeFlag(active ? 1 : 0)
                .build();
        return user;
    }

    private static java.util.List<org.springframework.security.core.GrantedAuthority> userAuthorities() {
        return java.util.List.of();
    }
}
