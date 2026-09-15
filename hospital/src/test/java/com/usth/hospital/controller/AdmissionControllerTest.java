package com.usth.hospital.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usth.hospital.config.SecurityConfig;
import com.usth.hospital.dto.request.CreateAdmissionRequest;
import com.usth.hospital.dto.request.UpdateAdmissionRequest;
import com.usth.hospital.dto.response.AdmissionEditDataResponse;
import com.usth.hospital.dto.response.AdmissionOptionsResponse;
import com.usth.hospital.dto.response.AdmissionResponse;
import com.usth.hospital.repository.AppUserRepository;
import com.usth.hospital.service.AdmissionService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdmissionController.class)
@Import(SecurityConfig.class)
class AdmissionControllerTest {

    private static final String PATIENT_ID = "P26MB001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdmissionService admissionService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @Test
    @WithMockUser(authorities = "ADMISSION_CREATE")
    void options_createPermission_returnsReferenceOptions() throws Exception {
        AdmissionOptionsResponse options = new AdmissionOptionsResponse(
                List.of(new AdmissionOptionsResponse.DepartmentOption(1L, "GEN", "General")),
                List.of(),
                List.of(),
                List.of(new AdmissionOptionsResponse.LookupOption(1, "Emergency"))
        );
        when(admissionService.getOptions()).thenReturn(options);

        mockMvc.perform(get("/api/v1/admissions/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.departments[0].id").value(1))
                .andExpect(jsonPath("$.data.types[0].value").value(1));
    }

    @Test
    @WithMockUser(authorities = "ADMISSION_CREATE")
    void create_validRequest_returns201() throws Exception {
        CreateAdmissionRequest request = request(LocalDate.now());
        AdmissionResponse response = new AdmissionResponse(
                "admission-1", PATIENT_ID, 1L, 2L, null,
                request.admissionDate(), null, request.reason(), request.type(), 1, request.notes());
        when(admissionService.create(PATIENT_ID, request)).thenReturn(response);

        mockMvc.perform(post("/api/v1/patients/{patientId}/admissions", PATIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.admissionId").value("admission-1"))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    @Test
    @WithMockUser(authorities = "ADMISSION_CREATE")
    void create_futureDate_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/patients/{patientId}/admissions", PATIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(LocalDate.now().plusDays(1)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.admissionDate")
                        .value("Admission date cannot be in the future"));

        verify(admissionService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(authorities = "VIEW_MEDICAL_RECORD")
    void create_withoutCreatePermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/patients/{patientId}/admissions", PATIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(LocalDate.now()))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMISSION_UPDATE")
    void editData_updatePermission_returnsAdmissionAndOptions() throws Exception {
        AdmissionResponse admission = response("admission-1", LocalDate.now());
        AdmissionOptionsResponse options = new AdmissionOptionsResponse(
                List.of(new AdmissionOptionsResponse.DepartmentOption(1L, "GEN", "General")),
                List.of(),
                List.of(),
                List.of()
        );
        when(admissionService.getEditData(PATIENT_ID, "admission-1"))
                .thenReturn(new AdmissionEditDataResponse(admission, options));

        mockMvc.perform(get(
                        "/api/v1/patients/{patientId}/admissions/{admissionId}/edit-data",
                        PATIENT_ID,
                        "admission-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.admission.admissionId").value("admission-1"))
                .andExpect(jsonPath("$.data.options.departments[0].id").value(1));
    }

    @Test
    @WithMockUser(authorities = "VIEW_MEDICAL_RECORD")
    void editData_withoutUpdatePermission_returns403() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/patients/{patientId}/admissions/{admissionId}/edit-data",
                        PATIENT_ID,
                        "admission-1"))
                .andExpect(status().isForbidden());

        verify(admissionService, never()).getEditData(any(), any());
    }

    @Test
    @WithMockUser(authorities = "ADMISSION_UPDATE")
    void update_validRequest_returns200() throws Exception {
        UpdateAdmissionRequest request = updateRequest(LocalDate.now());
        when(admissionService.update(PATIENT_ID, "admission-1", request))
                .thenReturn(response("admission-1", request.admissionDate()));

        mockMvc.perform(put(
                        "/api/v1/patients/{patientId}/admissions/{admissionId}",
                        PATIENT_ID,
                        "admission-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.admissionId").value("admission-1"));
    }

    @Test
    @WithMockUser(authorities = "ADMISSION_UPDATE")
    void update_futureDate_returnsValidationError() throws Exception {
        mockMvc.perform(put(
                        "/api/v1/patients/{patientId}/admissions/{admissionId}",
                        PATIENT_ID,
                        "admission-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                updateRequest(LocalDate.now().plusDays(1)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.admissionDate")
                        .value("Admission date cannot be in the future"));

        verify(admissionService, never()).update(any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "ADMISSION_CREATE")
    void update_withoutUpdatePermission_returns403() throws Exception {
        mockMvc.perform(put(
                        "/api/v1/patients/{patientId}/admissions/{admissionId}",
                        PATIENT_ID,
                        "admission-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest(LocalDate.now()))))
                .andExpect(status().isForbidden());

        verify(admissionService, never()).update(any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "ADMISSION_DISCHARGE")
    void discharge_withPermission_completesAdmission() throws Exception {
        AdmissionResponse discharged = new AdmissionResponse(
                "admission-1", PATIENT_ID, 1L, 2L, null,
                LocalDate.now().minusDays(1), LocalDate.now(),
                "Clinical observation", 2, 2, "Stable");
        when(admissionService.discharge(PATIENT_ID, "admission-1"))
                .thenReturn(discharged);

        mockMvc.perform(post(
                        "/api/v1/patients/{patientId}/admissions/{admissionId}/discharge",
                        PATIENT_ID,
                        "admission-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(2))
                .andExpect(jsonPath("$.data.dischargeDate").value(LocalDate.now().toString()));

        verify(admissionService).discharge(PATIENT_ID, "admission-1");
    }

    @Test
    @WithMockUser(authorities = "ADMISSION_UPDATE")
    void discharge_withoutPermission_returns403() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/patients/{patientId}/admissions/{admissionId}/discharge",
                        PATIENT_ID,
                        "admission-1"))
                .andExpect(status().isForbidden());

        verify(admissionService, never()).discharge(any(), any());
    }

    @Test
    @WithMockUser(authorities = "ADMISSION_DELETE")
    void delete_adminPermission_deletesAdmission() throws Exception {
        mockMvc.perform(delete(
                        "/api/v1/patients/{patientId}/admissions/{admissionId}",
                        PATIENT_ID,
                        "admission-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(204));

        verify(admissionService).delete(PATIENT_ID, "admission-1");
    }

    @Test
    @WithMockUser(authorities = "ADMISSION_CREATE")
    void delete_doctorCreatePermission_returns403() throws Exception {
        mockMvc.perform(delete(
                        "/api/v1/patients/{patientId}/admissions/{admissionId}",
                        PATIENT_ID,
                        "admission-1"))
                .andExpect(status().isForbidden());

        verify(admissionService, never()).delete(any(), any());
    }

    private static CreateAdmissionRequest request(LocalDate admissionDate) {
        return new CreateAdmissionRequest(
                1L,
                2L,
                null,
                admissionDate,
                "Clinical observation",
                2,
                "Stable"
        );
    }

    private static UpdateAdmissionRequest updateRequest(LocalDate admissionDate) {
        return new UpdateAdmissionRequest(
                1L,
                2L,
                null,
                admissionDate,
                "Clinical observation",
                2,
                "Stable"
        );
    }

    private static AdmissionResponse response(String admissionId, LocalDate admissionDate) {
        return new AdmissionResponse(
                admissionId,
                PATIENT_ID,
                1L,
                2L,
                null,
                admissionDate,
                null,
                "Clinical observation",
                2,
                1,
                "Stable"
        );
    }
}
