package com.usth.hospital.service;

import com.usth.hospital.dto.response.DiagnosisOptionResponse;
import com.usth.hospital.dto.response.MedicineOptionResponse;
import com.usth.hospital.dto.response.StatisticsResponse;
import java.time.LocalDate;
import java.util.List;

public interface StatisticsService {

    StatisticsResponse filterPatients(LocalDate fromDate, LocalDate toDate, String icdCode, Long medicineId);

    List<DiagnosisOptionResponse> diagnosisOptions();

    List<MedicineOptionResponse> medicineOptions();
}
