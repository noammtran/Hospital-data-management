package com.usth.hospital.service.impl;

import com.usth.hospital.dto.response.DiagnosisOptionResponse;
import com.usth.hospital.dto.response.MedicineOptionResponse;
import com.usth.hospital.dto.response.PatientRefResponse;
import com.usth.hospital.dto.response.StatisticsBreakdownResponse;
import com.usth.hospital.dto.response.StatisticsResponse;
import com.usth.hospital.dto.response.StatisticsSummaryResponse;
import com.usth.hospital.exception.InvalidStatisticsFilterException;
import com.usth.hospital.repository.StatisticsRepository;
import com.usth.hospital.service.StatisticsService;
import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsRepository statisticsRepository;

    @Override
    public StatisticsResponse filterPatients(LocalDate fromDate, LocalDate toDate, String icdCode, Long medicineId) {
        validateDateRange(fromDate, toDate);
        String icd = (icdCode == null || icdCode.isBlank()) ? null : icdCode.trim();
        List<StatisticsRepository.PatientRow> rows =
                statisticsRepository.filterPatients(fromDate, toDate, icd, medicineId);
        List<PatientRefResponse> patients = rows.stream()
                .map(row -> new PatientRefResponse(
                        row.getId(),
                        row.getName(),
                        row.getIdentityNo(),
                        ageOn(row.getBirthDate(), LocalDate.now()),
                        row.getGender(),
                        row.getStatus(),
                        count(row.getAdmissionCount())
                ))
                .toList();
        StatisticsSummaryResponse summary = new StatisticsSummaryResponse(
                rows.stream().mapToLong(row -> count(row.getAdmissionCount())).sum(),
                rows.stream().mapToLong(row -> count(row.getDiagnosisCount())).sum(),
                rows.stream().mapToLong(row -> count(row.getPrescriptionCount())).sum(),
                patientStatusBreakdown(patients),
                genderBreakdown(patients),
                ageBreakdown(patients)
        );
        return new StatisticsResponse(patients.size(), patients, summary);
    }

    @Override
    public List<DiagnosisOptionResponse> diagnosisOptions() {
        return statisticsRepository.findDiagnosisOptions().stream()
                .map(r -> new DiagnosisOptionResponse(r.getCode(), r.getCode() + " — " + r.getDescription()))
                .toList();
    }

    @Override
    public List<MedicineOptionResponse> medicineOptions() {
        return statisticsRepository.findMedicineOptions().stream()
                .map(r -> new MedicineOptionResponse(r.getId(), r.getName()))
                .toList();
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate != null) {
            throw new InvalidStatisticsFilterException("from", "is required when to is provided");
        }
        if (fromDate != null && toDate == null) {
            throw new InvalidStatisticsFilterException("to", "is required when from is provided");
        }
        if (fromDate != null && fromDate.isAfter(toDate)) {
            throw new InvalidStatisticsFilterException("to", "must be on or after from");
        }
    }

    private Integer ageOn(LocalDate birthDate, LocalDate date) {
        return birthDate == null ? null : Period.between(birthDate, date).getYears();
    }

    private long count(Long value) {
        return value == null ? 0 : value;
    }

    private List<StatisticsBreakdownResponse> patientStatusBreakdown(List<PatientRefResponse> patients) {
        Map<Integer, Long> counts = patients.stream()
                .collect(java.util.stream.Collectors.groupingBy(PatientRefResponse::status,
                        java.util.stream.Collectors.counting()));
        return List.of(
                breakdown("waiting", "Waiting", counts.getOrDefault(0, 0L)),
                breakdown("examining", "Examining", counts.getOrDefault(1, 0L)),
                breakdown("admitted", "Admitted", counts.getOrDefault(2, 0L)),
                breakdown("discharged", "Discharged", counts.getOrDefault(3, 0L))
        );
    }

    private List<StatisticsBreakdownResponse> genderBreakdown(List<PatientRefResponse> patients) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("male", 0L);
        counts.put("female", 0L);
        counts.put("other", 0L);
        counts.put("not-recorded", 0L);
        for (PatientRefResponse patient : patients) {
            String key = switch (patient.gender()) {
                case 1 -> "male";
                case 2 -> "female";
                case 3 -> "other";
                case null, default -> "not-recorded";
            };
            counts.computeIfPresent(key, (ignored, value) -> value + 1);
        }
        return List.of(
                breakdown("male", "Male", counts.get("male")),
                breakdown("female", "Female", counts.get("female")),
                breakdown("other", "Other", counts.get("other")),
                breakdown("not-recorded", "Not recorded", counts.get("not-recorded"))
        );
    }

    private List<StatisticsBreakdownResponse> ageBreakdown(List<PatientRefResponse> patients) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("under-18", 0L);
        counts.put("18-39", 0L);
        counts.put("40-59", 0L);
        counts.put("60-plus", 0L);
        counts.put("not-recorded", 0L);
        for (PatientRefResponse patient : patients) {
            Integer age = patient.age();
            String key = age == null
                    ? "not-recorded"
                    : age < 18 ? "under-18" : age < 40 ? "18-39" : age < 60 ? "40-59" : "60-plus";
            counts.computeIfPresent(key, (ignored, value) -> value + 1);
        }
        return List.of(
                breakdown("under-18", "Under 18", counts.get("under-18")),
                breakdown("18-39", "18 to 39", counts.get("18-39")),
                breakdown("40-59", "40 to 59", counts.get("40-59")),
                breakdown("60-plus", "60 and above", counts.get("60-plus")),
                breakdown("not-recorded", "Not recorded", counts.get("not-recorded"))
        );
    }

    private StatisticsBreakdownResponse breakdown(String key, String label, long count) {
        return new StatisticsBreakdownResponse(key, label, count);
    }
}
