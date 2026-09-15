package com.usth.hospital.dto.response;

import java.time.LocalDate;

public record PatientSelfProfileResponse(
        String id,
        String name,
        LocalDate birthDate,
        Integer gender,
        String address,
        String phoneNo,
        String email,
        String identityNo,
        Integer bloodType,
        String allergies,
        String medicalHistory,
        String insuranceNo,
        LocalDate insuranceExpiry,
        String emergencyContactName,
        String emergencyContactPhone,
        String emergencyContactRelation,
        Integer status,
        String note
) {
}
