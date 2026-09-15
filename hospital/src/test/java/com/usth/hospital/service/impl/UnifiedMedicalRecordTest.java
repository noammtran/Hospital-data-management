package com.usth.hospital.service.impl;

import com.usth.hospital.dto.response.*;
import com.usth.hospital.model.CareVisitEntity;
import com.usth.hospital.repository.*;
import com.usth.hospital.repository.MedicalRecordRepository.*;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class UnifiedMedicalRecordTest {
    @Mock MedicalRecordRepository medicalRecordRepository;
    @Mock PatientRepository patientRepository;
    @Mock CareVisitRepository careVisits;
    @Mock CareQueryService careQuery;
    @InjectMocks MedicalRecordServiceImpl service;
    private static final String PATIENT = "P26AA001";

    @Test void outpatientIncludesVisitOnlyDiagnosesAndPrescriptions() {
        var detail=detail("care-1",null,"COMPLETED");
        var diagnosis=diagnosis(null,"care-1");
        var prescription=mock(PrescriptionRow.class);
        when(prescription.getVisitId()).thenReturn("care-1");
        when(prescription.getMedicineName()).thenReturn("Fictional medicine");
        when(patientRepository.existsById(PATIENT)).thenReturn(true);
        when(careQuery.medicalRecords(anyList())).thenReturn(List.of(detail));
        when(medicalRecordRepository.findDiagnoses(PATIENT)).thenReturn(List.of(diagnosis));
        when(medicalRecordRepository.findPrescriptions(PATIENT)).thenReturn(List.of(prescription));
        assertThat(service.getMedicalRecord(PATIENT)).singleElement().satisfies(v -> {
            assertThat(v.admissionId()).isNull();
            assertThat(v.care().conclusion()).isEqualTo("Recorded conclusion");
            assertThat(v.diagnoses()).hasSize(1);
            assertThat(v.prescriptions()).extracting(VisitPrescriptionResponse::medicineName)
                .containsExactly("Fictional medicine");
        });
    }

    @Test void linkedAdmissionAppearsOnceAndKeepsBothClinicalLinks() {
        var admission=admission("a1","2026-08-01",null);
        when(patientRepository.existsById(PATIENT)).thenReturn(true);
        when(careVisits.findByPatientIdOrderByCreatedAtDesc(PATIENT))
            .thenReturn(List.of(CareVisitEntity.builder().id("care-1").admissionId("a1").build()));
        when(careQuery.medicalRecords(anyList())).thenReturn(List.of(detail("care-1","a1","INPATIENT")));
        when(medicalRecordRepository.findVisits(PATIENT)).thenReturn(List.of(admission));
        doReturn(List.of(diagnosis("a1","care-1"),diagnosis("a1",null)))
            .when(medicalRecordRepository).findDiagnoses(PATIENT);
        assertThat(service.getMedicalRecord(PATIENT)).singleElement().satisfies(v -> {
            assertThat(v.admissionId()).isEqualTo("a1");
            assertThat(v.admissionDate()).isEqualTo("2026-08-01");
            assertThat(v.care().id()).isEqualTo("care-1");
            assertThat(v.diagnoses()).hasSize(2);
        });
    }

    @Test void inaccessibleCareCannotLeakThroughItsAdmissionFallback() {
        when(patientRepository.existsById(PATIENT)).thenReturn(true);
        when(careVisits.findByPatientIdOrderByCreatedAtDesc(PATIENT))
            .thenReturn(List.of(CareVisitEntity.builder().id("hidden").admissionId("a1").build()));
        doReturn(List.of(admission("a1",null,null))).when(medicalRecordRepository).findVisits(PATIENT);
        assertThat(service.getMedicalRecord(PATIENT)).isEmpty();
    }

    @Test void independentHistoryIsSortedWithoutInventingCareConclusions() {
        when(patientRepository.existsById(PATIENT)).thenReturn(true);
        doReturn(List.of(admission("old","2025-01-01","2025-01-03"),
                admission("new","2026-09-09","2026-09-09")))
            .when(medicalRecordRepository).findVisits(PATIENT);
        var records=service.getMedicalRecord(PATIENT);
        assertThat(records).extracting(VisitResponse::admissionId).containsExactly("new","old");
        assertThat(records).allSatisfy(v -> assertThat(v.care()).isNull());
    }

    @Test void patientGetsClinicalDataOnlyForCompletedEpisodesAndNoAdmissionNotes() {
        doReturn(List.of(admission("active","2026-09-01",null),
                admission("closed","2026-08-01","2026-08-02")))
            .when(medicalRecordRepository).findVisits(PATIENT);
        doReturn(List.of(diagnosis("active",null),diagnosis("closed",null),diagnosis(null,"care-active")))
            .when(medicalRecordRepository).findDiagnoses(PATIENT);
        when(careQuery.patientMedicalRecords(anyList()))
            .thenReturn(List.of(detail("care-active",null,"ACTIVE")));
        var records=service.getPatientRecord(PATIENT);
        assertThat(records).filteredOn(v -> "closed".equals(v.admissionId()))
            .singleElement().satisfies(v -> assertThat(v.diagnoses()).hasSize(1));
        assertThat(records).filteredOn(v -> !"closed".equals(v.admissionId()))
            .allSatisfy(v -> { assertThat(v.diagnoses()).isEmpty(); assertThat(v.prescriptions()).isEmpty(); });
        assertThat(records).allSatisfy(v -> assertThat(v.notes()).isNull());
    }

    private VisitRow admission(String id,String date,String end) {
        var row=mock(VisitRow.class);
        when(row.getAdmissionId()).thenReturn(id);
        lenient().when(row.getAdmissionDate()).thenReturn(date);
        lenient().when(row.getDischargeDate()).thenReturn(end);
        return row;
    }
    private DiagnosisRow diagnosis(String admission,String visit) {
        var row=mock(DiagnosisRow.class);
        when(row.getAdmissionId()).thenReturn(admission);
        when(row.getVisitId()).thenReturn(visit);
        return row;
    }
    private CareResponses.Detail detail(String id,String admission,String status) {
        var time=LocalDateTime.of(2026,9,10,8,0);
        var visit=new CareResponses.Visit(id,PATIENT,"Fictional patient",1,status,status,1L,"Clinic",
            null,null,2L,"Doctor",0,time,"COMPLETED".equals(status)?time.plusHours(1):null,
            0,0,0,false,admission,null);
        return new CareResponses.Detail(visit,"Complaint","Recorded conclusion","Plan","OUTPATIENT",
            null,null,null,List.of(),List.of(),List.of(),List.of(),List.of());
    }
}
