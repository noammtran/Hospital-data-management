package com.usth.hospital.service;

import com.usth.hospital.dto.request.CreateUserRequest;
import com.usth.hospital.dto.request.UpdateUserRequest;
import com.usth.hospital.dto.response.UserResponse;
import com.usth.hospital.security.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserManagementService {
    Page<UserResponse> list(String search, UserRole role, Boolean active, Pageable pageable);

    UserResponse get(Long id);

    UserResponse create(CreateUserRequest request);

    UserResponse update(Long id, UpdateUserRequest request);

    UserResponse setActive(Long id, boolean active);

    void resetPassword(Long id);
}
