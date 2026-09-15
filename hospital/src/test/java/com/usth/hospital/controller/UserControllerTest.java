package com.usth.hospital.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usth.hospital.config.SecurityConfig;
import com.usth.hospital.dto.request.CreateUserRequest;
import com.usth.hospital.dto.response.UserResponse;
import com.usth.hospital.repository.AppUserRepository;
import com.usth.hospital.security.UserRole;
import com.usth.hospital.service.UserManagementService;
import com.usth.hospital.service.impl.StaffWorkProfileService;
import com.usth.hospital.dto.response.StaffWorkProfileResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@WithMockUser(authorities = "MANAGE_USERS")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserManagementService userManagementService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private StaffWorkProfileService staffWorkProfileService;

    @Test
    void workProfile_returnsAccountAndMissingSetupWithoutSecrets() throws Exception {
        when(staffWorkProfileService.get(2L)).thenReturn(new StaffWorkProfileResponse(
                user(), List.of("CARE_VIEW"), null, List.of(), List.of("DOCTOR_LINK_MISSING")));
        mockMvc.perform(get("/api/v1/users/2/work-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.id").value(2))
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.setupIssues[0]").value("DOCTOR_LINK_MISSING"));
    }

    @Test
    @WithMockUser(authorities = {"MANAGE_USERS", "CARE_MANAGE"})
    void createDoctorProfile_returnsCreatedEnvelope() throws Exception {
        when(staffWorkProfileService.createDoctor(eq(2L), any())).thenReturn(
                new StaffWorkProfileResponse.Doctor(1000L, "DOC_TEST", "Nguyễn Minh An", null, 1L, "Nội", true));
        mockMvc.perform(post("/api/v1/users/2/doctor-profile").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"DOC_TEST","name":"Nguyễn Minh An","departmentId":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1000));
    }

    @Test
    @WithMockUser(authorities = {"MANAGE_USERS", "CARE_MANAGE"})
    void createDoctorProfile_validatesCodeNameDepartmentAndEmail() throws Exception {
        mockMvc.perform(post("/api/v1/users/2/doctor-profile").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"bad code","name":" ","departmentId":0,"email":"invalid"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.code").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.departmentId").exists())
                .andExpect(jsonPath("$.errors.email").exists());
        verify(staffWorkProfileService, never()).createDoctor(any(), any());
    }

    @Test
    @WithMockUser(authorities = "CARE_VIEW")
    void staffWithoutManagementCannotReadProfilesOrCreateDoctors() throws Exception {
        mockMvc.perform(get("/api/v1/users/2/work-profile")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/users/2/doctor-profile").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"DOC_TEST","name":"Nguyễn Minh An","departmentId":1}
                                """))
                .andExpect(status().isForbidden());
        verify(staffWorkProfileService, never()).get(any());
        verify(staffWorkProfileService, never()).createDoctor(any(), any());
    }

    @Test
    void doctorCreationAlsoRequiresCareManagement() throws Exception {
        mockMvc.perform(post("/api/v1/users/2/doctor-profile").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"DOC_TEST","name":"Nguyễn Minh An","departmentId":1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_returnsPagedEnvelopeAndPassesFilters() throws Exception {
        PageRequest pageable = PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("username"));
        when(userManagementService.list(eq("doc"), eq(UserRole.DOCTOR), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(user()), pageable, 1));

        mockMvc.perform(get("/api/v1/users")
                        .param("search", "doc")
                        .param("role", "DOCTOR")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].username").value("001201100001"))
                .andExpect(jsonPath("$.data.items[0].identityNo").value("001201100001"))
                .andExpect(jsonPath("$.data.items[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.pagination.totalItems").value(1));
    }

    @Test
    void create_validRequest_returns201WithoutPasswordHash() throws Exception {
        when(userManagementService.create(any())).thenReturn(user());
        CreateUserRequest request = new CreateUserRequest(
                "001201100001", "doctor@hospital.example", "Trần Quốc Bảo",
                UserRole.DOCTOR
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("001201100001"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void create_invalidIdentityNumber_returnsFieldValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identityNo":"123",
                                  "email":"doctor2@hospital.example",
                                  "displayName":"Bác sĩ Demo",
                                  "role":"DOCTOR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.identityNo").exists());
    }

    @Test
    void list_pageBelowOne_returns400Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Request parameter is invalid"));

        verify(userManagementService, never()).list(any(), any(), any(), any());
    }

    @Test
    void setStatus_callsService() throws Exception {
        when(userManagementService.setActive(2L, false)).thenReturn(user());

        mockMvc.perform(patch("/api/v1/users/{id}/status", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());

        verify(userManagementService).setActive(2L, false);
    }

    @Test
    void resetPassword_callsServiceAndReturnsNoContentEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/reset-password", 2L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(204));

        verify(userManagementService).resetPassword(2L);
    }

    @Test
    @WithMockUser(authorities = "VIEW_PATIENT")
    void list_withoutManageUsersPermission_returns403Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403));

        verify(userManagementService, never()).list(any(), any(), any(), any());
    }

    private static UserResponse user() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 31, 10, 0);
        return new UserResponse(
                2L,
                "001201100001",
                "001201100001",
                "doctor@hospital.example",
                "Trần Quốc Bảo",
                UserRole.DOCTOR,
                true,
                false,
                "admin",
                now,
                "admin",
                now
        );
    }
}
