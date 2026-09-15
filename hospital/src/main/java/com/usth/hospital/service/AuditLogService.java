package com.usth.hospital.service;

import com.usth.hospital.dto.response.AuditLogResponse;
import com.usth.hospital.dto.response.AuditLogDetailResponse;
import com.usth.hospital.model.AuditLogAction;
import com.usth.hospital.model.AuditLogEntityType;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    Page<AuditLogResponse> list(
            String search,
            String actor,
            AuditLogAction action,
            AuditLogEntityType entityType,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    AuditLogDetailResponse get(Long id);

    void record(AuditEventCommand command);
}
