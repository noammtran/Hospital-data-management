package com.usth.hospital.service.impl;

import com.usth.hospital.dto.request.ChangeOwnPasswordRequest;
import com.usth.hospital.dto.request.LoginRequest;
import com.usth.hospital.dto.response.AuthTokenResponse;
import com.usth.hospital.dto.response.CurrentUserResponse;
import com.usth.hospital.exception.InvalidCredentialsException;
import com.usth.hospital.exception.InvalidUserOperationException;
import com.usth.hospital.exception.UserNotFoundException;
import com.usth.hospital.model.AppUserEntity;
import com.usth.hospital.repository.AppUserRepository;
import com.usth.hospital.security.CurrentUserProvider;
import com.usth.hospital.security.JwtTokenService;
import com.usth.hospital.service.AuthenticationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;
    private final JwtTokenService jwtTokenService;
    private final CurrentUserProvider currentUserProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthTokenResponse login(LoginRequest request) {
        String username = request.username().trim();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password())
            );
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException();
        }

        AppUserEntity user = appUserRepository.findByUsernameIgnoreCase(username)
                .filter(AppUserEntity::isActive)
                .orElseThrow(InvalidCredentialsException::new);
        return jwtTokenService.issue(user);
    }

    @Override
    public CurrentUserResponse currentUser() {
        AppUserEntity user = appUserRepository.findByUsernameIgnoreCase(currentUserProvider.username())
                .filter(AppUserEntity::isActive)
                .orElseThrow(() -> new UserNotFoundException(currentUserProvider.username()));
        List<String> permissions = effectivePermissionNames(user);
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                permissions,
                user.isPasswordChangeRequired()
        );
    }

    @Override
    @Transactional
    public void changePassword(ChangeOwnPasswordRequest request) {
        AppUserEntity user = appUserRepository.findByUsernameIgnoreCase(currentUserProvider.username())
                .filter(AppUserEntity::isActive)
                .orElseThrow(() -> new UserNotFoundException(currentUserProvider.username()));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidUserOperationException("currentPassword", "Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new InvalidUserOperationException("newPassword", "New password must be different");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangeRequired(false);
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setLastUpdatedBy(user.getUsername());
        user.setLastUpdatedDate(java.time.LocalDateTime.now());
        appUserRepository.save(user);
    }

    private static List<String> effectivePermissionNames(AppUserEntity user) {
        if (user.isPasswordChangeRequired()) {
            return List.of("CHANGE_OWN_PASSWORD");
        }
        return user.getRole().permissions().stream().map(Enum::name).sorted().toList();
    }
}
