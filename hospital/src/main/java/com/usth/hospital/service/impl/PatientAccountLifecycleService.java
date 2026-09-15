package com.usth.hospital.service.impl;

import com.usth.hospital.exception.DuplicateFieldException;
import com.usth.hospital.exception.InvalidUserOperationException;
import com.usth.hospital.model.AppUserEntity;
import com.usth.hospital.model.PatientEntity;
import com.usth.hospital.repository.AppUserRepository;
import com.usth.hospital.security.CurrentUserProvider;
import com.usth.hospital.security.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatientAccountLifecycleService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;

    public void createFor(PatientEntity patient) {
        String identityNo = requireIdentity(patient.getIdentityNo());
        requireAvailableUsername(identityNo, null);

        String actor = currentUserProvider.username();
        LocalDateTime now = LocalDateTime.now();
        appUserRepository.save(AppUserEntity.builder()
                .username(identityNo)
                .email(null)
                .passwordHash(passwordEncoder.encode(identityNo))
                .tokenVersion(0L)
                .patientId(patient.getId())
                .passwordChangeRequired(1)
                .fullName(patient.getName())
                .role(UserRole.PATIENT)
                .activeFlag(1)
                .createdBy(actor)
                .createdDate(now)
                .lastUpdatedBy(actor)
                .lastUpdatedDate(now)
                .build());
    }

    public void synchronize(PatientEntity patient) {
        AppUserEntity account = appUserRepository.findByPatientId(patient.getId()).orElse(null);
        if (account == null) {
            if (patient.getIdentityNo() != null && !patient.getIdentityNo().isBlank()) {
                createFor(patient);
            }
            return;
        }

        String identityNo = requireIdentity(patient.getIdentityNo());
        boolean usernameChanged = !account.getUsername().equals(identityNo);
        if (usernameChanged) {
            requireAvailableUsername(identityNo, account.getId());
            account.setUsername(identityNo);
            if (account.isPasswordChangeRequired()) {
                account.setPasswordHash(passwordEncoder.encode(identityNo));
            }
            account.setTokenVersion(account.getTokenVersion() + 1);
        }
        account.setFullName(patient.getName());
        account.setLastUpdatedBy(currentUserProvider.username());
        account.setLastUpdatedDate(LocalDateTime.now());
        appUserRepository.save(account);
    }

    public void deleteFor(String patientId) {
        appUserRepository.deleteByPatientId(patientId);
    }

    public void deleteFor(List<String> patientIds) {
        appUserRepository.deleteAllByPatientIdIn(patientIds);
    }

    private void requireAvailableUsername(String identityNo, Long currentAccountId) {
        boolean taken = currentAccountId == null
                ? appUserRepository.existsByUsernameIgnoreCase(identityNo)
                : appUserRepository.existsByUsernameIgnoreCaseAndIdNot(identityNo, currentAccountId);
        if (taken) {
            throw new DuplicateFieldException(
                    "identityNo", "Identity card number conflicts with an existing login account");
        }
    }

    private static String requireIdentity(String identityNo) {
        if (identityNo == null || identityNo.isBlank()) {
            throw new InvalidUserOperationException(
                    "identityNo", "Identity card number is required while a patient account exists");
        }
        return identityNo.trim();
    }
}
