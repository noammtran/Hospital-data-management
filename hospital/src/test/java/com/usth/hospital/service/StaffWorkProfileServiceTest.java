package com.usth.hospital.service;

import com.usth.hospital.dto.request.CreateStaffDoctorRequest;
import com.usth.hospital.dto.response.UserResponse;
import com.usth.hospital.exception.*;
import com.usth.hospital.model.*;
import com.usth.hospital.repository.*;
import com.usth.hospital.security.*;
import com.usth.hospital.service.impl.StaffWorkProfileService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffWorkProfileServiceTest {
    @Mock AppUserRepository users;
    @Mock DoctorRepository doctors;
    @Mock CareAssignmentRepository assignments;
    @Mock CarePointRepository points;
    @Mock AdmissionRepository references;
    @Mock UserManagementService userManagement;
    @Mock CurrentUserProvider currentUser;
    @Mock AuditLogService audit;
    @InjectMocks StaffWorkProfileService service;

    @Test
    void newDoctorHasRoleFunctionsButRequiresProfileAndPointSetup() {
        AppUserEntity user = user(UserRole.DOCTOR);
        user.setPasswordChangeRequired(true);
        read(user);
        var result = service.get(7L);
        assertThat(result.user().mustChangePassword()).isTrue();
        assertThat(result.setupIssues()).containsExactly("DOCTOR_LINK_MISSING", "POINT_ASSIGNMENT_MISSING");
        assertThat(result.rolePermissions()).contains("CARE_EXAMINE").doesNotContain("MANAGE_USERS");
        verifyNoInteractions(audit);
    }

    @Test
    void doctorWithActiveProfileAndExecutionAssignmentHasNoPendingSetup() {
        AppUserEntity user = user(UserRole.DOCTOR); user.setDoctorId(1000L); read(user);
        when(doctors.findById(1000L)).thenReturn(Optional.of(doctor(1)));
        work(1, 1, 1, 0);
        var result = service.get(7L);
        assertThat(result.setupIssues()).isEmpty();
        assertThat(result.doctor().name()).isEqualTo("Nguyễn Minh An");
        assertThat(result.assignments()).singleElement().satisfies(a -> {
            assertThat(a.pointName()).isEqualTo("Phòng khám 101");
            assertThat(a.perform()).isTrue(); assertThat(a.verify()).isFalse();
        });
        verify(assignments).findByUserIdOrderByIdAsc(7L);
    }

    @Test
    void unavailableDoctorAndInactiveAssignmentStayVisibleButDoNotSatisfySetup() {
        AppUserEntity user = user(UserRole.DOCTOR); user.setDoctorId(1000L); read(user);
        when(doctors.findById(1000L)).thenReturn(Optional.of(doctor(0)));
        work(0, 1, 1, 0);
        var result = service.get(7L);
        assertThat(result.setupIssues()).contains("DOCTOR_UNAVAILABLE", "POINT_ASSIGNMENT_MISSING");
        assertThat(result.doctor().active()).isFalse();
        assertThat(result.assignments()).hasSize(1);
    }

    @Test
    void unavailablePointDoesNotCountAsNewWorkSetup() {
        read(user(UserRole.NURSE)); work(1, 0, 0, 0);
        assertThat(service.get(7L).setupIssues()).containsExactly("POINT_ASSIGNMENT_MISSING");
    }

    @Test
    void nurseCoordinationDoesNotRequireDoctorLinkOrExecutionFlag() {
        read(user(UserRole.NURSE)); work(1, 1, 0, 0);
        assertThat(service.get(7L).setupIssues()).isEmpty();
        verifyNoInteractions(doctors);
    }

    @Test
    void technicianWithoutExecutionOrValidationIsIdentified() {
        read(user(UserRole.TECHNICIAN)); work(1, 1, 0, 0);
        assertThat(service.get(7L).setupIssues()).containsExactly("CLINICAL_CAPABILITY_MISSING");
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ADMIN", "RECEPTIONIST"})
    void nonClinicalStaffDoNotNeedDoctorOrPointSetup(UserRole role) {
        read(user(role));
        assertThat(service.get(7L).setupIssues()).isEmpty();
    }

    @Test
    void patientAndMissingAccountsCannotBeReadOrConfiguredAsStaff() {
        when(users.findById(7L)).thenReturn(Optional.of(user(UserRole.PATIENT)));
        assertThatThrownBy(() -> service.get(7L)).isInstanceOf(UserNotFoundException.class);
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user(UserRole.PATIENT)));
        assertThatThrownBy(() -> service.createDoctor(7L, request())).isInstanceOf(UserNotFoundException.class);
        assertThatThrownBy(() -> service.get(8L)).isInstanceOf(UserNotFoundException.class);
        verifyNoInteractions(doctors, audit);
    }

    @Test
    void createDoctorLinksProfileAndAuditsAccountInTheSameServiceTransaction() {
        AppUserEntity user = user(UserRole.DOCTOR);
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        department();
        when(doctors.saveAndFlush(any())).thenAnswer(invocation -> {
            DoctorEntity doctor = invocation.getArgument(0); doctor.setId(1000L); return doctor;
        });
        when(currentUser.username()).thenReturn("admin-test");
        var result = service.createDoctor(7L, request());
        assertThat(user.getDoctorId()).isEqualTo(1000L);
        assertThat(result.code()).isEqualTo("DOC_TEST");
        assertThat(result.name()).isEqualTo("Nguyễn Minh An");
        assertThat(result.active()).isTrue();
        ArgumentCaptor<DoctorEntity> saved = ArgumentCaptor.forClass(DoctorEntity.class);
        verify(doctors).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isNull();
        assertThat(saved.getValue().getSpecialization()).isEqualTo("Nội tổng quát");
        ArgumentCaptor<AuditEventCommand> event = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(audit).record(event.capture());
        assertThat(event.getValue().entityType()).isEqualTo(AuditLogEntityType.USER);
        assertThat(event.getValue().changes()).singleElement().satisfies(change -> {
            assertThat(change.fieldKey()).isEqualTo("doctorId");
            assertThat(change.newValue()).isEqualTo("1000");
        });
        InOrder order = inOrder(users, doctors, audit);
        order.verify(users).findByIdForUpdate(7L);
        order.verify(doctors).saveAndFlush(any());
        order.verify(users).saveAndFlush(user);
        order.verify(audit).record(any());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"NURSE", "RECEPTIONIST", "TECHNICIAN"})
    void nonDoctorRoleCannotCreateDoctorProfile(UserRole role) {
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user(role)));
        assertThatThrownBy(() -> service.createDoctor(7L, request())).isInstanceOf(InvalidUserOperationException.class);
        verifyNoInteractions(doctors, audit);
    }

    @Test
    void existingDoctorLinkCannotBeReplacedByCreatingAnotherProfile() {
        var user = user(UserRole.DOCTOR); user.setDoctorId(42L);
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.createDoctor(7L, request())).isInstanceOf(CareWorkflowException.class);
        verifyNoInteractions(doctors, audit);
    }

    @Test
    void duplicateCodeAndMissingDepartmentAreRejectedBeforeWriting() {
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user(UserRole.DOCTOR)));
        assertThatThrownBy(() -> service.createDoctor(7L, request())).isInstanceOf(InvalidUserOperationException.class);
        department(); when(doctors.existsByCodeIgnoreCase("DOC_TEST")).thenReturn(true);
        assertThatThrownBy(() -> service.createDoctor(7L, request())).isInstanceOf(DuplicateFieldException.class);
        verify(doctors, never()).saveAndFlush(any()); verifyNoInteractions(audit);
    }

    @Test
    void failedProfileInsertDoesNotLinkAccountOrRecordSuccess() {
        var user = user(UserRole.DOCTOR);
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user)); department();
        when(doctors.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate code"));
        assertThatThrownBy(() -> service.createDoctor(7L, request())).isInstanceOf(CareWorkflowException.class);
        assertThat(user.getDoctorId()).isNull();
        verify(users, never()).saveAndFlush(any()); verifyNoInteractions(audit);
    }

    private void read(AppUserEntity user) {
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(userManagement.get(7L)).thenReturn(new UserResponse(7L, "001201100099", "001201100099",
                "minh.an@example.test", user.getFullName(), user.getRole(), user.isActive(), user.isPasswordChangeRequired(),
                "test-admin", null, "test-admin", null));
    }
    private void work(int active, int pointActive, int perform, int verify) {
        when(assignments.findByUserIdOrderByIdAsc(7L)).thenReturn(List.of(CareAssignmentEntity.builder()
                .id(1L).userId(7L).pointId(101L).active(active).canPerform(perform).canVerify(verify).build()));
        when(points.findAllById(List.of(101L))).thenReturn(List.of(CarePointEntity.builder()
                .id(101L).name("Phòng khám 101").kind("CLINIC").departmentId(1L).active(pointActive).build()));
    }
    private void department() {
        var department = mock(AdmissionRepository.DepartmentRow.class);
        lenient().when(department.getId()).thenReturn(1L); lenient().when(department.getName()).thenReturn("Nội");
        when(references.findDepartment(1L)).thenReturn(Optional.of(department));
    }
    private static AppUserEntity user(UserRole role) {
        return AppUserEntity.builder().id(7L).fullName("Nguyễn Minh An").role(role)
                .activeFlag(1).passwordChangeRequired(0).build();
    }
    private static DoctorEntity doctor(int status) {
        return DoctorEntity.builder().id(1000L).code("DOC_TEST").name("Nguyễn Minh An").departmentId(1L).status(status).build();
    }
    private static CreateStaffDoctorRequest request() {
        return new CreateStaffDoctorRequest("DOC_TEST", " Nguyễn Minh An ", 1L, " Nội tổng quát ", "", "");
    }
}
