package com.usth.hospital.controller;

import com.usth.hospital.dto.request.ChangeOwnPasswordRequest;
import com.usth.hospital.dto.request.LoginRequest;
import com.usth.hospital.dto.response.ApiResponse;
import com.usth.hospital.dto.response.AuthTokenResponse;
import com.usth.hospital.dto.response.CurrentUserResponse;
import com.usth.hospital.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authenticationService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> currentUser() {
        return ApiResponse.ok(authenticationService.currentUser());
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAuthority('CHANGE_OWN_PASSWORD')")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangeOwnPasswordRequest request) {
        authenticationService.changePassword(request);
        return ApiResponse.noContent();
    }
}
