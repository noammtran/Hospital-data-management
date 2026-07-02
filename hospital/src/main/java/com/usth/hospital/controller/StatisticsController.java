package com.usth.hospital.controller;

import com.usth.hospital.dto.response.ApiResponse;
import com.usth.hospital.dto.response.DiagnosisOptionResponse;
import com.usth.hospital.dto.response.MedicineOptionResponse;
import com.usth.hospital.dto.response.StatisticsResponse;
import com.usth.hospital.service.StatisticsService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/patients")
    public ApiResponse<StatisticsResponse> filter(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String icdCode,
            @RequestParam(required = false) Long medicineId) {
        return ApiResponse.ok(statisticsService.filterPatients(from, to, icdCode, medicineId));
    }

    @GetMapping("/diagnosis-options")
    public ApiResponse<List<DiagnosisOptionResponse>> diagnosisOptions() {
        return ApiResponse.ok(statisticsService.diagnosisOptions());
    }

    @GetMapping("/medicine-options")
    public ApiResponse<List<MedicineOptionResponse>> medicineOptions() {
        return ApiResponse.ok(statisticsService.medicineOptions());
    }
}
