package com.usth.hospital.service.impl;

import com.usth.hospital.dto.response.VisitDiagnosisResponse;
import com.usth.hospital.dto.response.VisitPrescriptionResponse;
import com.usth.hospital.dto.response.VisitResponse;
import com.usth.hospital.exception.PatientNotFoundException;
import com.usth.hospital.repository.MedicalRecordRepository;
import com.usth.hospital.repository.MedicalRecordRepository.DiagnosisRow;
import com.usth.hospital.repository.MedicalRecordRepository.PrescriptionRow;
import com.usth.hospital.repository.PatientRepository;
import com.usth.hospital.service.MedicalRecordService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;

    @Override
    public List<VisitResponse> getMedicalRecord(String patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new PatientNotFoundException(patientId);
        }

        Map<String, List<VisitDiagnosisResponse>> diagnosesByVisit =
                medicalRecordRepository.findDiagnoses(patientId).stream()
                        .collect(Collectors.groupingBy(DiagnosisRow::getAdmissionId,
                                Collectors.mapping(d -> new VisitDiagnosisResponse(
                                        d.getDiagnoseDate(), d.getIcdCode(), d.getDescription(),
                                        d.getSeverity(), d.getDoctorName()), Collectors.toList())));

        Map<String, List<VisitPrescriptionResponse>> prescriptionsByVisit =
                medicalRecordRepository.findPrescriptions(patientId).stream()
                        .collect(Collectors.groupingBy(PrescriptionRow::getAdmissionId,
                                Collectors.mapping(p -> new VisitPrescriptionResponse(
                                        p.getMedicineName(), p.getDosage(), p.getFrequency(),
                                        p.getDuration(), p.getInstructions()), Collectors.toList())));

        return medicalRecordRepository.findVisits(patientId).stream()
                .map(v -> new VisitResponse(
                        v.getAdmissionId(), v.getAdmissionDate(), v.getDischargeDate(),
                        v.getDepartmentName(), v.getDoctorName(), v.getRoomName(),
                        v.getReason(), v.getType(), v.getStatus(), v.getNotes(),
                        diagnosesByVisit.getOrDefault(v.getAdmissionId(), List.of()),
                        prescriptionsByVisit.getOrDefault(v.getAdmissionId(), List.of())))
                .toList();
    }
}
