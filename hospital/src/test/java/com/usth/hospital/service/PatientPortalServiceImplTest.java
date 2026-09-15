package com.usth.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.usth.hospital.model.AppUserEntity;
import com.usth.hospital.model.PatientEntity;
import com.usth.hospital.repository.AppUserRepository;
import com.usth.hospital.dto.response.VisitResponse;
import com.usth.hospital.service.impl.MedicalRecordServiceImpl;
import com.usth.hospital.repository.PatientRepository;
import com.usth.hospital.security.CurrentUserProvider;
import com.usth.hospital.security.UserRole;
import com.usth.hospital.service.impl.PatientPortalServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientPortalServiceImplTest {

    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private AppUserRepository appUserRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private MedicalRecordServiceImpl medicalRecordService;
    @InjectMocks private PatientPortalServiceImpl service;

    @Test
    void getOwnRecord_resolvesPatientIdOnlyFromAuthenticatedAccount() {
        AppUserEntity account = AppUserEntity.builder()
                .username("079123456789").role(UserRole.PATIENT).activeFlag(1)
                .patientId("P26MB001").build();
        PatientEntity patient = PatientEntity.builder()
                .id("P26MB001").name("Nguyễn An").identityNo("079123456789").status(2).build();
        VisitResponse visit = new VisitResponse("A-1", "2026-08-01", null, "Nội khoa", null,
                null, null, null, null, null, List.of(), List.of(), null);
        when(currentUserProvider.username()).thenReturn("079123456789");
        when(appUserRepository.findByUsernameIgnoreCase("079123456789")).thenReturn(Optional.of(account));
        when(patientRepository.findById("P26MB001")).thenReturn(Optional.of(patient));
        when(medicalRecordService.getPatientRecord("P26MB001")).thenReturn(List.of(visit));

        var result = service.getOwnRecord();

        assertThat(result.patient().id()).isEqualTo("P26MB001");
        assertThat(result.patient().identityNo()).isEqualTo("079123456789");
        assertThat(result.records()).singleElement().satisfies(admission -> {
            assertThat(admission.admissionId()).isEqualTo("A-1");
            assertThat(admission.departmentName()).isEqualTo("Nội khoa");
        });
        verify(patientRepository).findById("P26MB001");
        verify(medicalRecordService).getPatientRecord("P26MB001");
    }
}
