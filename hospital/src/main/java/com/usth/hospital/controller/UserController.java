package com.usth.hospital.controller;

import com.usth.hospital.dto.request.CreateUserRequest;
import com.usth.hospital.dto.request.CreateStaffDoctorRequest;
import com.usth.hospital.dto.response.StaffWorkProfileResponse;
import com.usth.hospital.service.impl.StaffWorkProfileService;
import com.usth.hospital.dto.request.UpdateUserRequest;
import com.usth.hospital.dto.request.UpdateUserStatusRequest;
import com.usth.hospital.dto.response.ApiResponse;
import com.usth.hospital.dto.response.PageResponse;
import com.usth.hospital.dto.response.UserResponse;
import com.usth.hospital.security.UserRole;
import com.usth.hospital.service.UserManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MANAGE_USERS')")
@Validated
public class UserController {

    private final UserManagementService userManagementService;
    private final StaffWorkProfileService staffWorkProfileService;

    @GetMapping("/{id}/work-profile")
    public ApiResponse<StaffWorkProfileResponse> workProfile(@PathVariable @Positive Long id) {
        return ApiResponse.ok(staffWorkProfileService.get(id));
    }

    @PostMapping("/{id}/doctor-profile")
    @PreAuthorize("hasAuthority('MANAGE_USERS') and hasAuthority('CARE_MANAGE')")
    public ResponseEntity<ApiResponse<StaffWorkProfileResponse.Doctor>> createDoctorProfile(
            @PathVariable @Positive Long id, @Valid @RequestBody CreateStaffDoctorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(staffWorkProfileService.createDoctor(id, request)));
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active
    ) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("username").ascending());
        return ApiResponse.ok(PageResponse.from(userManagementService.list(search, role, active, pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> get(@PathVariable @Positive Long id) {
        return ApiResponse.ok(userManagementService.get(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userManagementService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ApiResponse.ok(userManagementService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<UserResponse> setActive(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return ApiResponse.ok(userManagementService.setActive(id, request.active()));
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable @Positive Long id) {
        userManagementService.resetPassword(id);
        return ApiResponse.noContent();
    }
}
