package com.usth.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.usth.hospital.exception.DuplicateFieldException;
import com.usth.hospital.exception.InvalidUserOperationException;
import com.usth.hospital.model.AppUserEntity;
import com.usth.hospital.model.PatientEntity;
import com.usth.hospital.repository.AppUserRepository;
import com.usth.hospital.security.CurrentUserProvider;
import com.usth.hospital.security.UserRole;
import com.usth.hospital.service.impl.PatientAccountLifecycleService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PatientAccountLifecycleServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CurrentUserProvider currentUserProvider;
    @InjectMocks private PatientAccountLifecycleService service;

    @Test
    void createFor_usesIdentityAsUsernameAndTemporaryPasswordWithoutPersistingPlaintext() {
        PatientEntity patient = PatientEntity.builder()
                .id("P26MB001").name("Nguyễn An").identityNo("079123456789").build();
        when(passwordEncoder.encode("079123456789")).thenReturn("bcrypt-hash");
        when(currentUserProvider.username()).thenReturn("receptionist");

        service.createFor(patient);

        ArgumentCaptor<AppUserEntity> captor = ArgumentCaptor.forClass(AppUserEntity.class);
        verify(appUserRepository).save(captor.capture());
        assertThat(captor.getValue()).satisfies(account -> {
            assertThat(account.getUsername()).isEqualTo("079123456789");
            assertThat(account.getPasswordHash()).isEqualTo("bcrypt-hash");
            assertThat(account.getPasswordHash()).doesNotContain("079123456789");
            assertThat(account.getRole()).isEqualTo(UserRole.PATIENT);
            assertThat(account.getPatientId()).isEqualTo("P26MB001");
            assertThat(account.isPasswordChangeRequired()).isTrue();
            assertThat(account.getEmail()).isNull();
        });
    }

    @Test
    void createFor_identityCollidingWithExistingLoginIsRejectedBeforeEncodingOrSaving() {
        PatientEntity patient = PatientEntity.builder()
                .id("P26MB001").name("Nguyễn An").identityNo("079123456789").build();
        when(appUserRepository.existsByUsernameIgnoreCase("079123456789")).thenReturn(true);

        assertThatThrownBy(() -> service.createFor(patient))
                .isInstanceOf(DuplicateFieldException.class)
                .hasFieldOrPropertyWithValue("field", "identityNo");

        verify(passwordEncoder, never()).encode("079123456789");
        verify(appUserRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void synchronize_changedIdentityRevokesSessionsAndRefreshesTemporaryPassword() {
        PatientEntity patient = PatientEntity.builder()
                .id("P26MB001").name("Nguyễn An Updated").identityNo("079987654321").build();
        AppUserEntity account = AppUserEntity.builder()
                .id(101L).username("079123456789").passwordHash("old-hash")
                .tokenVersion(2L).passwordChangeRequired(1).fullName("Nguyễn An")
                .patientId(patient.getId()).role(UserRole.PATIENT).activeFlag(1).build();
        when(appUserRepository.findByPatientId(patient.getId())).thenReturn(Optional.of(account));
        when(passwordEncoder.encode("079987654321")).thenReturn("new-hash");
        when(currentUserProvider.username()).thenReturn("admin");

        service.synchronize(patient);

        assertThat(account.getUsername()).isEqualTo("079987654321");
        assertThat(account.getPasswordHash()).isEqualTo("new-hash");
        assertThat(account.getTokenVersion()).isEqualTo(3L);
        assertThat(account.getFullName()).isEqualTo("Nguyễn An Updated");
    }

    @Test
    void synchronize_linkedAccountCannotLoseItsIdentityNumber() {
        PatientEntity patient = PatientEntity.builder()
                .id("P26MB001").name("Nguyễn An").identityNo(null).build();
        AppUserEntity account = AppUserEntity.builder()
                .id(101L).username("079123456789").passwordHash("hash")
                .tokenVersion(0L).passwordChangeRequired(0).fullName("Nguyễn An")
                .patientId(patient.getId()).role(UserRole.PATIENT).activeFlag(1).build();
        when(appUserRepository.findByPatientId(patient.getId())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.synchronize(patient))
                .isInstanceOf(InvalidUserOperationException.class)
                .hasFieldOrPropertyWithValue("field", "identityNo");

        verify(appUserRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteFor_bulkRemovesLinkedAccountsBeforePatientRows() {
        List<String> patientIds = List.of("P26MB001", "P26MB002");

        service.deleteFor(patientIds);

        verify(appUserRepository).deleteAllByPatientIdIn(patientIds);
    }
}
