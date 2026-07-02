package com.usth.hospital.service.impl;

import com.usth.hospital.dto.response.DiagnosisOptionResponse;
import com.usth.hospital.dto.response.MedicineOptionResponse;
import com.usth.hospital.dto.response.PatientRefResponse;
import com.usth.hospital.dto.response.StatisticsResponse;
import com.usth.hospital.repository.StatisticsRepository;
import com.usth.hospital.service.StatisticsService;
import java.time.LocalDate;
import java.util.List;
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
        String icd = (icdCode == null || icdCode.isBlank()) ? null : icdCode.trim();
        List<PatientRefResponse> patients = statisticsRepository
                .filterPatients(fromDate, toDate, icd, medicineId).stream()
                .map(r -> new PatientRefResponse(r.getId(), r.getName(), r.getIdentityNo()))
                .toList();
        return new StatisticsResponse(patients.size(), patients);
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
}
