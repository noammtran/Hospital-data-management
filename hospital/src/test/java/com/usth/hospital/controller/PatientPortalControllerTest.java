package com.usth.hospital.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.usth.hospital.config.SecurityConfig;
import com.usth.hospital.dto.response.PatientSelfProfileResponse;
import com.usth.hospital.dto.response.PatientSelfRecordResponse;
import com.usth.hospital.repository.AppUserRepository;
import com.usth.hospital.service.PatientPortalService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PatientPortalController.class)
@Import(SecurityConfig.class)
class PatientPortalControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private PatientPortalService patientPortalService;
    @MockitoBean private AppUserRepository appUserRepository;

    @Test
    @WithMockUser(authorities = {"ROLE_PATIENT", "VIEW_OWN_RECORD"})
    void ownRecord_patientPermission_returnsOnlyResolvedSelfRecord() throws Exception {
        var profile = new PatientSelfProfileResponse(
                "P26MB001", "Nguyễn An", null, 1, null, null, null,
                "079123456789", null, null, null, null, null, null, null, null, 0, null);
        when(patientPortalService.getOwnRecord())
                .thenReturn(new PatientSelfRecordResponse(profile, List.of()));

        mockMvc.perform(get("/api/v1/patient-portal/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patient.id").value("P26MB001"))
                .andExpect(jsonPath("$.data.patient.identityNo").value("079123456789"))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.admissions").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "VIEW_PATIENT")
    void ownRecord_staffPatientListPermission_isInsufficient() throws Exception {
        mockMvc.perform(get("/api/v1/patient-portal/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_PATIENT", "CHANGE_OWN_PASSWORD"})
    void ownRecord_temporaryPasswordPermission_isInsufficient() throws Exception {
        mockMvc.perform(get("/api/v1/patient-portal/me"))
                .andExpect(status().isForbidden());
    }
}
