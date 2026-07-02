package com.usth.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.usth.hospital.dto.response.VisitResponse;
import com.usth.hospital.exception.PatientNotFoundException;
import com.usth.hospital.repository.MedicalRecordRepository;
import com.usth.hospital.repository.MedicalRecordRepository.DiagnosisRow;
import com.usth.hospital.repository.MedicalRecordRepository.PrescriptionRow;
import com.usth.hospital.repository.MedicalRecordRepository.VisitRow;
import com.usth.hospital.repository.PatientRepository;
import com.usth.hospital.service.impl.MedicalRecordServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    private static final String PATIENT_ID = "patient-1";

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private MedicalRecordServiceImpl service;

    @Test
    void getMedicalRecord_unknownPatient_throwsNotFound() {
        when(patientRepository.existsById(PATIENT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getMedicalRecord(PATIENT_ID))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void getMedicalRecord_groupsDiagnosesAndPrescriptionsByEpisode() {
        VisitRow v1row = visit("a1");
        VisitRow v2row = visit("a2");
        DiagnosisRow d1 = diagnosis("a1", "E11.9");
        DiagnosisRow d2 = diagnosis("a2", "I10");
        PrescriptionRow pr1 = prescription("a1", "Metformin");
        when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);
        when(medicalRecordRepository.findVisits(PATIENT_ID)).thenReturn(List.of(v1row, v2row));
        when(medicalRecordRepository.findDiagnoses(PATIENT_ID)).thenReturn(List.of(d1, d2));
        when(medicalRecordRepository.findPrescriptions(PATIENT_ID)).thenReturn(List.of(pr1));

        List<VisitResponse> record = service.getMedicalRecord(PATIENT_ID);

        assertThat(record).hasSize(2);
        VisitResponse v1 = record.stream().filter(v -> v.admissionId().equals("a1")).findFirst().orElseThrow();
        VisitResponse v2 = record.stream().filter(v -> v.admissionId().equals("a2")).findFirst().orElseThrow();
        assertThat(v1.diagnoses()).extracting("icdCode").containsExactly("E11.9");
        assertThat(v1.prescriptions()).extracting("medicineName").containsExactly("Metformin");
        assertThat(v2.diagnoses()).extracting("icdCode").containsExactly("I10");
        assertThat(v2.prescriptions()).isEmpty();
    }

    private VisitRow visit(String id) {
        VisitRow v = mock(VisitRow.class);
        when(v.getAdmissionId()).thenReturn(id);
        return v;
    }

    private DiagnosisRow diagnosis(String admissionId, String icd) {
        DiagnosisRow d = mock(DiagnosisRow.class);
        when(d.getAdmissionId()).thenReturn(admissionId);
        when(d.getIcdCode()).thenReturn(icd);
        return d;
    }

    private PrescriptionRow prescription(String admissionId, String medicine) {
        PrescriptionRow p = mock(PrescriptionRow.class);
        when(p.getAdmissionId()).thenReturn(admissionId);
        when(p.getMedicineName()).thenReturn(medicine);
        return p;
    }
}
