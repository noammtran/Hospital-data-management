package com.usth.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.usth.hospital.exception.PatientStatusConflictException;
import com.usth.hospital.mapper.PatientMapper;
import com.usth.hospital.model.AuditLogAction;
import com.usth.hospital.model.AuditLogEntityType;
import com.usth.hospital.model.PatientEntity;
import com.usth.hospital.model.PatientHistoryEntity;
import com.usth.hospital.model.PatientStatus;
import com.usth.hospital.repository.AdmissionRepository;
import com.usth.hospital.repository.CareVisitRepository;
import com.usth.hospital.repository.PatientHistoryRepository;
import com.usth.hospital.repository.PatientRepository;
import com.usth.hospital.security.CurrentUserProvider;
import com.usth.hospital.service.impl.PatientStatusTransitionService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientStatusTransitionServiceTest {

    private static final String PATIENT_ID = "patient-1";

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AdmissionRepository admissionRepository;

    @Mock
    private CareVisitRepository careVisitRepository;

    @Mock
    private PatientHistoryRepository patientHistoryRepository;

    @Spy
    private PatientMapper patientMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private PatientStatusTransitionService service;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.username()).thenReturn("doctor1");
        lenient().when(patientRepository.save(any(PatientEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void startExamination_waitingPatient_recordsHistoryAndAudit() {
        PatientEntity patient = patient(PatientStatus.WAITING);
        when(patientRepository.findByIdForUpdate(PATIENT_ID)).thenReturn(Optional.of(patient));

        var result = service.startExamination(PATIENT_ID);

        assertThat(result.status()).isEqualTo(PatientStatus.EXAMINING.code());
        assertThat(patient.getLastUpdatedBy()).isEqualTo("doctor1");
        assertThat(patient.getLastUpdatedDate()).isEqualTo(LocalDate.now());

        ArgumentCaptor<PatientHistoryEntity> historyCaptor =
                ArgumentCaptor.forClass(PatientHistoryEntity.class);
        verify(patientHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue()).satisfies(history -> {
            assertThat(history.getPatientId()).isEqualTo(PATIENT_ID);
            assertThat(history.getFieldName()).isEqualTo("status");
            assertThat(history.getOldValue()).isEqualTo("0");
            assertThat(history.getNewValue()).isEqualTo("1");
            assertThat(history.getChangedBy()).isEqualTo("doctor1");
        });

        ArgumentCaptor<AuditEventCommand> auditCaptor =
                ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue()).satisfies(event -> {
            assertThat(event.action()).isEqualTo(AuditLogAction.STATUS_UPDATE);
            assertThat(event.entityType()).isEqualTo(AuditLogEntityType.PATIENT);
            assertThat(event.summary()).isEqualTo("Waiting to Examining");
            assertThat(event.changes()).singleElement().satisfies(change -> {
                assertThat(change.fieldKey()).isEqualTo("status");
                assertThat(change.oldDisplayValue()).isEqualTo("Waiting");
                assertThat(change.newDisplayValue()).isEqualTo("Examining");
            });
        });
    }

    @Test
    void legacyActionCannotBypassAnOpenCareVisit() {
        PatientEntity patient = patient(PatientStatus.EXAMINING);
        when(patientRepository.findByIdForUpdate(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(careVisitRepository.existsByPatientIdAndClosedAtIsNull(PATIENT_ID)).thenReturn(true);
        assertThatThrownBy(() -> service.completeExamination(PATIENT_ID))
                .isInstanceOf(PatientStatusConflictException.class).hasMessageContaining("care visit");
        verify(auditLogService, never()).record(any());
        assertThat(patient.getStatus()).isEqualTo(PatientStatus.EXAMINING.code());
    }

    @Test
    void checkIn_waitingPatient_rejectsOutOfOrderTransition() {
        PatientEntity patient = patient(PatientStatus.WAITING);
        when(patientRepository.findByIdForUpdate(PATIENT_ID)).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> service.checkIn(PATIENT_ID))
                .isInstanceOf(PatientStatusConflictException.class)
                .hasMessage("Patient must be Discharged before this action");

        verify(patientRepository, never()).save(any());
        verify(patientHistoryRepository, never()).save(any());
        verify(auditLogService, never()).record(any());
    }

    @Test
    void completeExamination_activeEpisode_rejectsDirectCompletion() {
        PatientEntity patient = patient(PatientStatus.EXAMINING);
        when(patientRepository.findByIdForUpdate(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(admissionRepository.existsByPatientIdAndDischargeDateIsNull(PATIENT_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.completeExamination(PATIENT_ID))
                .isInstanceOf(PatientStatusConflictException.class)
                .hasMessageContaining("active treatment episode");

        verify(patientRepository, never()).save(any());
        verify(auditLogService, never()).record(any());
    }

    @Test
    void alignWithActiveAdmissionRoom_roomAdded_setsAdmitted() {
        PatientEntity patient = patient(PatientStatus.EXAMINING);

        service.alignWithActiveAdmissionRoom(patient, true);

        assertThat(patient.getStatus()).isEqualTo(PatientStatus.ADMITTED.code());
        verify(patientHistoryRepository).save(any(PatientHistoryEntity.class));
        verify(auditLogService).record(any(AuditEventCommand.class));
    }

    @Test
    void discharge_admittedPatient_setsDischarged() {
        PatientEntity patient = patient(PatientStatus.ADMITTED);

        service.discharge(patient);

        assertThat(patient.getStatus()).isEqualTo(PatientStatus.DISCHARGED.code());
        verify(patientRepository).save(patient);
    }

    private static PatientEntity patient(PatientStatus status) {
        return PatientEntity.builder()
                .id(PATIENT_ID)
                .name("Nguyen Van A")
                .status(status.code())
                .createdBy("SYSTEM")
                .createdDate(LocalDate.now())
                .lastUpdatedBy("SYSTEM")
                .lastUpdatedDate(LocalDate.now())
                .build();
    }
}
