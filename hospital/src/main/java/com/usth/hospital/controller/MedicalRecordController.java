package com.usth.hospital.controller;

import com.usth.hospital.dto.response.ApiResponse;
import com.usth.hospital.dto.response.VisitResponse;
import com.usth.hospital.service.MedicalRecordService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @GetMapping("/{id}/medical-record")
    @PreAuthorize("hasAuthority('VIEW_MEDICAL_RECORD')")
    public ApiResponse<List<VisitResponse>> medicalRecord(@PathVariable String id) {
        return ApiResponse.ok(medicalRecordService.getMedicalRecord(id));
    }
}
