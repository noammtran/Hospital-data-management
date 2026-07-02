package com.usth.hospital.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.usth.hospital.dto.response.VisitResponse;
import com.usth.hospital.exception.PatientNotFoundException;
import com.usth.hospital.service.MedicalRecordService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MedicalRecordController.class)
class MedicalRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MedicalRecordService medicalRecordService;

    @Test
    void medicalRecord_returnsVisits() throws Exception {
        VisitResponse visit = new VisitResponse("a1", "2026-06-07", null, "Cardiology", "Dr A",
                null, "Routine checkup", 3, 1, null, List.of(), List.of());
        when(medicalRecordService.getMedicalRecord("patient-1")).thenReturn(List.of(visit));

        mockMvc.perform(get("/api/v1/patients/{id}/medical-record", "patient-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].admissionId").value("a1"))
                .andExpect(jsonPath("$.data[0].departmentName").value("Cardiology"));
    }

    @Test
    void medicalRecord_unknownPatient_returns404() throws Exception {
        when(medicalRecordService.getMedicalRecord("unknown"))
                .thenThrow(new PatientNotFoundException("unknown"));

        mockMvc.perform(get("/api/v1/patients/{id}/medical-record", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));
    }
}
