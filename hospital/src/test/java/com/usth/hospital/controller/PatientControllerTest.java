package com.usth.hospital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usth.hospital.dto.request.CreatePatientRequest;
import com.usth.hospital.dto.response.PatientDetailResponse;
import com.usth.hospital.dto.response.PatientListResponse;
import com.usth.hospital.exception.DuplicateFieldException;
import com.usth.hospital.exception.PatientNotFoundException;
import com.usth.hospital.service.PatientService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PatientController.class)
class PatientControllerTest {

    private static final String PATIENT_ID = "patient-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PatientService patientService;

    private PatientDetailResponse detail() {
        return new PatientDetailResponse(PATIENT_ID, "Nguyen Van A", LocalDate.of(1990, 1, 1),
                1, "Hanoi", "0900000000", "a@b.com", "012345678", 7, null, null, null, null,
                "Tran Thi B", "0911111111", "mother", 0, null,
                "SYSTEM", LocalDate.now(), "SYSTEM", LocalDate.now());
    }

    private CreatePatientRequest validCreate() {
        return new CreatePatientRequest("Nguyen Van A", LocalDate.of(1990, 1, 1), 1, "Hanoi",
                "0900000000", "a@b.com", "012345678", 7, null, null, null, null,
                "Tran Thi B", "0911111111", "mother", 0, null);
    }

    private PatientListResponse listItem() {
        return new PatientListResponse(PATIENT_ID, "Nguyen Van A", 1,
                LocalDate.of(1990, 1, 1), "0900000000", 0, true);
    }

    @Test
    void list_returnsPagedEnvelope() throws Exception {
        Page<PatientListResponse> page = new PageImpl<>(List.of(listItem()), PageRequest.of(0, 20), 1);
        when(patientService.list(any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.items[0].id").value(PATIENT_ID))
                .andExpect(jsonPath("$.data.items[0].hasHistory").value(true))
                .andExpect(jsonPath("$.data.pagination.page").value(1))
                .andExpect(jsonPath("$.data.pagination.totalItems").value(1));
    }

    @Test
    void list_convertsOneIndexedPageToZeroIndexed() throws Exception {
        when(patientService.list(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

        mockMvc.perform(get("/api/v1/patients").param("page", "2").param("size", "10"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(patientService).list(any(), any(), any(), any(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void list_passesSearchAndFilterParamsToService() throws Exception {
        when(patientService.list(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/patients")
                        .param("search", "0900")
                        .param("searchField", "phone")
                        .param("status", "1")
                        .param("gender", "2"))
                .andExpect(status().isOk());

        verify(patientService).list(eq("0900"), eq("phone"), eq(1), eq(2), any());
    }

    @Test
    void detail_returnsPatient() throws Exception {
        when(patientService.getDetail(PATIENT_ID)).thenReturn(detail());

        mockMvc.perform(get("/api/v1/patients/{id}", PATIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Nguyen Van A"));
    }

    @Test
    void detail_unknownId_returns404() throws Exception {
        when(patientService.getDetail(PATIENT_ID))
                .thenThrow(new PatientNotFoundException(PATIENT_ID));

        mockMvc.perform(get("/api/v1/patients/{id}", PATIENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(patientService.create(any())).thenReturn(detail());

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreate())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.id").value(PATIENT_ID));
    }

    @Test
    void create_blankName_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"status\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void create_duplicateIdentity_returns400WithFieldError() throws Exception {
        when(patientService.create(any()))
                .thenThrow(new DuplicateFieldException("identityNo", "Identity card number already exists"));

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreate())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors.identityNo").value("Identity card number already exists"));
    }

    @Test
    void create_missingStatus_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nguyen Van A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").exists());
    }

    @Test
    void create_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is invalid or missing"));
    }

    @Test
    void update_validRequest_returns200() throws Exception {
        when(patientService.update(eq(PATIENT_ID), any())).thenReturn(detail());

        mockMvc.perform(put("/api/v1/patients/{id}", PATIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(PATIENT_ID));
    }

    @Test
    void delete_returnsNoContentEnvelope() throws Exception {
        mockMvc.perform(delete("/api/v1/patients/{id}", PATIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(204));

        verify(patientService).delete(PATIENT_ID);
    }

    @Test
    void bulkDelete_emptyIds_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/patients/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.ids").exists());
    }

    @Test
    void bulkDelete_validIds_callsService() throws Exception {
        mockMvc.perform(post("/api/v1/patients/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"patient-1\",\"patient-2\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(204));

        verify(patientService).bulkDelete(List.of("patient-1", "patient-2"));
    }

    @Test
    void history_returnsPagedEnvelope() throws Exception {
        when(patientService.getHistory(eq(PATIENT_ID), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/patients/{id}/history", PATIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.page").value(1));
    }

    @Test
    void allHistory_literalRouteResolvesToGlobalHistory() throws Exception {
        when(patientService.getAllHistory(any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/patients/history"))
                .andExpect(status().isOk());

        verify(patientService).getAllHistory(any());
    }
}
