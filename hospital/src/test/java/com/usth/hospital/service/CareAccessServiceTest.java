package com.usth.hospital.service;

import com.usth.hospital.model.*;
import com.usth.hospital.repository.*;
import com.usth.hospital.security.*;
import com.usth.hospital.service.impl.CareAccessService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareAccessServiceTest {
    @Mock CurrentUserProvider currentUser;
    @Mock AppUserRepository users;
    @Mock CareAssignmentRepository assignments;
    @Mock CareTaskRepository tasks;
    @Mock CareHandoffRepository handoffs;
    @Mock CareVisitRepository visits;
    @Mock PatientRepository patients;
    @Mock AdmissionRepository admissions;
    @InjectMocks CareAccessService access;
    private AppUserEntity user(UserRole role) { return AppUserEntity.builder().id(7L).username("fictional-staff").role(role).activeFlag(1).build(); }
    private CareAssignmentEntity assignment(boolean perform,boolean verify) { return CareAssignmentEntity.builder().id(1L).userId(7L).pointId(10L).active(1).canPerform(perform?1:0).canVerify(verify?1:0).build(); }

    @Test void patientAndTemporaryAccountsCannotEnterStaffWorkflow() {
        AppUserEntity patient=user(UserRole.PATIENT); when(currentUser.username()).thenReturn(patient.getUsername());
        when(users.findByUsernameIgnoreCase(patient.getUsername())).thenReturn(Optional.of(patient));
        assertThatThrownBy(access::actor).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        patient.setRole(UserRole.DOCTOR); patient.setPasswordChangeRequired(true);
        assertThatThrownBy(access::actor).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void technicianRequiresExplicitResultValidationAssignment() {
        AppUserEntity tech=user(UserRole.TECHNICIAN);
        when(assignments.findByUserIdAndPointId(7L,10L)).thenReturn(Optional.of(assignment(true,false)));
        assertThat(access.perform(tech,10L)).isTrue(); assertThat(access.verify(tech,10L)).isFalse();
        assertThat(access.perform(tech,11L)).isFalse();
    }
    @Test void nurseCannotValidateResultsEvenWithAnErroneousAssignmentFlag() {
        assertThat(access.verify(user(UserRole.NURSE),10L)).isFalse(); verifyNoInteractions(assignments);
    }
    @Test void administratorCannotSignClinicalWorkWithoutDoctorLinkAndAssignment() {
        AppUserEntity admin=user(UserRole.ADMIN);
        assertThat(access.examine(admin,10L)).isFalse();
        admin.setDoctorId(9L); assertThat(access.examine(admin,10L)).isFalse();
    }
    @Test void doctorMustBeLinkedActiveAndAssignedAtResponsiblePoint() {
        AppUserEntity doctor=user(UserRole.DOCTOR); doctor.setDoctorId(9L);
        when(assignments.findByUserIdAndPointId(7L,10L)).thenReturn(Optional.of(assignment(true,false)));
        var reference=mock(AdmissionRepository.DoctorRow.class); when(reference.getStatus()).thenReturn(1);
        when(admissions.findDoctor(9L)).thenReturn(Optional.of(reference));
        assertThat(access.examine(doctor,10L)).isTrue(); assertThat(access.examine(doctor,11L)).isFalse();
        when(reference.getStatus()).thenReturn(0); assertThat(access.examine(doctor,10L)).isFalse();
    }
    @Test void serviceAssignmentDoesNotExposeWholeClinicalRecord() {
        AppUserEntity tech=user(UserRole.TECHNICIAN);
        var visit=CareVisitEntity.builder().id("visit").pointId(1L).ownerId(2L).build();
        when(tasks.findByVisitIdOrderByCreatedAtAscIdAsc("visit")).thenReturn(List.of(CareTaskEntity.builder().pointId(10L).build()));
        when(assignments.findByUserIdAndPointId(7L,10L)).thenReturn(Optional.of(assignment(true,false)));
        assertThat(access.clinical(tech,visit)).isFalse(); assertThatCode(() -> access.requireRead(tech,visit)).doesNotThrowAnyException();
    }
    @Test void unrelatedNurseCannotReadAnotherTeamVisit() {
        var visit=CareVisitEntity.builder().id("visit").pointId(1L).ownerId(2L).build();
        assertThatThrownBy(() -> access.requireRead(user(UserRole.NURSE),visit)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void lockOrderAcquiresPatientBeforeReadingMutableVisit() {
        var visit=CareVisitEntity.builder().id("v").patientId("p").build();
        when(visits.patientId("v")).thenReturn(Optional.of("p")); when(patients.findByIdForUpdate("p")).thenReturn(Optional.of(new PatientEntity())); when(visits.findById("v")).thenReturn(Optional.of(visit));
        assertThat(access.lockVisit("v")).isSameAs(visit);
        InOrder order=inOrder(visits,patients); order.verify(visits).patientId("v"); order.verify(patients).findByIdForUpdate("p"); order.verify(visits).findById("v");
    }
}
