package com.usth.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.usth.hospital.dto.request.CreatePatientRequest;
import com.usth.hospital.dto.request.UpdatePatientRequest;
import com.usth.hospital.dto.response.PatientDetailResponse;
import com.usth.hospital.dto.response.PatientHistoryEntryResponse;
import com.usth.hospital.dto.response.PatientHistoryResponse;
import com.usth.hospital.dto.response.PatientListResponse;
import com.usth.hospital.exception.DuplicateFieldException;
import com.usth.hospital.exception.PatientNotFoundException;
import com.usth.hospital.mapper.PatientMapper;
import com.usth.hospital.model.AuditLogAction;
import com.usth.hospital.model.AuditLogEntityType;
import com.usth.hospital.model.AuditLogSensitivity;
import com.usth.hospital.model.PatientEntity;
import com.usth.hospital.model.PatientHistoryEntity;
import com.usth.hospital.repository.AdmissionRepository;
import com.usth.hospital.repository.PatientHistoryRepository;
import com.usth.hospital.repository.PatientRepository;
import com.usth.hospital.security.CurrentUserProvider;
import com.usth.hospital.service.PatientIdGenerator;
import com.usth.hospital.service.impl.PatientServiceImpl;
import com.usth.hospital.service.impl.PatientAccountLifecycleService;
import com.usth.hospital.service.impl.PatientStatusTransitionService;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    private static final String PATIENT_ID = "patient-1";

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AdmissionRepository admissionRepository;

    @Mock
    private PatientHistoryRepository patientHistoryRepository;

    @Spy
    private PatientMapper patientMapper;

    @Spy
    private PatientIdGenerator patientIdGenerator = new PatientIdGenerator();

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PatientStatusTransitionService patientStatusTransitionService;

    @Mock
    private PatientAccountLifecycleService patientAccountLifecycleService;

    @InjectMocks
    private PatientServiceImpl service;

    @Captor
    private ArgumentCaptor<List<PatientHistoryEntity>> historyCaptor;

    private PatientEntity existing;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.username()).thenReturn("doctor1");
        existing = PatientEntity.builder()
                .id(PATIENT_ID)
                .name("Nguyen Van A")
                .gender(1)
                .phoneNo("0900000000")
                .status(0)
                .createdBy("SYSTEM")
                .createdDate(LocalDate.now())
                .lastUpdatedBy("SYSTEM")
                .lastUpdatedDate(LocalDate.now())
                .build();
    }

    private UpdatePatientRequest request(String name, String phoneNo) {
        return new UpdatePatientRequest(name, null, 1, null, phoneNo, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    private CreatePatientRequest createWithIdentity(String identityNo) {
        return new CreatePatientRequest(
                "Nguyen Van A", LocalDate.of(1990, 1, 2), 1, null, "0900000000", null,
                identityNo, 7, null, null, null, LocalDate.of(2027, 12, 31),
                null, null, null, null);
    }

    private UpdatePatientRequest updateWithIdentity(String identityNo) {
        return new UpdatePatientRequest("Nguyen Van A", null, 1, null, "0900000000", null,
                identityNo, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void create_duplicateIdentity_throwsDuplicateField() {
        when(patientRepository.existsByIdentityNo("079123456789")).thenReturn(true);

        assertThatThrownBy(() -> service.create(createWithIdentity("079123456789")))
                .isInstanceOf(DuplicateFieldException.class)
                .hasFieldOrPropertyWithValue("field", "identityNo");
        verify(patientRepository, never()).save(any());
        verify(auditLogService, never()).record(any());
    }

    @Test
    void create_uniqueIdentity_savesPatient() {
        when(patientRepository.existsByIdentityNo("079123456789")).thenReturn(false);
        when(patientRepository.save(any(PatientEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PatientDetailResponse created = service.create(createWithIdentity("079123456789"));

        assertThat(created.identityNo()).isEqualTo("079123456789");
        assertThat(created.createdBy()).isEqualTo("doctor1");
        assertThat(created.lastUpdatedBy()).isEqualTo("doctor1");
        verify(patientRepository).save(any(PatientEntity.class));
        verify(patientAccountLifecycleService).createFor(any(PatientEntity.class));
        ArgumentCaptor<AuditEventCommand> auditCaptor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue()).satisfies(event -> {
            assertThat(event.action()).isEqualTo(AuditLogAction.CREATE);
            assertThat(event.entityType()).isEqualTo(AuditLogEntityType.PATIENT);
            assertThat(event.entityId()).isEqualTo(created.id());
            assertThat(event.targetLabel()).isEqualTo("Nguyen Van A · " + created.id());
            assertThat(event.summary()).isEqualTo("Record created");
            assertThat(event.changes()).extracting(AuditLogChange::fieldKey)
                    .contains(
                            "name", "birthDate", "gender", "phoneNo", "identityNo",
                            "bloodType", "insuranceExpiry", "status");
            assertThat(event.changes())
                    .extracting(AuditLogChange::fieldKey, AuditLogChange::sensitivity)
                    .contains(
                            tuple("birthDate", AuditLogSensitivity.PERSONAL),
                            tuple("gender", AuditLogSensitivity.PERSONAL),
                            tuple("insuranceExpiry", AuditLogSensitivity.PERSONAL),
                            tuple("bloodType", AuditLogSensitivity.MEDICAL),
                            tuple("name", AuditLogSensitivity.GENERAL),
                            tuple("status", AuditLogSensitivity.GENERAL)
                    );
        });
    }

    @Test
    void update_duplicateIdentity_throwsDuplicateField() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(java.util.Optional.of(existing));
        when(patientRepository.existsByIdentityNoAndIdNot("079123456789", PATIENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(PATIENT_ID, updateWithIdentity("079123456789")))
                .isInstanceOf(DuplicateFieldException.class)
                .hasFieldOrPropertyWithValue("field", "identityNo");
        verify(patientRepository, never()).save(any());
    }

    @Test
    void update_changesTwoFields_writesOneHistoryRowPerField() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(java.util.Optional.of(existing));
        when(patientRepository.save(any(PatientEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(PATIENT_ID, request("Tran Thi B", "0911111111"));

        verify(patientHistoryRepository).saveAll(historyCaptor.capture());
        verify(patientAccountLifecycleService).synchronize(existing);
        List<PatientHistoryEntity> rows = historyCaptor.getValue();

        assertThat(rows).extracting(PatientHistoryEntity::getFieldName)
                .containsExactlyInAnyOrder("name", "phoneNo");
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getPatientId()).isEqualTo(PATIENT_ID);
            assertThat(row.getChangedBy()).isEqualTo("doctor1");
            assertThat(row.getId()).isNotBlank();
        });
        PatientHistoryEntity nameRow = rows.stream()
                .filter(r -> r.getFieldName().equals("name")).findFirst().orElseThrow();
        assertThat(nameRow.getOldValue()).isEqualTo("Nguyen Van A");
        assertThat(nameRow.getNewValue()).isEqualTo("Tran Thi B");
        ArgumentCaptor<AuditEventCommand> auditCaptor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditLogAction.UPDATE);
        assertThat(auditCaptor.getValue().summary()).isEqualTo("2 fields changed");
        assertThat(auditCaptor.getValue().changes()).extracting(AuditLogChange::fieldKey)
                .containsExactly("name", "phoneNo");
    }

    @Test
    void update_noFieldChanged_writesNoHistory() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(java.util.Optional.of(existing));
        when(patientRepository.save(any(PatientEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(PATIENT_ID, request("Nguyen Van A", "0900000000"));

        verify(patientHistoryRepository, never()).saveAll(any());
        ArgumentCaptor<AuditEventCommand> auditCaptor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().changes()).isEmpty();
        assertThat(auditCaptor.getValue().summary()).isEqualTo("No field changes");
    }

    @Test
    void update_unknownPatient_throwsNotFound() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.update(PATIENT_ID, request("X", "Y")))
                .isInstanceOf(PatientNotFoundException.class);
        verify(patientHistoryRepository, never()).saveAll(any());
        verify(auditLogService, never()).record(any());
    }

    @Test
    void getHistory_unknownPatient_throwsNotFound() {
        when(patientRepository.existsById(PATIENT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getHistory(PATIENT_ID, PageRequest.of(0, 20)))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void delete_releasesActiveAdmissionRoomBeforePatientCascade() {
        when(patientRepository.findByIdForUpdate(PATIENT_ID))
                .thenReturn(java.util.Optional.of(existing));
        when(admissionRepository.findActiveRoomIdsByPatientIds(List.of(PATIENT_ID)))
                .thenReturn(List.of(7L));

        service.delete(PATIENT_ID);

        InOrder mutationOrder = inOrder(patientRepository, admissionRepository);
        mutationOrder.verify(patientRepository).findByIdForUpdate(PATIENT_ID);
        mutationOrder.verify(admissionRepository).findActiveRoomIdsByPatientIds(List.of(PATIENT_ID));
        mutationOrder.verify(admissionRepository).releaseRoom(7L);
        verify(patientAccountLifecycleService).deleteFor(PATIENT_ID);
        mutationOrder.verify(patientRepository).deleteById(PATIENT_ID);
        ArgumentCaptor<AuditEventCommand> auditCaptor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue()).satisfies(event -> {
            assertThat(event.action()).isEqualTo(AuditLogAction.DELETE);
            assertThat(event.entityId()).isEqualTo(PATIENT_ID);
            assertThat(event.targetLabel()).isEqualTo("Nguyen Van A · " + PATIENT_ID);
            assertThat(event.summary()).isEqualTo("Permanently deleted");
            assertThat(event.changes()).allSatisfy(change -> assertThat(change.newValue()).isNull());
        });
    }

    @Test
    void bulkDelete_locksPatientsInStableOrderAndReleasesEveryActiveRoom() {
        String secondPatientId = "P26MB002";
        List<String> requestedIds = List.of(secondPatientId, PATIENT_ID, secondPatientId);
        List<String> orderedIds = List.of(secondPatientId, PATIENT_ID);
        when(patientRepository.findByIdForUpdate(PATIENT_ID))
                .thenReturn(java.util.Optional.of(existing));
        when(patientRepository.findByIdForUpdate(secondPatientId))
                .thenReturn(java.util.Optional.of(PatientEntity.builder()
                        .id(secondPatientId).name("Second Patient").build()));
        when(admissionRepository.findActiveRoomIdsByPatientIds(orderedIds))
                .thenReturn(List.of(7L, 8L));

        service.bulkDelete(requestedIds);

        InOrder lockOrder = inOrder(patientRepository);
        lockOrder.verify(patientRepository).findByIdForUpdate(secondPatientId);
        lockOrder.verify(patientRepository).findByIdForUpdate(PATIENT_ID);
        verify(admissionRepository).releaseRoom(7L);
        verify(admissionRepository).releaseRoom(8L);
        verify(patientAccountLifecycleService).deleteFor(orderedIds);
        verify(patientRepository).deleteAllById(orderedIds);
        ArgumentCaptor<AuditEventCommand> auditCaptor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditLogAction.BULK_DELETE);
        assertThat(auditCaptor.getValue().summary()).isEqualTo("2 records deleted");
        assertThat(auditCaptor.getValue().affectedTargets())
                .extracting(AuditLogTarget::entityId, AuditLogTarget::targetLabel)
                .containsExactly(
                        tuple(secondPatientId, "Second Patient · " + secondPatientId),
                        tuple(PATIENT_ID, "Nguyen Van A · " + PATIENT_ID)
                );
    }

    @Test
    void bulkDelete_recordsEveryAffectedTargetWithoutPackingIdsIntoSummaryText() {
        List<String> requestedIds = IntStream.range(0, 150)
                .mapToObj(index -> "P26AA" + String.format("%03d", index))
                .toList();
        when(patientRepository.findByIdForUpdate(any())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return java.util.Optional.of(PatientEntity.builder().id(id).name("Synthetic Patient").build());
        });
        when(admissionRepository.findActiveRoomIdsByPatientIds(requestedIds)).thenReturn(List.of());

        service.bulkDelete(requestedIds);

        ArgumentCaptor<AuditEventCommand> auditCaptor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().summary()).isEqualTo("150 records deleted");
        assertThat(auditCaptor.getValue().affectedTargets()).hasSize(150);
    }

    @Test
    void getHistory_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 20);
        PatientHistoryEntity row = PatientHistoryEntity.builder()
                .id("h-1").patientId(PATIENT_ID).fieldName("name")
                .oldValue("Nguyen Van A").newValue("Tran Thi B")
                .changedBy("SYSTEM").changedDate(LocalDate.now()).build();
        Page<PatientHistoryEntity> page = new PageImpl<>(List.of(row), pageable, 1);
        when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);
        when(patientHistoryRepository.findByPatientIdOrderByChangedDateDesc(PATIENT_ID, pageable))
                .thenReturn(page);

        Page<PatientHistoryResponse> result = service.getHistory(PATIENT_ID, pageable);

        assertThat(result.getContent()).singleElement().satisfies(r -> {
            assertThat(r.fieldName()).isEqualTo("name");
            assertThat(r.oldValue()).isEqualTo("Nguyen Van A");
            assertThat(r.newValue()).isEqualTo("Tran Thi B");
            assertThat(r.changedBy()).isEqualTo("SYSTEM");
        });
    }

    @Test
    void list_marksPatientsThatHaveHistory() {
        Pageable pageable = PageRequest.of(0, 20);
        PatientEntity other = PatientEntity.builder()
                .id("patient-2").name("Le Van C").gender(1).phoneNo("0922222222").status(1).build();
        Page<PatientEntity> page = new PageImpl<>(List.of(existing, other), pageable, 2);
        when(patientRepository.search(null, "name", null, null, pageable)).thenReturn(page);
        when(patientHistoryRepository.findDistinctPatientIdsByPatientIdIn(List.of(PATIENT_ID, "patient-2")))
                .thenReturn(List.of(PATIENT_ID));

        Page<PatientListResponse> result = service.list(null, null, null, null, pageable);

        assertThat(result.getContent())
                .extracting(PatientListResponse::id, PatientListResponse::hasHistory)
                .containsExactly(tuple(PATIENT_ID, true), tuple("patient-2", false));
    }

    @Test
    void getAllHistory_resolvesPatientNames() {
        Pageable pageable = PageRequest.of(0, 20);
        PatientHistoryEntity row = PatientHistoryEntity.builder()
                .id("h-1").patientId(PATIENT_ID).fieldName("phoneNo")
                .oldValue("0900000000").newValue("0911111111")
                .changedBy("SYSTEM").changedDate(LocalDate.now()).build();
        Page<PatientHistoryEntity> page = new PageImpl<>(List.of(row), pageable, 1);
        when(patientHistoryRepository.findAllByOrderByChangedDateDesc(pageable)).thenReturn(page);
        when(patientRepository.findAllById(java.util.Set.of(PATIENT_ID)))
                .thenReturn(List.of(existing));

        Page<PatientHistoryEntryResponse> result = service.getAllHistory(pageable);

        assertThat(result.getContent()).singleElement().satisfies(r -> {
            assertThat(r.patientId()).isEqualTo(PATIENT_ID);
            assertThat(r.patientName()).isEqualTo("Nguyen Van A");
            assertThat(r.fieldName()).isEqualTo("phoneNo");
            assertThat(r.oldValue()).isEqualTo("0900000000");
            assertThat(r.newValue()).isEqualTo("0911111111");
        });
    }
}
