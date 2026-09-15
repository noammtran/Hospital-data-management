package com.usth.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.usth.hospital.dto.response.AuditLogResponse;
import com.usth.hospital.exception.AuditLogNotFoundException;
import com.usth.hospital.exception.InvalidAuditLogFilterException;
import com.usth.hospital.model.AuditLogAction;
import com.usth.hospital.model.AuditLogChangeEntity;
import com.usth.hospital.model.AuditLogEntity;
import com.usth.hospital.model.AuditLogEntityType;
import com.usth.hospital.model.AuditLogSensitivity;
import com.usth.hospital.model.AuditLogTargetEntity;
import com.usth.hospital.model.AuditLogValueType;
import com.usth.hospital.repository.AuditLogChangeRepository;
import com.usth.hospital.repository.AuditLogRepository;
import com.usth.hospital.repository.AuditLogTargetRepository;
import com.usth.hospital.security.CurrentUserProvider;
import com.usth.hospital.security.CurrentUserSnapshot;
import com.usth.hospital.service.impl.AuditLogServiceImpl;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditLogChangeRepository auditLogChangeRepository;

    @Mock
    private AuditLogTargetRepository auditLogTargetRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private AuditLogServiceImpl service;

    @Test
    void record_requiresTheCallingMutationTransaction() throws NoSuchMethodException {
        Transactional transaction = AuditLogServiceImpl.class.getMethod(
                "record",
                AuditEventCommand.class
        ).getAnnotation(Transactional.class);

        assertThat(transaction).isNotNull();
        assertThat(transaction.propagation()).isEqualTo(Propagation.MANDATORY);
    }

    @Test
    void record_persistsStructuredEventAndRedactsClassifiedValues() {
        when(currentUserProvider.snapshot()).thenReturn(
                new CurrentUserSnapshot("admin", "Admin Test", "ADMIN"));
        when(auditLogRepository.save(any(AuditLogEntity.class)))
                .thenReturn(AuditLogEntity.builder().id(42L).build());
        String longSummary = "x".repeat(1100);
        AuditEventCommand command = AuditEventCommand.builder()
                .action(AuditLogAction.UPDATE)
                .entityType(AuditLogEntityType.PATIENT)
                .entityId("P26MB001")
                .targetLabel("Patient Test · P26MB001")
                .summary(longSummary)
                .changes(List.of(
                        AuditLogChange.generalReference(
                                "status", AuditLogValueType.REFERENCE,
                                0, 1, "Waiting", "Examining", 0),
                        AuditLogChange.personal(
                                 "phoneNo", AuditLogValueType.TEXT,
                                 "0900000000", "0911111111", 1),
                        AuditLogChange.medical(
                                "bloodType", AuditLogValueType.REFERENCE,
                                "1", "7", 2),
                        AuditLogChange.secret("password", 3)
                ))
                .affectedTargets(List.of(new AuditLogTarget(
                        AuditLogEntityType.PATIENT,
                        "P26MB001",
                        "Patient Test · P26MB001",
                        0
                )))
                .build();

        service.record(command);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntity saved = captor.getValue();
        assertThat(saved.getActor()).isEqualTo("admin");
        assertThat(saved.getActorDisplayName()).isEqualTo("Admin Test");
        assertThat(saved.getActorRole()).isEqualTo("ADMIN");
        assertThat(saved.getAction()).isEqualTo(AuditLogAction.UPDATE);
        assertThat(saved.getEntityType()).isEqualTo(AuditLogEntityType.PATIENT);
        assertThat(saved.getEntityId()).isEqualTo("P26MB001");
        assertThat(saved.getTargetLabel()).isEqualTo("Patient Test · P26MB001");
        assertThat(saved.getDetails()).hasSize(1000);
        assertThat(saved.getSummary()).hasSize(1000);
        assertThat(saved.getEventVersion()).isEqualTo(2);
        assertThat(saved.isSampleData()).isFalse();
        assertThat(saved.getChangeCount()).isEqualTo(3);
        assertThat(saved.getAffectedTargetCount()).isEqualTo(1);
        assertThat(saved.getOccurredAt()).isNotNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<AuditLogChangeEntity>> changeCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(auditLogChangeRepository).saveAll(changeCaptor.capture());
        List<AuditLogChangeEntity> storedChanges = new java.util.ArrayList<>();
        changeCaptor.getValue().forEach(storedChanges::add);
        assertThat(storedChanges).extracting(AuditLogChangeEntity::getFieldKey)
                .containsExactly("status", "phoneNo", "bloodType");
        AuditLogChangeEntity phoneChange = storedChanges.get(1);
        assertThat(phoneChange.getOldValue()).isEqualTo("[REDACTED]");
        assertThat(phoneChange.getNewValue()).isEqualTo("[REDACTED]");
        assertThat(phoneChange.getSensitivity()).isEqualTo(AuditLogSensitivity.PERSONAL);
        AuditLogChangeEntity bloodTypeChange = storedChanges.get(2);
        assertThat(bloodTypeChange.getOldValue()).isEqualTo("[REDACTED]");
        assertThat(bloodTypeChange.getNewValue()).isEqualTo("[REDACTED]");
        assertThat(bloodTypeChange.getOldDisplayValue()).isEqualTo("[REDACTED]");
        assertThat(bloodTypeChange.getNewDisplayValue()).isEqualTo("[REDACTED]");
        assertThat(bloodTypeChange.getSensitivity()).isEqualTo(AuditLogSensitivity.MEDICAL);

        verify(auditLogTargetRepository).saveAll(any());
    }

    @Test
    void list_mapsEntityAndPassesNormalizedFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDateTime occurredAt = LocalDateTime.of(2026, 8, 14, 15, 0);
        AuditLogEntity entity = AuditLogEntity.builder()
                .id(7L)
                .occurredAt(occurredAt)
                .actor("admin")
                .action(AuditLogAction.UPDATE)
                .entityType(AuditLogEntityType.ADMISSION)
                .entityId("admission-1")
                .targetLabel("P26MB001")
                .details(null)
                .build();
        when(auditLogRepository.search(
                "P26MB001",
                "Admin Test",
                AuditLogAction.UPDATE,
                AuditLogEntityType.ADMISSION,
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 15, 0, 0),
                pageable
        ))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        Page<AuditLogResponse> result = service.list(
                "  P26MB001  ",
                "  Admin Test  ",
                AuditLogAction.UPDATE,
                AuditLogEntityType.ADMISSION,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 14),
                pageable
        );

        assertThat(result.getContent()).containsExactly(new AuditLogResponse(
                7L,
                occurredAt,
                "admin",
                "UPDATE",
                "ADMISSION",
                "admission-1",
                "P26MB001",
                null
        ));
        verify(auditLogRepository).search(
                "P26MB001",
                "Admin Test",
                AuditLogAction.UPDATE,
                AuditLogEntityType.ADMISSION,
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 15, 0, 0),
                pageable
        );
    }

    @Test
    void list_supportsBlankSearchAndOneSidedDateRanges() {
        Pageable pageable = PageRequest.of(0, 20);
        when(auditLogRepository.search(null, null, null, null, null,
                LocalDateTime.of(2026, 8, 15, 0, 0), pageable))
                .thenReturn(Page.empty(pageable));

        Page<AuditLogResponse> result = service.list(
                "   ",
                "   ",
                null,
                null,
                null,
                LocalDate.of(2026, 8, 14),
                pageable
        );

        assertThat(result).isEmpty();
        verify(auditLogRepository).search(
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 8, 15, 0, 0),
                pageable
        );
    }

    @Test
    void get_returnsStructuredChangesAndAffectedTargets() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 8, 23, 10, 0);
        AuditLogEntity event = AuditLogEntity.builder()
                .id(42L)
                .occurredAt(occurredAt)
                .actor("admin")
                .actorDisplayName("Admin Test")
                .actorRole("ADMIN")
                .action(AuditLogAction.BULK_DELETE)
                .entityType(AuditLogEntityType.PATIENT)
                .targetLabel("2 patients")
                .details("Deleted 2 patients")
                .eventVersion(2)
                .sampleData(true)
                .summary("Deleted 2 patients")
                .changeCount(1)
                .affectedTargetCount(1)
                .build();
        AuditLogChangeEntity change = AuditLogChangeEntity.builder()
                .id(3L)
                .auditLogId(42L)
                .fieldKey("status")
                .valueType(AuditLogValueType.REFERENCE)
                .oldValue("0")
                .newValue("1")
                .oldDisplayValue("Waiting")
                .newDisplayValue("Examining")
                .sensitivity(AuditLogSensitivity.GENERAL)
                .displayOrder(0)
                .build();
        AuditLogTargetEntity target = AuditLogTargetEntity.builder()
                .id(4L)
                .auditLogId(42L)
                .entityType(AuditLogEntityType.PATIENT)
                .entityId("P26MB001")
                .targetLabel("Patient Test · P26MB001")
                .displayOrder(0)
                .build();
        when(auditLogRepository.findById(42L)).thenReturn(java.util.Optional.of(event));
        when(auditLogChangeRepository.findByAuditLogIdOrderByDisplayOrderAscIdAsc(42L))
                .thenReturn(List.of(change));
        when(auditLogTargetRepository.findByAuditLogIdOrderByDisplayOrderAscIdAsc(42L))
                .thenReturn(List.of(target));

        var result = service.get(42L);

        assertThat(result.event().legacy()).isFalse();
        assertThat(result.event().sampleData()).isTrue();
        assertThat(result.event().actorDisplayName()).isEqualTo("Admin Test");
        assertThat(result.changes()).singleElement().satisfies(item -> {
            assertThat(item.fieldKey()).isEqualTo("status");
            assertThat(item.oldDisplayValue()).isEqualTo("Waiting");
            assertThat(item.newDisplayValue()).isEqualTo("Examining");
        });
        assertThat(result.affectedTargets()).singleElement().satisfies(item ->
                assertThat(item.targetLabel()).isEqualTo("Patient Test · P26MB001"));
    }

    @Test
    void get_redactsUnsafeStoredClassifiedValuesAndOmitsSecretsWithoutChangingHeaderCount() {
        AuditLogEntity event = AuditLogEntity.builder()
                .id(43L)
                .occurredAt(LocalDateTime.of(2026, 8, 23, 11, 0))
                .actor("admin")
                .action(AuditLogAction.UPDATE)
                .entityType(AuditLogEntityType.PATIENT)
                .entityId("P26MB001")
                .targetLabel("Patient Test · P26MB001")
                .eventVersion(2)
                .summary("4 fields changed")
                .changeCount(4)
                .affectedTargetCount(0)
                .build();
        AuditLogChangeEntity general = AuditLogChangeEntity.builder()
                .id(10L)
                .auditLogId(43L)
                .fieldKey("status")
                .valueType(AuditLogValueType.REFERENCE)
                .oldValue("0")
                .newValue("1")
                .oldDisplayValue("Waiting")
                .newDisplayValue("Examining")
                .sensitivity(AuditLogSensitivity.GENERAL)
                .displayOrder(0)
                .build();
        AuditLogChangeEntity personal = AuditLogChangeEntity.builder()
                .id(11L)
                .auditLogId(43L)
                .fieldKey("birthDate")
                .valueType(AuditLogValueType.DATE)
                .oldValue("1990-01-02")
                .newValue("1990-01-03")
                .oldDisplayValue("2 Jan 1990")
                .newDisplayValue("3 Jan 1990")
                .sensitivity(AuditLogSensitivity.PERSONAL)
                .displayOrder(1)
                .build();
        AuditLogChangeEntity medical = AuditLogChangeEntity.builder()
                .id(12L)
                .auditLogId(43L)
                .fieldKey("bloodType")
                .valueType(AuditLogValueType.REFERENCE)
                .oldValue("1")
                .newValue("7")
                .oldDisplayValue("A+")
                .newDisplayValue("O+")
                .sensitivity(AuditLogSensitivity.MEDICAL)
                .displayOrder(2)
                .build();
        AuditLogChangeEntity secret = AuditLogChangeEntity.builder()
                .id(13L)
                .auditLogId(43L)
                .fieldKey("password")
                .valueType(AuditLogValueType.TEXT)
                .oldValue("unsafe-old-secret")
                .newValue("unsafe-new-secret")
                .oldDisplayValue("unsafe-old-secret")
                .newDisplayValue("unsafe-new-secret")
                .sensitivity(AuditLogSensitivity.SECRET)
                .displayOrder(3)
                .build();
        when(auditLogRepository.findById(43L)).thenReturn(java.util.Optional.of(event));
        when(auditLogChangeRepository.findByAuditLogIdOrderByDisplayOrderAscIdAsc(43L))
                .thenReturn(List.of(general, personal, medical, secret));
        when(auditLogTargetRepository.findByAuditLogIdOrderByDisplayOrderAscIdAsc(43L))
                .thenReturn(List.of());

        var result = service.get(43L);

        assertThat(result.event().changeCount()).isEqualTo(4);
        assertThat(result.changes()).extracting(item -> item.fieldKey())
                .containsExactly("status", "birthDate", "bloodType");
        assertThat(result.changes().getFirst()).satisfies(item -> {
            assertThat(item.oldValue()).isEqualTo("0");
            assertThat(item.newDisplayValue()).isEqualTo("Examining");
        });
        assertThat(result.changes().subList(1, 3)).allSatisfy(item -> {
            assertThat(item.oldValue()).isEqualTo("[REDACTED]");
            assertThat(item.newValue()).isEqualTo("[REDACTED]");
            assertThat(item.oldDisplayValue()).isEqualTo("[REDACTED]");
            assertThat(item.newDisplayValue()).isEqualTo("[REDACTED]");
        });
    }

    @Test
    void get_unknownEvent_throwsNotFoundWithoutLoadingChildren() {
        when(auditLogRepository.findById(404L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.get(404L))
                .isInstanceOf(AuditLogNotFoundException.class)
                .hasMessageContaining("404");

        verify(auditLogChangeRepository, never())
                .findByAuditLogIdOrderByDisplayOrderAscIdAsc(any());
        verify(auditLogTargetRepository, never())
                .findByAuditLogIdOrderByDisplayOrderAscIdAsc(any());
    }

    @Test
    void list_supportsFromWithoutTo() {
        Pageable pageable = PageRequest.of(0, 20);
        when(auditLogRepository.search(null, null, null, null,
                LocalDateTime.of(2026, 8, 1, 0, 0), null, pageable))
                .thenReturn(Page.empty(pageable));

        Page<AuditLogResponse> result = service.list(
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 8, 1),
                null,
                pageable
        );

        assertThat(result).isEmpty();
        verify(auditLogRepository).search(
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 8, 1, 0, 0),
                null,
                pageable
        );
    }

    @Test
    void list_fromAfterTo_rejectsFilterBeforeQuerying() {
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> service.list(
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 8, 14),
                pageable
        ))
                .isInstanceOf(InvalidAuditLogFilterException.class)
                .hasMessage("must be on or after from");

        verify(auditLogRepository, never()).search(any(), any(), any(), any(), any(), any(), any());
    }
}
