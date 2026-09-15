package com.usth.hospital.controller;

import com.usth.hospital.dto.response.ApiResponse;
import com.usth.hospital.dto.response.AuditLogDetailResponse;
import com.usth.hospital.dto.response.AuditLogResponse;
import com.usth.hospital.dto.response.PageResponse;
import com.usth.hospital.model.AuditLogAction;
import com.usth.hospital.model.AuditLogEntityType;
import com.usth.hospital.service.AuditLogService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VIEW_AUDIT_LOG')")
@Validated
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ApiResponse<PageResponse<AuditLogResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 200) String search,
            @RequestParam(required = false) @Size(max = 100) String actor,
            @RequestParam(required = false) AuditLogAction action,
            @RequestParam(required = false) AuditLogEntityType entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Sort sort = Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"));
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return ApiResponse.ok(PageResponse.from(
                auditLogService.list(search, actor, action, entityType, from, to, pageable)
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<AuditLogDetailResponse> get(@PathVariable @Min(1) Long id) {
        return ApiResponse.ok(auditLogService.get(id));
    }
}
