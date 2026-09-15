package com.usth.hospital.service;

import com.usth.hospital.dto.request.LoginRequest;
import com.usth.hospital.dto.request.ChangeOwnPasswordRequest;
import com.usth.hospital.dto.response.AuthTokenResponse;
import com.usth.hospital.dto.response.CurrentUserResponse;

public interface AuthenticationService {
    AuthTokenResponse login(LoginRequest request);

    CurrentUserResponse currentUser();

    void changePassword(ChangeOwnPasswordRequest request);
}
