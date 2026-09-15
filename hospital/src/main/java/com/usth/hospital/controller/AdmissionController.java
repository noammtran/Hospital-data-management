package com.usth.hospital.controller;

import com.usth.hospital.dto.request.CreateAdmissionRequest;
import com.usth.hospital.dto.request.UpdateAdmissionRequest;
import com.usth.hospital.dto.response.AdmissionEditDataResponse;
import com.usth.hospital.dto.response.AdmissionOptionsResponse;
import com.usth.hospital.dto.response.AdmissionResponse;
import com.usth.hospital.dto.response.ApiResponse;
import com.usth.hospital.service.AdmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdmissionController {

    private final AdmissionService admissionService;

    @GetMapping("/admissions/options")
    @PreAuthorize("hasAuthority('ADMISSION_CREATE')")
    public ApiResponse<AdmissionOptionsResponse> options() {
        return ApiResponse.ok(admissionService.getOptions());
    }

    @PostMapping("/patients/{patientId}/admissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ADMISSION_CREATE')")
    public ApiResponse<AdmissionResponse> create(
            @PathVariable String patientId,
            @Valid @RequestBody CreateAdmissionRequest request
    ) {
        return ApiResponse.created(admissionService.create(patientId, request));
    }

    @GetMapping("/patients/{patientId}/admissions/{admissionId}/edit-data")
    @PreAuthorize("hasAuthority('ADMISSION_UPDATE')")
    public ApiResponse<AdmissionEditDataResponse> editData(
            @PathVariable String patientId,
            @PathVariable String admissionId
    ) {
        return ApiResponse.ok(admissionService.getEditData(patientId, admissionId));
    }

    @PutMapping("/patients/{patientId}/admissions/{admissionId}")
    @PreAuthorize("hasAuthority('ADMISSION_UPDATE')")
    public ApiResponse<AdmissionResponse> update(
            @PathVariable String patientId,
            @PathVariable String admissionId,
            @Valid @RequestBody UpdateAdmissionRequest request
    ) {
        return ApiResponse.ok(admissionService.update(patientId, admissionId, request));
    }

    @PostMapping("/patients/{patientId}/admissions/{admissionId}/discharge")
    @PreAuthorize("hasAuthority('ADMISSION_DISCHARGE')")
    public ApiResponse<AdmissionResponse> discharge(
            @PathVariable String patientId,
            @PathVariable String admissionId
    ) {
        return ApiResponse.ok(admissionService.discharge(patientId, admissionId));
    }

    @DeleteMapping("/patients/{patientId}/admissions/{admissionId}")
    @PreAuthorize("hasAuthority('ADMISSION_DELETE')")
    public ApiResponse<Void> delete(
            @PathVariable String patientId,
            @PathVariable String admissionId
    ) {
        admissionService.delete(patientId, admissionId);
        return ApiResponse.noContent();
    }
}
