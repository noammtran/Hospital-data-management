package com.usth.hospital.service;

import com.usth.hospital.model.AuditLogAction;
import com.usth.hospital.model.AuditLogEntityType;
import java.util.List;
import lombok.Builder;

@Builder
public record AuditEventCommand(
        AuditLogAction action,
        AuditLogEntityType entityType,
        String entityId,
        String targetLabel,
        AuditLogEntityType subjectType,
        String subjectId,
        String subjectLabel,
        String summary,
        List<AuditLogChange> changes,
        List<AuditLogTarget> affectedTargets
) {

    public AuditEventCommand {
        changes = changes == null ? List.of() : List.copyOf(changes);
        affectedTargets = affectedTargets == null ? List.of() : List.copyOf(affectedTargets);
    }
}
