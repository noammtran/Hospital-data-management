package com.usth.hospital.dto.response;

import java.util.List;

public record AuditLogDetailResponse(
        AuditLogResponse event,
        List<AuditLogChangeResponse> changes,
        List<AuditLogTargetResponse> affectedTargets
) {
}
