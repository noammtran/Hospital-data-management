package com.usth.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.usth.hospital.dto.request.CreateUserRequest;
import com.usth.hospital.dto.request.UpdateUserRequest;
import com.usth.hospital.dto.response.UserResponse;
import com.usth.hospital.exception.DuplicateFieldException;
import com.usth.hospital.exception.InvalidUserOperationException;
import com.usth.hospital.model.AppUserEntity;
import com.usth.hospital.model.AuditLogAction;
import com.usth.hospital.model.AuditLogEntityType;
import com.usth.hospital.repository.AppUserRepository;
import com.usth.hospital.security.CurrentUserProvider;
import com.usth.hospital.security.UserRole;
import com.usth.hospital.service.impl.UserManagementServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.username()).thenReturn("admin");
    }

    @Test
    void create_normalizesFieldsHashesPasswordAndStampsActor() {
        when(passwordEncoder.encode("001201100022")).thenReturn("bcrypt-hash");
        when(appUserRepository.save(any(AppUserEntity.class))).thenAnswer(invocation -> {
            AppUserEntity user = invocation.getArgument(0);
            user.setId(101L);
            return user;
        });

        UserResponse result = service.create(new CreateUserRequest(
                "001201100022", " Doctor2@Hospital.Example ", "  Bác sĩ Demo  ",
                UserRole.DOCTOR
        ));

        assertThat(result.username()).isEqualTo("001201100022");
        assertThat(result.identityNo()).isEqualTo("001201100022");
        assertThat(result.email()).isEqualTo("doctor2@hospital.example");
        assertThat(result.displayName()).isEqualTo("Bác sĩ Demo");
        assertThat(result.createdBy()).isEqualTo("admin");
        assertThat(result.active()).isTrue();
        assertThat(result.mustChangePassword()).isTrue();

        ArgumentCaptor<AppUserEntity> captor = ArgumentCaptor.forClass(AppUserEntity.class);
        verify(appUserRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        ArgumentCaptor<AuditEventCommand> auditCaptor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue()).satisfies(event -> {
            assertThat(event.action()).isEqualTo(AuditLogAction.CREATE);
            assertThat(event.entityType()).isEqualTo(AuditLogEntityType.USER);
            assertThat(event.entityId()).isEqualTo("101");
            assertThat(event.targetLabel()).isEqualTo("Bác sĩ Demo · Doctor");
            assertThat(event.summary()).isEqualTo("Record created");
            assertThat(event.changes()).extracting(AuditLogChange::fieldKey)
                    .containsExactly("identityNo", "email", "displayName", "role", "active");
            assertThat(event.changes()).noneMatch(change -> change.fieldKey().toLowerCase().contains("password"));
        });
    }

    @Test
    void create_duplicateEmail_returnsFieldError() {
        when(appUserRepository.existsByEmailIgnoreCase("doctor2@hospital.example")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateUserRequest(
                "001201100022", "doctor2@hospital.example", "Bác sĩ Demo",
                UserRole.DOCTOR
        )))
                .isInstanceOf(DuplicateFieldException.class)
                .hasFieldOrPropertyWithValue("field", "email");

        verify(appUserRepository, never()).save(any());
        verify(auditLogService, never()).record(any());
    }

    @Test
    void create_duplicateIdentityNumber_returnsFieldError() {
        when(appUserRepository.existsByUsernameIgnoreCase("001201100022")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateUserRequest(
                "001201100022", "doctor2@hospital.example", "Bác sĩ Demo", UserRole.DOCTOR
        )))
                .isInstanceOf(DuplicateFieldException.class)
                .hasFieldOrPropertyWithValue("field", "identityNo");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void create_patientRole_isRejectedBecausePatientAccountsAreLifecycleManaged() {
        assertThatThrownBy(() -> service.create(new CreateUserRequest(
                "001201400001", "patient@hospital.example", "Patient Demo",
                UserRole.PATIENT
        )))
                .isInstanceOf(InvalidUserOperationException.class)
                .hasFieldOrPropertyWithValue("field", "role");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void get_patientAccount_isHiddenFromStaffManagement() {
        AppUserEntity patient = user(10L, "079123456789", UserRole.PATIENT, true);
        patient.setPatientId("P26MB001");
        when(appUserRepository.findById(10L)).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> service.get(10L))
                .isInstanceOf(com.usth.hospital.exception.UserNotFoundException.class);
    }

    @Test
    void setActive_cannotLockOwnAccount() {
        AppUserEntity admin = user(4L, "admin", UserRole.ADMIN, true);
        when(appUserRepository.findById(4L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.setActive(4L, false))
                .isInstanceOf(InvalidUserOperationException.class)
                .hasFieldOrPropertyWithValue("field", "active");
        verify(auditLogService, never()).record(any());
    }

    @Test
    void update_success_recordsOneUserUpdateEvent() {
        AppUserEntity doctor = user(2L, "001201100001", UserRole.DOCTOR, true);
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(appUserRepository.save(doctor)).thenReturn(doctor);

        UserResponse updated = service.update(2L, new UpdateUserRequest(
                "001201100001", "updated@hospital.example", "Updated Doctor", UserRole.DOCTOR
        ));

        assertThat(updated.email()).isEqualTo("updated@hospital.example");
        ArgumentCaptor<AuditEventCommand> auditCaptor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditLogAction.UPDATE);
        assertThat(auditCaptor.getValue().summary()).isEqualTo("2 fields changed");
        assertThat(auditCaptor.getValue().changes()).extracting(AuditLogChange::fieldKey)
                .containsExactly("email", "displayName");
    }

    @Test
    void update_identityNumber_refreshesTemporaryCredentialsAndRevokesSessions() {
        AppUserEntity doctor = user(2L, "001201100001", UserRole.DOCTOR, true);
        doctor.setPasswordChangeRequired(true);
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(appUserRepository.save(doctor)).thenReturn(doctor);
        when(passwordEncoder.encode("001201100099")).thenReturn("refreshed-hash");

        service.update(2L, new UpdateUserRequest(
                "001201100099", doctor.getEmail(), doctor.getFullName(), UserRole.DOCTOR
        ));

        assertThat(doctor.getUsername()).isEqualTo("001201100099");
        assertThat(doctor.getIdentityNo()).isEqualTo("001201100099");
        assertThat(doctor.getPasswordHash()).isEqualTo("refreshed-hash");
        assertThat(doctor.getTokenVersion()).isEqualTo(1L);
    }

    @Test
    void setActive_success_recordsStatusWithoutSensitiveData() {
        AppUserEntity doctor = user(2L, "001201100001", UserRole.DOCTOR, true);
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(appUserRepository.save(doctor)).thenReturn(doctor);

        UserResponse updated = service.setActive(2L, false);

        assertThat(updated.active()).isFalse();
        ArgumentCaptor<AuditEventCommand> auditCaptor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditLogAction.STATUS_UPDATE);
        assertThat(auditCaptor.getValue().summary()).isEqualTo("Active to Locked");
        assertThat(auditCaptor.getValue().changes()).singleElement().satisfies(change -> {
            assertThat(change.fieldKey()).isEqualTo("active");
            assertThat(change.oldDisplayValue()).isEqualTo("Active");
            assertThat(change.newDisplayValue()).isEqualTo("Locked");
        });
    }

    @Test
    void update_cannotDemoteLastActiveAdmin() {
        AppUserEntity admin = user(4L, "admin", UserRole.ADMIN, true);
        when(appUserRepository.findById(4L)).thenReturn(Optional.of(admin));
        when(appUserRepository.lockActiveUsersByRole(UserRole.ADMIN)).thenReturn(List.of(admin));

        assertThatThrownBy(() -> service.update(4L, new UpdateUserRequest(
                "001201900001", "admin@hospital.example", "Admin Demo", UserRole.DOCTOR
        )))
                .isInstanceOf(InvalidUserOperationException.class)
                .hasFieldOrPropertyWithValue("field", "role");
    }

    @Test
    void resetPassword_restoresIdentityNumberAndRequiresAnotherChange() {
        AppUserEntity doctor = user(2L, "001201100001", UserRole.DOCTOR, true);
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(passwordEncoder.encode("001201100001")).thenReturn("new-hash");

        service.resetPassword(2L);

        assertThat(doctor.getPasswordHash()).isEqualTo("new-hash");
        assertThat(doctor.getTokenVersion()).isEqualTo(1L);
        assertThat(doctor.isPasswordChangeRequired()).isTrue();
        assertThat(doctor.getLastUpdatedBy()).isEqualTo("admin");
        verify(appUserRepository).save(doctor);
        ArgumentCaptor<AuditEventCommand> auditCaptor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue()).satisfies(event -> {
            assertThat(event.action()).isEqualTo(AuditLogAction.PASSWORD_RESET);
            assertThat(event.targetLabel()).isEqualTo("Demo User · Doctor");
            assertThat(event.summary()).isEqualTo("Credentials reset and existing sessions revoked");
            assertThat(event.changes()).isEmpty();
        });
    }

    @Test
    void resetPassword_requiresIdentityNumberForLegacyStaffAccount() {
        AppUserEntity doctor = user(2L, "legacy-doctor", UserRole.DOCTOR, true);
        doctor.setIdentityNo(null);
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(doctor));

        assertThatThrownBy(() -> service.resetPassword(2L))
                .isInstanceOf(InvalidUserOperationException.class)
                .hasFieldOrPropertyWithValue("field", "identityNo");

        verify(appUserRepository, never()).save(any());
        verify(auditLogService, never()).record(any());
    }

    private static AppUserEntity user(Long id, String username, UserRole role, boolean active) {
        return AppUserEntity.builder()
                .id(id)
                .username(username)
                .email(username + "@hospital.example")
                .passwordHash("existing-hash")
                .identityNo(role == UserRole.PATIENT ? null : username)
                .passwordChangeRequired(0)
                .fullName("Demo User")
                .role(role)
                .activeFlag(active ? 1 : 0)
                .createdBy("SYSTEM")
                .createdDate(LocalDateTime.now())
                .lastUpdatedBy("SYSTEM")
                .lastUpdatedDate(LocalDateTime.now())
                .build();
    }
}
