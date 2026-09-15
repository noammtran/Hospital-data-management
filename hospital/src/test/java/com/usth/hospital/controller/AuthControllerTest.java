package com.usth.hospital.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usth.hospital.config.SecurityConfig;
import com.usth.hospital.dto.request.LoginRequest;
import com.usth.hospital.dto.response.AuthTokenResponse;
import com.usth.hospital.dto.response.CurrentUserResponse;
import com.usth.hospital.exception.InvalidCredentialsException;
import com.usth.hospital.repository.AppUserRepository;
import com.usth.hospital.security.UserRole;
import com.usth.hospital.service.AuthenticationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @Test
    void login_validCredentials_returnsTokenEnvelopeWithoutAuthentication() throws Exception {
        when(authenticationService.login(any()))
                .thenReturn(new AuthTokenResponse("signed-jwt", "Bearer", 1800));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("doctor", "Doctor@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("signed-jwt"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800));
    }

    @Test
    void login_invalidCredentials_returns401Envelope() throws Exception {
        when(authenticationService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"doctor\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void currentUser_anonymous_returns401Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401));
    }

    @Test
    @WithMockUser(username = "doctor")
    void currentUser_authenticated_returnsProfile() throws Exception {
        when(authenticationService.currentUser()).thenReturn(new CurrentUserResponse(
                2L,
                "doctor",
                "doctor@hospital.example",
                "Trần Quốc Bảo",
                UserRole.DOCTOR,
                List.of(
                        "ADMISSION_CREATE",
                        "ADMISSION_UPDATE",
                        "ADMISSION_DISCHARGE",
                        "PATIENT_COMPLETE_EXAMINATION",
                        "PATIENT_START_EXAMINATION",
                        "VIEW_MEDICAL_RECORD",
                        "VIEW_PATIENT",
                        "VIEW_STATISTICS"),
                false
        ));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("doctor"))
                .andExpect(jsonPath("$.data.role").value("DOCTOR"))
                .andExpect(jsonPath("$.data.permissions[0]").value("ADMISSION_CREATE"))
                .andExpect(jsonPath("$.data.permissions[1]").value("ADMISSION_UPDATE"));
    }

    @Test
    @WithMockUser(authorities = "CHANGE_OWN_PASSWORD")
    void changePassword_withDedicatedPermission_returnsNoContentEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"079123456789\",\"newPassword\":\"Patient@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(204));
    }

    @Test
    @WithMockUser(authorities = "VIEW_OWN_RECORD")
    void changePassword_withoutDedicatedPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"079123456789\",\"newPassword\":\"Patient@123\"}"))
                .andExpect(status().isForbidden());
    }
}
