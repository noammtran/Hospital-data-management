package com.usth.hospital.service.impl;

import com.usth.hospital.dto.response.*;
import com.usth.hospital.exception.PatientNotFoundException;
import com.usth.hospital.repository.CareVisitRepository;
import com.usth.hospital.repository.MedicalRecordRepository;
import com.usth.hospital.repository.MedicalRecordRepository.*;
import com.usth.hospital.repository.PatientRepository;
import com.usth.hospital.service.MedicalRecordService;
import java.util.*;
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
    private final CareVisitRepository careVisits;
    private final CareQueryService careQuery;

    @Override
    public List<VisitResponse> getMedicalRecord(String patientId) {
        if (!patientRepository.existsById(patientId)) throw new PatientNotFoundException(patientId);
        var careRows = careVisits.findByPatientIdOrderByCreatedAtDesc(patientId);
        // Hide the admission fallback even when its care record is inaccessible.
        var linkedAdmissions = careRows.stream().map(v -> v.getAdmissionId())
                .filter(Objects::nonNull).collect(Collectors.toSet());
        return assemble(patientId, careQuery.medicalRecords(careRows), linkedAdmissions);
    }

    /** The caller must first resolve ownership through the Patient account link. */
    public List<VisitResponse> getPatientRecord(String patientId) {
        var rows = careVisits.findByPatientIdOrderByCreatedAtDesc(patientId);
        var linked = rows.stream().map(v -> v.getAdmissionId()).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return assemble(patientId, careQuery.patientMedicalRecords(rows), linked).stream()
            .map(v -> {
                boolean complete = v.care() == null ? v.dischargeDate() != null
                    : "COMPLETED".equals(v.care().status()) && v.care().closedAt() != null;
                return new VisitResponse(v.admissionId(), v.admissionDate(), v.dischargeDate(),
                    v.departmentName(), v.doctorName(), v.roomName(), v.reason(), v.type(), v.status(),
                    null, complete?v.diagnoses():List.of(), complete?v.prescriptions():List.of(), v.care());
            }).toList();
    }

    List<VisitResponse> assemble(String patientId, List<CareResponses.Detail> details,
                                Set<String> linkedAdmissions) {
        var admissions = medicalRecordRepository.findVisits(patientId);
        var diagnoses = medicalRecordRepository.findDiagnoses(patientId);
        var prescriptions = medicalRecordRepository.findPrescriptions(patientId);
        Map<String, VisitRow> admissionById = admissions.stream()
                .collect(Collectors.toMap(VisitRow::getAdmissionId, v -> v));
        List<VisitResponse> records = new ArrayList<>();
        for (var detail : details) {
            records.add(episode(admissionById.get(detail.visit().admissionId()), detail, diagnoses, prescriptions));
        }
        admissions.stream().filter(a -> !linkedAdmissions.contains(a.getAdmissionId()))
                .forEach(a -> records.add(episode(a, null, diagnoses, prescriptions)));
        records.sort(Comparator.comparing(MedicalRecordServiceImpl::start,
                Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(v -> v.care() == null ? v.admissionId() : v.care().id()));
        return List.copyOf(records);
    }

    private VisitResponse episode(VisitRow admission, CareResponses.Detail detail,
                                  List<DiagnosisRow> diagnoses, List<PrescriptionRow> prescriptions) {
        String admissionId = admission == null ? null : admission.getAdmissionId();
        String careId = detail == null ? null : detail.visit().id();
        var diagnosisRows = diagnoses.stream()
                .filter(d -> belongs(d.getVisitId(), d.getAdmissionId(), careId, admissionId))
                .map(d -> new VisitDiagnosisResponse(d.getDiagnoseDate(), d.getIcdCode(),
                        d.getDescription(), d.getSeverity(), d.getDoctorName())).toList();
        var prescriptionRows = prescriptions.stream()
                .filter(p -> belongs(p.getVisitId(), p.getAdmissionId(), careId, admissionId))
                .map(p -> new VisitPrescriptionResponse(p.getMedicineName(), p.getDosage(),
                        p.getFrequency(), p.getDuration(), p.getInstructions())).toList();
        return new VisitResponse(admissionId, admission == null ? null : admission.getAdmissionDate(),
                admission == null ? null : admission.getDischargeDate(),
                admission == null ? null : admission.getDepartmentName(),
                admission == null ? null : admission.getDoctorName(),
                admission == null ? null : admission.getRoomName(),
                admission == null ? null : admission.getReason(),
                admission == null ? null : admission.getType(),
                admission == null ? null : admission.getStatus(),
                admission == null ? null : admission.getNotes(), diagnosisRows, prescriptionRows,
                detail == null ? null : MedicalRecordCareResponse.from(detail));
    }

    private static boolean belongs(String visitId, String admissionId, String careId, String episodeId) {
        if (visitId != null) return visitId.equals(careId);
        return episodeId != null && episodeId.equals(admissionId);
    }

    private static String start(VisitResponse record) {
        // Imported admissions keep their actual start instead of the workspace entry date.
        String admissionDate = record.admissionDate();
        String careDate = record.care() == null ? null : record.care().createdAt().toString();
        if (admissionDate == null) return careDate;
        if (careDate == null) return admissionDate;
        return admissionDate.compareTo(careDate.substring(0, 10)) < 0 ? admissionDate : careDate;
    }
}
