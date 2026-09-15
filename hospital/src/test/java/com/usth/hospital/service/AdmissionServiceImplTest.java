package com.usth.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.usth.hospital.dto.request.CreateAdmissionRequest;
import com.usth.hospital.dto.request.UpdateAdmissionRequest;
import com.usth.hospital.dto.response.AdmissionEditDataResponse;
import com.usth.hospital.dto.response.AdmissionOptionsResponse;
import com.usth.hospital.dto.response.AdmissionResponse;
import com.usth.hospital.exception.AdmissionConflictException;
import com.usth.hospital.exception.AdmissionNotFoundException;
import com.usth.hospital.exception.InvalidAdmissionOperationException;
import com.usth.hospital.exception.PatientNotFoundException;
import com.usth.hospital.model.AdmissionEntity;
import com.usth.hospital.model.AuditLogAction;
import com.usth.hospital.model.AuditLogEntityType;
import com.usth.hospital.model.PatientEntity;
import com.usth.hospital.repository.AdmissionRepository;
import com.usth.hospital.repository.CareVisitRepository;
import com.usth.hospital.service.impl.CareAccessService;
import com.usth.hospital.repository.AdmissionRepository.DepartmentRow;
import com.usth.hospital.repository.AdmissionRepository.DoctorRow;
import com.usth.hospital.repository.AdmissionRepository.LookupRow;
import com.usth.hospital.repository.AdmissionRepository.RoomRow;
import com.usth.hospital.repository.PatientRepository;
import com.usth.hospital.service.impl.AdmissionServiceImpl;
import com.usth.hospital.service.impl.PatientStatusTransitionService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceImplTest {

    private static final String PATIENT_ID = "P26MB001";

    @Mock
    private AdmissionRepository admissionRepository;

    @Mock
    private CareVisitRepository careVisitRepository;

    @Mock
    private CareAccessService careAccessService;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PatientStatusTransitionService patientStatusTransitionService;

    @InjectMocks
    private AdmissionServiceImpl service;

    @Test
    void getOptions_returnsOnlyRepositoryApprovedReferenceRows() {
        DepartmentRow department = department(4L);
        DoctorRow doctor = doctor(9L, 4L, 1);
        RoomRow room = room(12L, 4L, 10, 6, 1);
        LookupRow type = mock(LookupRow.class);
        when(type.getValue()).thenReturn(2);
        when(type.getLabel()).thenReturn("Urgent");
        when(admissionRepository.findDepartmentOptions()).thenReturn(List.of(department));
        when(admissionRepository.findActiveDoctorOptions()).thenReturn(List.of(doctor));
        when(admissionRepository.findAvailableRoomOptions()).thenReturn(List.of(room));
        when(admissionRepository.findActiveAdmissionTypes()).thenReturn(List.of(type));

        AdmissionOptionsResponse options = service.getOptions();

        assertThat(options.departments()).singleElement().satisfies(value ->
                assertThat(value.id()).isEqualTo(4L));
        assertThat(options.doctors()).singleElement().satisfies(value ->
                assertThat(value.id()).isEqualTo(9L));
        assertThat(options.rooms()).singleElement().satisfies(value ->
                assertThat(value.currentOccupancy()).isEqualTo(6));
        assertThat(options.types()).containsExactly(new AdmissionOptionsResponse.LookupOption(2, "Urgent"));
    }

    @Test
    void getEditData_appendsCurrentInactiveAndFullReferences() {
        AdmissionEntity admission = activeAdmission(4L, 9L, 12L, 2);
        LookupRow currentType = lookup(2, "Legacy urgent");
        when(admissionRepository.findByIdAndPatientId("admission-1", PATIENT_ID))
                .thenReturn(Optional.of(admission));
        when(admissionRepository.findDepartmentOptions()).thenReturn(List.of(department(4L)));
        when(admissionRepository.findActiveDoctorOptions()).thenReturn(List.of());
        when(admissionRepository.findAvailableRoomOptions()).thenReturn(List.of());
        when(admissionRepository.findActiveAdmissionTypes()).thenReturn(List.of());
        when(admissionRepository.findDoctor(9L)).thenReturn(Optional.of(doctor(9L, 4L, 0)));
        when(admissionRepository.findRoom(12L)).thenReturn(
                Optional.of(room(12L, 4L, 10, 10, 0)));
        when(admissionRepository.findAdmissionType(2)).thenReturn(Optional.of(currentType));

        AdmissionEditDataResponse data = service.getEditData(PATIENT_ID, "admission-1");

        assertThat(data.admission().admissionId()).isEqualTo("admission-1");
        assertThat(data.options().doctors()).singleElement().satisfies(option ->
                assertThat(option.id()).isEqualTo(9L));
        assertThat(data.options().rooms()).singleElement().satisfies(option -> {
            assertThat(option.id()).isEqualTo(12L);
            assertThat(option.currentOccupancy()).isEqualTo(10);
        });
        assertThat(data.options().types())
                .containsExactly(new AdmissionOptionsResponse.LookupOption(2, "Legacy urgent"));
    }

    @Test
    void getEditData_missingLegacyType_usesFallbackOption() {
        AdmissionEntity admission = activeAdmission(4L, 9L, null, 3);
        when(admissionRepository.findByIdAndPatientId("admission-1", PATIENT_ID))
                .thenReturn(Optional.of(admission));
        when(admissionRepository.findDepartmentOptions()).thenReturn(List.of(department(4L)));
        when(admissionRepository.findActiveDoctorOptions()).thenReturn(List.of(doctor(9L, 4L, 1)));
        when(admissionRepository.findAvailableRoomOptions()).thenReturn(List.of());
        when(admissionRepository.findActiveAdmissionTypes()).thenReturn(List.of());
        when(admissionRepository.findAdmissionType(3)).thenReturn(Optional.empty());

        AdmissionEditDataResponse data = service.getEditData(PATIENT_ID, "admission-1");

        assertThat(data.options().types())
                .containsExactly(new AdmissionOptionsResponse.LookupOption(3, "Admission type 3"));
    }

    @Test
    void getEditData_dischargedAdmission_rejectsEdit() {
        AdmissionEntity admission = activeAdmission(4L, 9L, null, 2);
        admission.setDischargeDate(LocalDate.now());
        when(admissionRepository.findByIdAndPatientId("admission-1", PATIENT_ID))
                .thenReturn(Optional.of(admission));

        assertThatThrownBy(() -> service.getEditData(PATIENT_ID, "admission-1"))
                .isInstanceOf(InvalidAdmissionOperationException.class)
                .hasFieldOrPropertyWithValue("field", "admissionId");

        verify(admissionRepository, never()).findDepartmentOptions();
    }

    @Test
    void create_validRequest_reservesRoomAndCreatesActiveAdmission() {
        CreateAdmissionRequest request = request(4L, 9L, 12L, 2);
        RoomRow room = room(12L, 4L, 10, 6, 1);
        allowCreate(request, doctor(9L, 15L, 1));
        when(admissionRepository.findRoom(12L)).thenReturn(Optional.of(room));
        when(admissionRepository.occupyRoom(12L, 4L)).thenReturn(1);
        when(admissionRepository.saveAndFlush(any(AdmissionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionResponse created = service.create(PATIENT_ID, request);

        assertThatCodeIsUuid(created.admissionId());
        assertThat(created.patientId()).isEqualTo(PATIENT_ID);
        assertThat(created.status()).isEqualTo(1);
        assertThat(created.dischargeDate()).isNull();
        assertThat(created.reason()).isEqualTo("Needs monitoring");
        verify(patientStatusTransitionService).requireExamining(any(PatientEntity.class));
        verify(patientStatusTransitionService)
                .alignWithActiveAdmissionRoom(any(PatientEntity.class), org.mockito.ArgumentMatchers.eq(true));
        verify(admissionRepository).occupyRoom(12L, 4L);
        ArgumentCaptor<AuditEventCommand> auditCaptor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue()).satisfies(event -> {
            assertThat(event.action()).isEqualTo(AuditLogAction.CREATE);
            assertThat(event.entityType()).isEqualTo(AuditLogEntityType.ADMISSION);
            assertThat(event.entityId()).isEqualTo(created.admissionId());
            assertThat(event.subjectId()).isEqualTo(PATIENT_ID);
            assertThat(event.subjectLabel()).isEqualTo("Patient Test · " + PATIENT_ID);
            assertThat(event.summary()).isEqualTo("Record created");
            assertThat(event.changes()).extracting(AuditLogChange::fieldKey)
                    .contains("departmentId", "doctorId", "roomId", "admissionDate", "type", "status");
        });
    }

    @Test
    void create_doctorMayBeAssignedAcrossDepartments() {
        CreateAdmissionRequest request = request(4L, 9L, null, null);
        allowCreate(request, doctor(9L, 15L, 1));
        when(admissionRepository.saveAndFlush(any(AdmissionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionResponse created = service.create(PATIENT_ID, request);

        assertThat(created.departmentId()).isEqualTo(4L);
        assertThat(created.doctorId()).isEqualTo(9L);
        assertThat(created.type()).isNull();
        verify(patientStatusTransitionService)
                .alignWithActiveAdmissionRoom(any(PatientEntity.class), org.mockito.ArgumentMatchers.eq(false));
        verify(admissionRepository, never()).countActiveAdmissionType(any());
    }

    @Test
    void create_unknownPatient_throwsNotFound() {
        when(patientRepository.findByIdForUpdate(PATIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(PATIENT_ID, request(4L, 9L, null, 2)))
                .isInstanceOf(PatientNotFoundException.class);
        verify(admissionRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_existingActiveAdmission_throwsConflict() {
        allowPatient();
        when(admissionRepository.existsByPatientIdAndDischargeDateIsNull(PATIENT_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(PATIENT_ID, request(4L, 9L, null, 2)))
                .isInstanceOf(AdmissionConflictException.class)
                .hasMessageContaining("active admission");
    }

    @Test
    void create_inactiveDoctor_returnsFieldError() {
        CreateAdmissionRequest request = request(4L, 9L, null, 2);
        DepartmentRow department = department(4L);
        DoctorRow inactiveDoctor = doctor(9L, 4L, 0);
        allowPatient();
        when(admissionRepository.findDepartment(4L)).thenReturn(Optional.of(department));
        when(admissionRepository.findDoctor(9L)).thenReturn(Optional.of(inactiveDoctor));

        assertThatThrownBy(() -> service.create(PATIENT_ID, request))
                .isInstanceOf(InvalidAdmissionOperationException.class)
                .hasFieldOrPropertyWithValue("field", "doctorId");
    }

    @Test
    void create_roomFromAnotherDepartment_returnsFieldErrorWithoutOccupyingIt() {
        CreateAdmissionRequest request = request(4L, 9L, 12L, 2);
        RoomRow room = room(12L, 5L, 10, 6, 1);
        allowCreate(request, doctor(9L, 4L, 1));
        when(admissionRepository.findRoom(12L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.create(PATIENT_ID, request))
                .isInstanceOf(InvalidAdmissionOperationException.class)
                .hasFieldOrPropertyWithValue("field", "roomId");
        verify(admissionRepository, never()).occupyRoom(any(), any());
    }

    @Test
    void create_databaseRace_returnsConflict() {
        CreateAdmissionRequest request = request(4L, 9L, null, 2);
        allowCreate(request, doctor(9L, 4L, 1));
        when(admissionRepository.saveAndFlush(any(AdmissionEntity.class)))
                .thenThrow(new DataIntegrityViolationException("unique index"));

        assertThatThrownBy(() -> service.create(PATIENT_ID, request))
                .isInstanceOf(AdmissionConflictException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
        verify(auditLogService, never()).record(any());
    }

    @Test
    void create_roomBecomesFullDuringReservation_returnsFieldError() {
        CreateAdmissionRequest request = request(4L, 9L, 12L, 2);
        RoomRow room = room(12L, 4L, 10, 6, 1);
        allowCreate(request, doctor(9L, 4L, 1));
        when(admissionRepository.findRoom(12L)).thenReturn(Optional.of(room));
        when(admissionRepository.occupyRoom(12L, 4L)).thenReturn(0);

        assertThatThrownBy(() -> service.create(PATIENT_ID, request))
                .isInstanceOf(InvalidAdmissionOperationException.class)
                .hasFieldOrPropertyWithValue("field", "roomId");
        verify(admissionRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_inactiveAdmissionType_returnsFieldError() {
        CreateAdmissionRequest request = request(4L, 9L, null, 2);
        DepartmentRow department = department(4L);
        DoctorRow doctor = doctor(9L, 4L, 1);
        allowPatient();
        when(admissionRepository.existsByPatientIdAndDischargeDateIsNull(PATIENT_ID))
                .thenReturn(false);
        when(admissionRepository.findDepartment(4L)).thenReturn(Optional.of(department));
        when(admissionRepository.findDoctor(9L)).thenReturn(Optional.of(doctor));
        when(admissionRepository.countActiveAdmissionType(2)).thenReturn(0L);

        assertThatThrownBy(() -> service.create(PATIENT_ID, request))
                .isInstanceOf(InvalidAdmissionOperationException.class)
                .hasFieldOrPropertyWithValue("field", "type");
    }

    @Test
    void update_sameLegacyReferences_updatesFieldsWithoutChangingOccupancyOrPatientStatus() {
        AdmissionEntity admission = activeAdmission(4L, 9L, 12L, 2);
        admission.setStatus(7);
        PatientEntity patient = PatientEntity.builder().id(PATIENT_ID).status(2).build();
        UpdateAdmissionRequest request = updateRequest(4L, 9L, 12L, 2);
        when(patientRepository.findByIdForUpdate(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(admissionRepository.findByIdAndPatientId("admission-1", PATIENT_ID))
                .thenReturn(Optional.of(admission));
        when(admissionRepository.findDepartment(4L)).thenReturn(Optional.of(department(4L)));
        when(admissionRepository.findDoctor(9L)).thenReturn(Optional.of(doctor(9L, 4L, 0)));
        when(admissionRepository.findRoom(12L)).thenReturn(
                Optional.of(room(12L, 4L, 10, 10, 0)));
        when(admissionRepository.saveAndFlush(admission)).thenReturn(admission);

        AdmissionResponse updated = service.update(PATIENT_ID, "admission-1", request);

        assertThat(updated.status()).isEqualTo(7);
        assertThat(updated.dischargeDate()).isNull();
        assertThat(updated.reason()).isEqualTo("Updated reason");
        assertThat(updated.notes()).isEqualTo("Updated notes");
        assertThat(patient.getStatus()).isEqualTo(2);
        verify(admissionRepository, never()).occupyRoom(any(), any());
        verify(admissionRepository, never()).releaseRoom(any());
        verify(admissionRepository, never()).countActiveAdmissionType(any());
        verify(patientRepository, never()).save(any(PatientEntity.class));
        ArgumentCaptor<AuditEventCommand> auditCaptor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditLogAction.UPDATE);
        assertThat(auditCaptor.getValue().targetLabel()).startsWith("Admission ").contains("admissio");
        assertThat(auditCaptor.getValue().summary()).isEqualTo("3 fields changed");
        assertThat(auditCaptor.getValue().changes()).extracting(AuditLogChange::fieldKey)
                .containsExactly("admissionDate", "reason", "notes");
    }

    @Test
    void update_switchRoom_reservesNewRoomBeforeSavingAndReleasingOldRoom() {
        AdmissionEntity admission = activeAdmission(4L, 9L, 12L, 2);
        UpdateAdmissionRequest request = updateRequest(4L, 9L, 13L, 2);
        allowUpdate(admission, request, doctor(9L, 4L, 1));
        when(admissionRepository.findRoom(13L)).thenReturn(
                Optional.of(room(13L, 4L, 10, 5, 1)));
        when(admissionRepository.findRoom(12L)).thenReturn(
                Optional.of(room(12L, 4L, 10, 5, 1)));
        when(admissionRepository.occupyRoom(13L, 4L)).thenReturn(1);
        when(admissionRepository.saveAndFlush(admission)).thenReturn(admission);
        when(admissionRepository.releaseRoom(12L)).thenReturn(1);

        AdmissionResponse updated = service.update(PATIENT_ID, "admission-1", request);

        assertThat(updated.roomId()).isEqualTo(13L);
        verify(patientStatusTransitionService)
                .alignWithActiveAdmissionRoom(any(PatientEntity.class), org.mockito.ArgumentMatchers.eq(true));
        InOrder mutationOrder = Mockito.inOrder(admissionRepository);
        mutationOrder.verify(admissionRepository).occupyRoom(13L, 4L);
        mutationOrder.verify(admissionRepository).saveAndFlush(admission);
        mutationOrder.verify(admissionRepository).releaseRoom(12L);
        ArgumentCaptor<AuditEventCommand> auditCaptor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().changes())
                .filteredOn(change -> change.fieldKey().equals("roomId"))
                .singleElement()
                .satisfies(change -> {
                    assertThat(change.oldDisplayValue()).isEqualTo("Room 12");
                    assertThat(change.newDisplayValue()).isEqualTo("Room 13");
                });
    }

    @Test
    void update_removeRoom_savesBeforeReleasingOldRoom() {
        AdmissionEntity admission = activeAdmission(4L, 9L, 12L, 2);
        UpdateAdmissionRequest request = updateRequest(4L, 9L, null, 2);
        allowUpdate(admission, request, doctor(9L, 4L, 1));
        when(admissionRepository.saveAndFlush(admission)).thenReturn(admission);
        when(admissionRepository.releaseRoom(12L)).thenReturn(1);

        AdmissionResponse updated = service.update(PATIENT_ID, "admission-1", request);

        assertThat(updated.roomId()).isNull();
        verify(patientStatusTransitionService)
                .alignWithActiveAdmissionRoom(any(PatientEntity.class), org.mockito.ArgumentMatchers.eq(false));
        InOrder mutationOrder = Mockito.inOrder(admissionRepository);
        mutationOrder.verify(admissionRepository).saveAndFlush(admission);
        mutationOrder.verify(admissionRepository).releaseRoom(12L);
        verify(admissionRepository, never()).occupyRoom(any(), any());
    }

    @Test
    void update_dischargedAdmission_rejectsEditBeforeReferenceMutation() {
        AdmissionEntity admission = activeAdmission(4L, 9L, null, 2);
        admission.setDischargeDate(LocalDate.now());
        allowPatient();
        when(admissionRepository.findByIdAndPatientId("admission-1", PATIENT_ID))
                .thenReturn(Optional.of(admission));

        assertThatThrownBy(() -> service.update(
                PATIENT_ID,
                "admission-1",
                updateRequest(4L, 9L, null, 2)))
                .isInstanceOf(InvalidAdmissionOperationException.class)
                .hasFieldOrPropertyWithValue("field", "admissionId");

        verify(admissionRepository, never()).findDepartment(any());
        verify(admissionRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_admissionBelongsToAnotherPatient_throwsNotFound() {
        allowPatient();
        when(admissionRepository.findByIdAndPatientId("admission-1", PATIENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                PATIENT_ID,
                "admission-1",
                updateRequest(4L, 9L, null, 2)))
                .isInstanceOf(AdmissionNotFoundException.class);
    }

    @Test
    void update_changedInactiveDoctor_returnsFieldError() {
        AdmissionEntity admission = activeAdmission(4L, 9L, null, 2);
        UpdateAdmissionRequest request = updateRequest(4L, 10L, null, 2);
        allowPatient();
        when(admissionRepository.findByIdAndPatientId("admission-1", PATIENT_ID))
                .thenReturn(Optional.of(admission));
        when(admissionRepository.findDepartment(4L)).thenReturn(Optional.of(department(4L)));
        when(admissionRepository.findDoctor(10L)).thenReturn(Optional.of(doctor(10L, 4L, 0)));

        assertThatThrownBy(() -> service.update(PATIENT_ID, "admission-1", request))
                .isInstanceOf(InvalidAdmissionOperationException.class)
                .hasFieldOrPropertyWithValue("field", "doctorId");

        verify(admissionRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_changedInactiveType_returnsFieldError() {
        AdmissionEntity admission = activeAdmission(4L, 9L, null, 1);
        UpdateAdmissionRequest request = updateRequest(4L, 9L, null, 2);
        allowPatient();
        when(admissionRepository.findByIdAndPatientId("admission-1", PATIENT_ID))
                .thenReturn(Optional.of(admission));
        when(admissionRepository.findDepartment(4L)).thenReturn(Optional.of(department(4L)));
        when(admissionRepository.findDoctor(9L)).thenReturn(Optional.of(doctor(9L, 4L, 1)));
        when(admissionRepository.countActiveAdmissionType(2)).thenReturn(0L);

        assertThatThrownBy(() -> service.update(PATIENT_ID, "admission-1", request))
                .isInstanceOf(InvalidAdmissionOperationException.class)
                .hasFieldOrPropertyWithValue("field", "type");

        verify(admissionRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_changedRoomFromAnotherDepartment_rejectsWithoutOccupyingIt() {
        AdmissionEntity admission = activeAdmission(4L, 9L, 12L, 2);
        UpdateAdmissionRequest request = updateRequest(4L, 9L, 13L, 2);
        allowUpdate(admission, request, doctor(9L, 4L, 1));
        when(admissionRepository.findRoom(13L)).thenReturn(
                Optional.of(room(13L, 5L, 10, 5, 1)));

        assertThatThrownBy(() -> service.update(PATIENT_ID, "admission-1", request))
                .isInstanceOf(InvalidAdmissionOperationException.class)
                .hasFieldOrPropertyWithValue("field", "roomId");

        verify(admissionRepository, never()).occupyRoom(any(), any());
        verify(admissionRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_newRoomBecomesFullDuringReservation_rejectsWithoutSaving() {
        AdmissionEntity admission = activeAdmission(4L, 9L, 12L, 2);
        UpdateAdmissionRequest request = updateRequest(4L, 9L, 13L, 2);
        allowUpdate(admission, request, doctor(9L, 4L, 1));
        when(admissionRepository.findRoom(13L)).thenReturn(
                Optional.of(room(13L, 4L, 10, 5, 1)));
        when(admissionRepository.findRoom(12L)).thenReturn(
                Optional.of(room(12L, 4L, 10, 5, 1)));
        when(admissionRepository.occupyRoom(13L, 4L)).thenReturn(0);

        assertThatThrownBy(() -> service.update(PATIENT_ID, "admission-1", request))
                .isInstanceOf(InvalidAdmissionOperationException.class)
                .hasFieldOrPropertyWithValue("field", "roomId");

        verify(admissionRepository, never()).saveAndFlush(any());
        verify(admissionRepository, never()).releaseRoom(any());
    }

    @Test
    void update_releaseOldRoomFails_throwsConflictToRollBackTransaction() {
        AdmissionEntity admission = activeAdmission(4L, 9L, 12L, 2);
        UpdateAdmissionRequest request = updateRequest(4L, 9L, null, 2);
        allowUpdate(admission, request, doctor(9L, 4L, 1));
        when(admissionRepository.saveAndFlush(admission)).thenReturn(admission);
        when(admissionRepository.releaseRoom(12L)).thenReturn(0);

        assertThatThrownBy(() -> service.update(PATIENT_ID, "admission-1", request))
                .isInstanceOf(AdmissionConflictException.class)
                .hasMessageContaining("occupancy");
        verify(auditLogService, never()).record(any());
    }

    @Test
    void discharge_activeAdmission_setsDatesReleasesRoomAndTransitionsPatient() {
        AdmissionEntity admission = activeAdmission(4L, 9L, 12L, 2);
        PatientEntity patient = allowPatient();
        when(admissionRepository.findByIdAndPatientId("admission-1", PATIENT_ID))
                .thenReturn(Optional.of(admission));
        when(admissionRepository.findDepartment(4L)).thenReturn(Optional.of(department(4L)));
        when(admissionRepository.findDoctor(9L)).thenReturn(Optional.of(doctor(9L, 4L, 1)));
        when(admissionRepository.findRoom(12L)).thenReturn(
                Optional.of(room(12L, 4L, 10, 5, 1)));
        when(admissionRepository.findAdmissionType(2)).thenReturn(Optional.of(lookup(2, "Urgent")));
        when(admissionRepository.saveAndFlush(admission)).thenReturn(admission);
        when(admissionRepository.releaseRoom(12L)).thenReturn(1);

        AdmissionResponse discharged = service.discharge(PATIENT_ID, "admission-1");

        assertThat(discharged.dischargeDate()).isEqualTo(LocalDate.now());
        assertThat(discharged.status()).isEqualTo(2);
        verify(admissionRepository).releaseRoom(12L);
        verify(patientStatusTransitionService).discharge(patient);
        ArgumentCaptor<AuditEventCommand> auditCaptor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditLogAction.STATUS_UPDATE);
        assertThat(auditCaptor.getValue().summary()).isEqualTo("In treatment to Discharged");
        assertThat(auditCaptor.getValue().changes()).extracting(AuditLogChange::fieldKey)
                .containsExactly("dischargeDate", "status");
    }

    @Test
    void discharge_roomReleaseFails_rejectsBeforePatientTransitionOrAudit() {
        AdmissionEntity admission = activeAdmission(4L, 9L, 12L, 2);
        allowPatient();
        when(admissionRepository.findByIdAndPatientId("admission-1", PATIENT_ID))
                .thenReturn(Optional.of(admission));
        when(admissionRepository.findDepartment(4L)).thenReturn(Optional.of(department(4L)));
        when(admissionRepository.findDoctor(9L)).thenReturn(Optional.of(doctor(9L, 4L, 1)));
        when(admissionRepository.findRoom(12L)).thenReturn(
                Optional.of(room(12L, 4L, 10, 5, 1)));
        when(admissionRepository.findAdmissionType(2)).thenReturn(Optional.of(lookup(2, "Urgent")));
        when(admissionRepository.saveAndFlush(admission)).thenReturn(admission);
        when(admissionRepository.releaseRoom(12L)).thenReturn(0);

        assertThatThrownBy(() -> service.discharge(PATIENT_ID, "admission-1"))
                .isInstanceOf(AdmissionConflictException.class)
                .hasMessageContaining("occupancy");

        verify(patientStatusTransitionService, never()).discharge(any());
        verify(auditLogService, never()).record(any());
    }

    @Test
    void delete_activeAdmission_releasesItsRoom() {
        AdmissionEntity admission = activeAdmission(4L, 9L, 12L, 2);
        allowPatient();
        when(admissionRepository.findByIdAndPatientId("admission-1", PATIENT_ID))
                .thenReturn(Optional.of(admission));
        when(admissionRepository.findDepartment(4L)).thenReturn(Optional.of(department(4L)));
        when(admissionRepository.findDoctor(9L)).thenReturn(Optional.of(doctor(9L, 4L, 1)));
        when(admissionRepository.findRoom(12L)).thenReturn(
                Optional.of(room(12L, 4L, 10, 5, 1)));
        when(admissionRepository.findAdmissionType(2)).thenReturn(Optional.of(lookup(2, "Urgent")));

        service.delete(PATIENT_ID, "admission-1");

        verify(admissionRepository).delete(admission);
        verify(admissionRepository).flush();
        verify(admissionRepository).releaseRoom(12L);
        verify(patientStatusTransitionService)
                .restoreExaminingAfterActiveAdmissionDeletion(any(PatientEntity.class));
        ArgumentCaptor<AuditEventCommand> auditCaptor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue()).satisfies(event -> {
            assertThat(event.action()).isEqualTo(AuditLogAction.DELETE);
            assertThat(event.entityId()).isEqualTo("admission-1");
            assertThat(event.subjectLabel()).isEqualTo("Patient Test · " + PATIENT_ID);
            assertThat(event.summary()).isEqualTo("Permanently deleted");
            assertThat(event.changes()).allSatisfy(change -> assertThat(change.newValue()).isNull());
            assertThat(event.changes())
                    .filteredOn(change -> change.fieldKey().equals("departmentId"))
                    .singleElement()
                    .satisfies(change -> assertThat(change.oldDisplayValue()).isEqualTo("Department 4"));
            assertThat(event.changes())
                    .filteredOn(change -> change.fieldKey().equals("doctorId"))
                    .singleElement()
                    .satisfies(change -> assertThat(change.oldDisplayValue()).isEqualTo("Doctor 9"));
            assertThat(event.changes())
                    .filteredOn(change -> change.fieldKey().equals("roomId"))
                    .singleElement()
                    .satisfies(change -> assertThat(change.oldDisplayValue()).isEqualTo("Room 12"));
            assertThat(event.changes())
                    .filteredOn(change -> change.fieldKey().equals("type"))
                    .singleElement()
                    .satisfies(change -> assertThat(change.oldDisplayValue()).isEqualTo("Urgent"));
        });
    }

    @Test
    void delete_dischargedAdmission_doesNotReleaseRoomAgain() {
        AdmissionEntity admission = AdmissionEntity.builder()
                .id("admission-1")
                .patientId(PATIENT_ID)
                .roomId(12L)
                .dischargeDate(LocalDate.now())
                .build();
        allowPatient();
        when(admissionRepository.findByIdAndPatientId("admission-1", PATIENT_ID))
                .thenReturn(Optional.of(admission));

        service.delete(PATIENT_ID, "admission-1");

        verify(admissionRepository, never()).releaseRoom(any());
    }

    @Test
    void delete_unknownAdmission_throwsNotFound() {
        allowPatient();
        when(admissionRepository.findByIdAndPatientId("missing", PATIENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(PATIENT_ID, "missing"))
                .isInstanceOf(AdmissionNotFoundException.class);
        verify(auditLogService, never()).record(any());
    }

    private void allowCreate(CreateAdmissionRequest request, DoctorRow doctor) {
        DepartmentRow department = department(request.departmentId());
        allowPatient();
        when(admissionRepository.existsByPatientIdAndDischargeDateIsNull(PATIENT_ID))
                .thenReturn(false);
        when(admissionRepository.findDepartment(request.departmentId()))
                .thenReturn(Optional.of(department));
        when(admissionRepository.findDoctor(request.doctorId())).thenReturn(Optional.of(doctor));
        if (request.type() != null) {
            when(admissionRepository.countActiveAdmissionType(request.type())).thenReturn(1L);
        }
    }

    private void allowUpdate(
            AdmissionEntity admission,
            UpdateAdmissionRequest request,
            DoctorRow doctor
    ) {
        allowPatient();
        when(admissionRepository.findByIdAndPatientId("admission-1", PATIENT_ID))
                .thenReturn(Optional.of(admission));
        when(admissionRepository.findDepartment(request.departmentId()))
                .thenReturn(Optional.of(department(request.departmentId())));
        when(admissionRepository.findDoctor(request.doctorId())).thenReturn(Optional.of(doctor));
        if (!java.util.Objects.equals(admission.getType(), request.type()) && request.type() != null) {
            when(admissionRepository.countActiveAdmissionType(request.type())).thenReturn(1L);
        }
    }

    private PatientEntity allowPatient() {
        PatientEntity patient = PatientEntity.builder()
                .id(PATIENT_ID)
                .name("Patient Test")
                .status(2)
                .build();
        when(patientRepository.findByIdForUpdate(PATIENT_ID)).thenReturn(Optional.of(patient));
        return patient;
    }

    private static CreateAdmissionRequest request(
            Long departmentId,
            Long doctorId,
            Long roomId,
            Integer type
    ) {
        return new CreateAdmissionRequest(
                departmentId,
                doctorId,
                roomId,
                LocalDate.now(),
                "  Needs monitoring  ",
                type,
                "  Stable  "
        );
    }

    private static UpdateAdmissionRequest updateRequest(
            Long departmentId,
            Long doctorId,
            Long roomId,
            Integer type
    ) {
        return new UpdateAdmissionRequest(
                departmentId,
                doctorId,
                roomId,
                LocalDate.now(),
                "  Updated reason  ",
                type,
                "  Updated notes  "
        );
    }

    private static AdmissionEntity activeAdmission(
            Long departmentId,
            Long doctorId,
            Long roomId,
            Integer type
    ) {
        return AdmissionEntity.builder()
                .id("admission-1")
                .patientId(PATIENT_ID)
                .departmentId(departmentId)
                .doctorId(doctorId)
                .roomId(roomId)
                .admissionDate(LocalDate.now().minusDays(1))
                .reason("Original reason")
                .type(type)
                .status(1)
                .notes("Original notes")
                .build();
    }

    private static DepartmentRow department(Long id) {
        return new DepartmentRow() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getCode() {
                return "DEP" + id;
            }

            @Override
            public String getName() {
                return "Department " + id;
            }
        };
    }

    private static DoctorRow doctor(Long id, Long departmentId, Integer status) {
        return new DoctorRow() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getCode() {
                return "DOC" + id;
            }

            @Override
            public String getName() {
                return "Doctor " + id;
            }

            @Override
            public Long getDepartmentId() {
                return departmentId;
            }

            @Override
            public Integer getStatus() {
                return status;
            }
        };
    }

    private static RoomRow room(
            Long id,
            Long departmentId,
            Integer beds,
            Integer occupancy,
            Integer status
    ) {
        return new RoomRow() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getName() {
                return "Room " + id;
            }

            @Override
            public Long getDepartmentId() {
                return departmentId;
            }

            @Override
            public Integer getBedCount() {
                return beds;
            }

            @Override
            public Integer getCurrentOccupancy() {
                return occupancy;
            }

            @Override
            public Integer getStatus() {
                return status;
            }
        };
    }

    private static LookupRow lookup(Integer value, String label) {
        return new LookupRow() {
            @Override
            public Integer getValue() {
                return value;
            }

            @Override
            public String getLabel() {
                return label;
            }
        };
    }

    private static void assertThatCodeIsUuid(String value) {
        assertThat(UUID.fromString(value)).isNotNull();
    }
}
