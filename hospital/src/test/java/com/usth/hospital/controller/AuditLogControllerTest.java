package com.usth.hospital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.usth.hospital.config.SecurityConfig;
import com.usth.hospital.dto.response.AuditLogChangeResponse;
import com.usth.hospital.dto.response.AuditLogDetailResponse;
import com.usth.hospital.dto.response.AuditLogResponse;
import com.usth.hospital.dto.response.AuditLogTargetResponse;
import com.usth.hospital.exception.AuditLogNotFoundException;
import com.usth.hospital.exception.InvalidAuditLogFilterException;
import com.usth.hospital.model.AuditLogAction;
import com.usth.hospital.model.AuditLogEntityType;
import com.usth.hospital.repository.AppUserRepository;
import com.usth.hospital.service.AuditLogService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditLogController.class)
@Import(SecurityConfig.class)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @Test
    @WithMockUser(authorities = "VIEW_AUDIT_LOG")
    void list_returnsOneBasedPagedEnvelopeInNewestFirstOrder() throws Exception {
        AuditLogResponse event = new AuditLogResponse(
                7L,
                LocalDateTime.of(2026, 8, 14, 15, 0),
                "admin",
                "UPDATE",
                "PATIENT",
                "P26MB001",
                "P26MB001",
                null
        );
        when(auditLogService.list(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(event)));

        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("page", "2")
                        .param("size", "10")
                        .param("search", " P26MB001 ")
                        .param("actor", " Admin Test ")
                        .param("action", "UPDATE")
                        .param("entityType", "PATIENT")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(7))
                .andExpect(jsonPath("$.data.items[0].occurredAt").exists())
                .andExpect(jsonPath("$.data.items[0].actor").value("admin"))
                .andExpect(jsonPath("$.data.items[0].action").value("UPDATE"))
                .andExpect(jsonPath("$.data.items[0].entityType").value("PATIENT"))
                .andExpect(jsonPath("$.data.items[0].entityId").value("P26MB001"))
                .andExpect(jsonPath("$.data.items[0].targetLabel").value("P26MB001"))
                .andExpect(jsonPath("$.data.items[0].eventVersion").value(1))
                .andExpect(jsonPath("$.data.items[0].legacy").value(true))
                .andExpect(jsonPath("$.data.items[0].sampleData").value(false));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogService).list(
                org.mockito.ArgumentMatchers.eq(" P26MB001 "),
                org.mockito.ArgumentMatchers.eq(" Admin Test "),
                org.mockito.ArgumentMatchers.eq(AuditLogAction.UPDATE),
                org.mockito.ArgumentMatchers.eq(AuditLogEntityType.PATIENT),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 8, 1)),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 8, 14)),
                captor.capture()
        );
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("occurredAt").isDescending()).isTrue();
        assertThat(pageable.getSort().getOrderFor("id").isDescending()).isTrue();
        verify(auditLogService, never()).record(any());
    }

    @Test
    @WithMockUser(authorities = "VIEW_PATIENT")
    void list_withoutAuditPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403));

        verify(auditLogService, never()).list(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "VIEW_AUDIT_LOG")
    void list_pageBelowOne_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        verify(auditLogService, never()).list(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "VIEW_AUDIT_LOG")
    void list_searchLongerThanTwoHundredCharacters_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs").param("search", "x".repeat(201)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        verify(auditLogService, never()).list(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "VIEW_AUDIT_LOG")
    void list_actorLongerThanOneHundredCharacters_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs").param("actor", "x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        verify(auditLogService, never()).list(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "VIEW_AUDIT_LOG")
    void list_fromAfterTo_returnsFieldValidationError() throws Exception {
        when(auditLogService.list(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new InvalidAuditLogFilterException("to", "must be on or after from"));

        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("from", "2026-08-15")
                        .param("to", "2026-08-14"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Invalid data"))
                .andExpect(jsonPath("$.errors.to").value("must be on or after from"));
    }

    @Test
    @WithMockUser(authorities = "VIEW_AUDIT_LOG")
    void get_returnsStructuredEventDetail() throws Exception {
        AuditLogResponse event = new AuditLogResponse(
                42L,
                LocalDateTime.of(2026, 8, 23, 10, 0),
                "admin",
                "UPDATE",
                "PATIENT",
                "P26MB001",
                "Patient Test · P26MB001",
                "Updated patient Patient Test · P26MB001",
                2,
                false,
                "Admin Test",
                "ADMIN",
                null,
                null,
                null,
                "Updated patient Patient Test · P26MB001",
                1,
                0,
                true
        );
        when(auditLogService.get(42L)).thenReturn(new AuditLogDetailResponse(
                event,
                List.of(new AuditLogChangeResponse(
                        "status", "REFERENCE", "0", "1", "Waiting", "Examining", "GENERAL", 0)),
                List.of(new AuditLogTargetResponse(
                        "PATIENT", "P26MB001", "Patient Test · P26MB001", 0))
        ));

        mockMvc.perform(get("/api/v1/audit-logs/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.event.id").value(42))
                .andExpect(jsonPath("$.data.event.actorDisplayName").value("Admin Test"))
                .andExpect(jsonPath("$.data.event.sampleData").value(true))
                .andExpect(jsonPath("$.data.changes[0].fieldKey").value("status"))
                .andExpect(jsonPath("$.data.changes[0].oldDisplayValue").value("Waiting"))
                .andExpect(jsonPath("$.data.affectedTargets[0].entityId").value("P26MB001"));
    }

    @Test
    @WithMockUser(authorities = "VIEW_AUDIT_LOG")
    void get_unknownEvent_returns404Envelope() throws Exception {
        when(auditLogService.get(404L)).thenThrow(new AuditLogNotFoundException(404L));

        mockMvc.perform(get("/api/v1/audit-logs/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Audit event not found: 404"));
    }
}
