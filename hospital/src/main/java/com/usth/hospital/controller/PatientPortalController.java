package com.usth.hospital.controller;

import com.usth.hospital.dto.response.ApiResponse;
import com.usth.hospital.dto.response.PatientSelfRecordResponse;
import com.usth.hospital.service.PatientPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patient-portal")
@RequiredArgsConstructor
public class PatientPortalController {

    private final PatientPortalService patientPortalService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT') and hasAuthority('VIEW_OWN_RECORD')")
    public ApiResponse<PatientSelfRecordResponse> ownRecord() {
        return ApiResponse.ok(patientPortalService.getOwnRecord());
    }
}
