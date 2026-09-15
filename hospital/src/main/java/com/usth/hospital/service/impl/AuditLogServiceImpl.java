package com.usth.hospital.service.impl;

import com.usth.hospital.dto.response.AuditLogChangeResponse;
import com.usth.hospital.dto.response.AuditLogDetailResponse;
import com.usth.hospital.dto.response.AuditLogResponse;
import com.usth.hospital.dto.response.AuditLogTargetResponse;
import com.usth.hospital.exception.AuditLogNotFoundException;
import com.usth.hospital.exception.InvalidAuditLogFilterException;
import com.usth.hospital.model.AuditLogAction;
import com.usth.hospital.model.AuditLogChangeEntity;
import com.usth.hospital.model.AuditLogEntity;
import com.usth.hospital.model.AuditLogEntityType;
import com.usth.hospital.model.AuditLogSensitivity;
import com.usth.hospital.model.AuditLogTargetEntity;
import com.usth.hospital.repository.AuditLogChangeRepository;
import com.usth.hospital.repository.AuditLogRepository;
import com.usth.hospital.repository.AuditLogTargetRepository;
import com.usth.hospital.security.CurrentUserSnapshot;
import com.usth.hospital.service.AuditEventCommand;
import com.usth.hospital.service.AuditLogChange;
import com.usth.hospital.security.CurrentUserProvider;
import com.usth.hospital.service.AuditLogService;
import com.usth.hospital.service.AuditLogTarget;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

    private static final int EVENT_VERSION = 2;
    private static final String REDACTED = "[REDACTED]";
    private static final int ACTOR_MAX_LENGTH = 30;
    private static final int ACTOR_DISPLAY_NAME_MAX_LENGTH = 100;
    private static final int ACTOR_ROLE_MAX_LENGTH = 20;
    private static final int ENTITY_ID_MAX_LENGTH = 100;
    private static final int TARGET_LABEL_MAX_LENGTH = 200;
    private static final int DETAILS_MAX_LENGTH = 1000;
    private static final int FIELD_KEY_MAX_LENGTH = 80;
    private static final int VALUE_MAX_LENGTH = 2000;
    private static final int DISPLAY_VALUE_MAX_LENGTH = 500;

    private final AuditLogRepository auditLogRepository;
    private final AuditLogChangeRepository auditLogChangeRepository;
    private final AuditLogTargetRepository auditLogTargetRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public Page<AuditLogResponse> list(
            String search,
            String actor,
            AuditLogAction action,
            AuditLogEntityType entityType,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidAuditLogFilterException("to", "must be on or after from");
        }
        String keyword = search == null || search.isBlank() ? null : search.trim();
        String actorFilter = actor == null || actor.isBlank() ? null : actor.trim();
        LocalDateTime fromInclusive = from == null ? null : from.atStartOfDay();
        LocalDateTime toExclusive = to == null ? null : to.plusDays(1).atStartOfDay();
        return auditLogRepository.search(
                keyword,
                actorFilter,
                action,
                entityType,
                fromInclusive,
                toExclusive,
                pageable
        ).map(AuditLogServiceImpl::toResponse);
    }

    @Override
    public AuditLogDetailResponse get(Long id) {
        AuditLogEntity event = auditLogRepository.findById(id)
                .orElseThrow(() -> new AuditLogNotFoundException(id));
        List<AuditLogChangeResponse> changes = auditLogChangeRepository
                .findByAuditLogIdOrderByDisplayOrderAscIdAsc(id)
                .stream()
                .filter(change -> change.getSensitivity() != AuditLogSensitivity.SECRET)
                .map(AuditLogServiceImpl::toChangeResponse)
                .toList();
        List<AuditLogTargetResponse> affectedTargets = auditLogTargetRepository
                .findByAuditLogIdOrderByDisplayOrderAscIdAsc(id)
                .stream()
                .map(AuditLogServiceImpl::toTargetResponse)
                .toList();
        return new AuditLogDetailResponse(toResponse(event), changes, affectedTargets);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditEventCommand command) {
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(command.action(), "action is required");
        Objects.requireNonNull(command.entityType(), "entityType is required");

        CurrentUserSnapshot actor = currentUserProvider.snapshot();
        if (actor == null) {
            String username = currentUserProvider.username();
            actor = new CurrentUserSnapshot(username, username, null);
        }
        List<AuditLogChange> changes = command.changes().stream()
                .filter(Objects::nonNull)
                .filter(change -> change.sensitivity() != AuditLogSensitivity.SECRET)
                .map(AuditLogServiceImpl::sanitizeChange)
                .toList();
        List<AuditLogTarget> affectedTargets = command.affectedTargets().stream()
                .filter(Objects::nonNull)
                .map(AuditLogServiceImpl::sanitizeTarget)
                .toList();
        String summary = limit(command.summary(), DETAILS_MAX_LENGTH);

        AuditLogEntity event = AuditLogEntity.builder()
                .occurredAt(LocalDateTime.now())
                .actor(limit(actor.username(), ACTOR_MAX_LENGTH))
                .actorDisplayName(limit(actor.displayName(), ACTOR_DISPLAY_NAME_MAX_LENGTH))
                .actorRole(limit(actor.role(), ACTOR_ROLE_MAX_LENGTH))
                .action(command.action())
                .entityType(command.entityType())
                .entityId(limit(command.entityId(), ENTITY_ID_MAX_LENGTH))
                .targetLabel(limit(command.targetLabel(), TARGET_LABEL_MAX_LENGTH))
                .details(summary)
                .eventVersion(EVENT_VERSION)
                .sampleData(false)
                .subjectType(command.subjectType())
                .subjectId(limit(command.subjectId(), ENTITY_ID_MAX_LENGTH))
                .subjectLabel(limit(command.subjectLabel(), TARGET_LABEL_MAX_LENGTH))
                .summary(summary)
                .changeCount(changes.size())
                .affectedTargetCount(affectedTargets.size())
                .build();
        AuditLogEntity saved = auditLogRepository.save(event);

        if (!changes.isEmpty()) {
            auditLogChangeRepository.saveAll(changes.stream()
                    .map(change -> toEntity(saved.getId(), change))
                    .toList());
        }
        if (!affectedTargets.isEmpty()) {
            auditLogTargetRepository.saveAll(affectedTargets.stream()
                    .map(target -> toEntity(saved.getId(), target))
                    .toList());
        }
    }

    private static AuditLogResponse toResponse(AuditLogEntity event) {
        int eventVersion = event.getEventVersion() == null ? 1 : event.getEventVersion();
        String summary = firstPresent(event.getSummary(), event.getDetails());
        return new AuditLogResponse(
                event.getId(),
                event.getOccurredAt(),
                event.getActor(),
                event.getAction().name(),
                event.getEntityType().name(),
                event.getEntityId(),
                event.getTargetLabel(),
                event.getDetails(),
                eventVersion,
                eventVersion < EVENT_VERSION,
                event.getActorDisplayName(),
                event.getActorRole(),
                event.getSubjectType() == null ? null : event.getSubjectType().name(),
                event.getSubjectId(),
                event.getSubjectLabel(),
                summary,
                event.getChangeCount() == null ? 0 : event.getChangeCount(),
                event.getAffectedTargetCount() == null ? 0 : event.getAffectedTargetCount(),
                event.isSampleData()
        );
    }

    private static AuditLogChangeResponse toChangeResponse(AuditLogChangeEntity change) {
        boolean redact = change.getSensitivity() == AuditLogSensitivity.PERSONAL
                || change.getSensitivity() == AuditLogSensitivity.MEDICAL;
        return new AuditLogChangeResponse(
                change.getFieldKey(),
                change.getValueType().name(),
                protectedValue(change.getOldValue(), VALUE_MAX_LENGTH, redact),
                protectedValue(change.getNewValue(), VALUE_MAX_LENGTH, redact),
                protectedValue(change.getOldDisplayValue(), DISPLAY_VALUE_MAX_LENGTH, redact),
                protectedValue(change.getNewDisplayValue(), DISPLAY_VALUE_MAX_LENGTH, redact),
                change.getSensitivity().name(),
                change.getDisplayOrder()
        );
    }

    private static AuditLogTargetResponse toTargetResponse(AuditLogTargetEntity target) {
        return new AuditLogTargetResponse(
                target.getEntityType().name(),
                target.getEntityId(),
                target.getTargetLabel(),
                target.getDisplayOrder()
        );
    }

    private static AuditLogChangeEntity toEntity(Long auditLogId, AuditLogChange change) {
        return AuditLogChangeEntity.builder()
                .auditLogId(auditLogId)
                .fieldKey(change.fieldKey())
                .valueType(change.valueType())
                .oldValue(change.oldValue())
                .newValue(change.newValue())
                .oldDisplayValue(change.oldDisplayValue())
                .newDisplayValue(change.newDisplayValue())
                .sensitivity(change.sensitivity())
                .displayOrder(change.displayOrder())
                .build();
    }

    private static AuditLogTargetEntity toEntity(Long auditLogId, AuditLogTarget target) {
        return AuditLogTargetEntity.builder()
                .auditLogId(auditLogId)
                .entityType(target.entityType())
                .entityId(target.entityId())
                .targetLabel(target.targetLabel())
                .displayOrder(target.displayOrder())
                .build();
    }

    private static AuditLogChange sanitizeChange(AuditLogChange change) {
        Objects.requireNonNull(change.valueType(), "change valueType is required");
        Objects.requireNonNull(change.sensitivity(), "change sensitivity is required");
        String fieldKey = required(change.fieldKey(), FIELD_KEY_MAX_LENGTH, "change fieldKey is required");
        boolean redact = change.sensitivity() == AuditLogSensitivity.PERSONAL
                || change.sensitivity() == AuditLogSensitivity.MEDICAL;
        return new AuditLogChange(
                fieldKey,
                change.valueType(),
                protectedValue(change.oldValue(), VALUE_MAX_LENGTH, redact),
                protectedValue(change.newValue(), VALUE_MAX_LENGTH, redact),
                protectedValue(change.oldDisplayValue(), DISPLAY_VALUE_MAX_LENGTH, redact),
                protectedValue(change.newDisplayValue(), DISPLAY_VALUE_MAX_LENGTH, redact),
                change.sensitivity(),
                Math.max(0, change.displayOrder())
        );
    }

    private static AuditLogTarget sanitizeTarget(AuditLogTarget target) {
        Objects.requireNonNull(target.entityType(), "target entityType is required");
        return new AuditLogTarget(
                target.entityType(),
                limit(target.entityId(), ENTITY_ID_MAX_LENGTH),
                limit(target.targetLabel(), TARGET_LABEL_MAX_LENGTH),
                Math.max(0, target.displayOrder())
        );
    }

    private static String protectedValue(String value, int maxLength, boolean redact) {
        if (value == null) {
            return null;
        }
        return redact ? REDACTED : limit(value, maxLength);
    }

    private static String firstPresent(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private static String required(String value, int maxLength, String message) {
        String normalized = limit(value, maxLength);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }
}
