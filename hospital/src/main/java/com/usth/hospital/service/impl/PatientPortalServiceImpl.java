package com.usth.hospital.service.impl;

import com.usth.hospital.dto.response.PatientSelfProfileResponse;
import com.usth.hospital.dto.response.PatientSelfRecordResponse;
import com.usth.hospital.exception.PatientNotFoundException;
import com.usth.hospital.exception.UserNotFoundException;
import com.usth.hospital.model.AppUserEntity;
import com.usth.hospital.model.PatientEntity;
import com.usth.hospital.repository.AppUserRepository;
import com.usth.hospital.repository.PatientRepository;
import com.usth.hospital.security.CurrentUserProvider;
import com.usth.hospital.security.UserRole;
import com.usth.hospital.service.PatientPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientPortalServiceImpl implements PatientPortalService {

    private final CurrentUserProvider currentUserProvider;
    private final AppUserRepository appUserRepository;
    private final PatientRepository patientRepository;
    private final MedicalRecordServiceImpl medicalRecordService;

    @Override
    public PatientSelfRecordResponse getOwnRecord() {
        String username = currentUserProvider.username();
        AppUserEntity account = appUserRepository.findByUsernameIgnoreCase(username)
                .filter(AppUserEntity::isActive)
                .filter(user -> user.getRole() == UserRole.PATIENT)
                .filter(user -> user.getPatientId() != null)
                .orElseThrow(() -> new UserNotFoundException(username));
        PatientEntity patient = patientRepository.findById(account.getPatientId())
                .orElseThrow(() -> new PatientNotFoundException(account.getPatientId()));

        PatientSelfProfileResponse profile = new PatientSelfProfileResponse(
                patient.getId(),
                patient.getName(),
                patient.getBirthDate(),
                patient.getGender(),
                patient.getAddress(),
                patient.getPhoneNo(),
                patient.getEmail(),
                patient.getIdentityNo(),
                patient.getBloodType(),
                patient.getAllergies(),
                patient.getMedicalHistory(),
                patient.getInsuranceNo(),
                patient.getInsuranceExpiry(),
                patient.getEmergencyContactName(),
                patient.getEmergencyContactPhone(),
                patient.getEmergencyContactRelation(),
                patient.getStatus(),
                patient.getNote()
        );
        return new PatientSelfRecordResponse(profile, medicalRecordService.getPatientRecord(patient.getId()));
    }
}
