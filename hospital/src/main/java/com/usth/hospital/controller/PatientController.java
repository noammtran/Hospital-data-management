package com.usth.hospital.controller;

import com.usth.hospital.dto.request.BulkDeleteRequest;
import com.usth.hospital.dto.request.CreatePatientRequest;
import com.usth.hospital.dto.request.UpdatePatientRequest;
import com.usth.hospital.dto.response.ApiResponse;
import com.usth.hospital.dto.response.PageResponse;
import com.usth.hospital.dto.response.PatientDetailResponse;
import com.usth.hospital.dto.response.PatientHistoryEntryResponse;
import com.usth.hospital.dto.response.PatientHistoryResponse;
import com.usth.hospital.dto.response.PatientListResponse;
import com.usth.hospital.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_PATIENT')")
    public ApiResponse<PageResponse<PatientListResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "name") String searchField,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer gender
    ) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("name").ascending());
        return ApiResponse.ok(PageResponse.from(
                patientService.list(search, searchField, status, gender, pageable)));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('VIEW_PATIENT_HISTORY')")
    public ApiResponse<PageResponse<PatientHistoryEntryResponse>> allHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return ApiResponse.ok(PageResponse.from(patientService.getAllHistory(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_PATIENT')")
    public ApiResponse<PatientDetailResponse> detail(@PathVariable String id) {
        return ApiResponse.ok(patientService.getDetail(id));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('VIEW_PATIENT_HISTORY')")
    public ApiResponse<PageResponse<PatientHistoryResponse>> history(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return ApiResponse.ok(PageResponse.from(patientService.getHistory(id, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_PATIENT')")
    public ResponseEntity<ApiResponse<PatientDetailResponse>> create(@Valid @RequestBody CreatePatientRequest request) {
        PatientDetailResponse created = patientService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('UPDATE_PATIENT')")
    public ApiResponse<PatientDetailResponse> update(@PathVariable String id,
                                                     @Valid @RequestBody UpdatePatientRequest request) {
        return ApiResponse.ok(patientService.update(id, request));
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAuthority('PATIENT_CHECK_IN')")
    public ApiResponse<PatientDetailResponse> checkIn(@PathVariable String id) {
        return ApiResponse.ok(patientService.checkIn(id));
    }

    @PostMapping("/{id}/start-examination")
    @PreAuthorize("hasAuthority('PATIENT_START_EXAMINATION')")
    public ApiResponse<PatientDetailResponse> startExamination(@PathVariable String id) {
        return ApiResponse.ok(patientService.startExamination(id));
    }

    @PostMapping("/{id}/complete-examination")
    @PreAuthorize("hasAuthority('PATIENT_COMPLETE_EXAMINATION')")
    public ApiResponse<PatientDetailResponse> completeExamination(@PathVariable String id) {
        return ApiResponse.ok(patientService.completeExamination(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_PATIENT')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        patientService.delete(id);
        return ApiResponse.noContent();
    }

    @PostMapping("/bulk-delete")
    @PreAuthorize("hasAuthority('DELETE_PATIENT')")
    public ApiResponse<Void> bulkDelete(@Valid @RequestBody BulkDeleteRequest request) {
        patientService.bulkDelete(request.ids());
        return ApiResponse.noContent();
    }
}
