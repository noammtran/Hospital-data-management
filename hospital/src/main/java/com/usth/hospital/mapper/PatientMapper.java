package com.usth.hospital.mapper;

import com.usth.hospital.dto.request.CreatePatientRequest;
import com.usth.hospital.dto.request.UpdatePatientRequest;
import com.usth.hospital.dto.response.PatientDetailResponse;
import com.usth.hospital.dto.response.PatientHistoryEntryResponse;
import com.usth.hospital.dto.response.PatientHistoryResponse;
import com.usth.hospital.dto.response.PatientListResponse;
import com.usth.hospital.model.PatientEntity;
import com.usth.hospital.model.PatientHistoryEntity;
import com.usth.hospital.model.PatientStatus;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

    public PatientEntity toEntity(CreatePatientRequest req) {
        return PatientEntity.builder()
                .name(req.name())
                .birthDate(req.birthDate())
                .gender(req.gender())
                .address(req.address())
                .phoneNo(req.phoneNo())
                .email(req.email())
                .identityNo(req.identityNo())
                .bloodType(req.bloodType())
                .allergies(req.allergies())
                .medicalHistory(req.medicalHistory())
                .insuranceNo(req.insuranceNo())
                .insuranceExpiry(req.insuranceExpiry())
                .emergencyContactName(req.emergencyContactName())
                .emergencyContactPhone(req.emergencyContactPhone())
                .emergencyContactRelation(req.emergencyContactRelation())
                .status(PatientStatus.WAITING.code())
                .note(req.note())
                .build();
    }

    public void updateEntity(UpdatePatientRequest req, PatientEntity entity) {
        entity.setName(req.name());
        entity.setBirthDate(req.birthDate());
        entity.setGender(req.gender());
        entity.setAddress(req.address());
        entity.setPhoneNo(req.phoneNo());
        entity.setEmail(req.email());
        entity.setIdentityNo(req.identityNo());
        entity.setBloodType(req.bloodType());
        entity.setAllergies(req.allergies());
        entity.setMedicalHistory(req.medicalHistory());
        entity.setInsuranceNo(req.insuranceNo());
        entity.setInsuranceExpiry(req.insuranceExpiry());
        entity.setEmergencyContactName(req.emergencyContactName());
        entity.setEmergencyContactPhone(req.emergencyContactPhone());
        entity.setEmergencyContactRelation(req.emergencyContactRelation());
        entity.setNote(req.note());
    }

    public PatientDetailResponse toDetail(PatientEntity e) {
        return new PatientDetailResponse(
                e.getId(),
                e.getName(),
                e.getBirthDate(),
                e.getGender(),
                e.getAddress(),
                e.getPhoneNo(),
                e.getEmail(),
                e.getIdentityNo(),
                e.getBloodType(),
                e.getAllergies(),
                e.getMedicalHistory(),
                e.getInsuranceNo(),
                e.getInsuranceExpiry(),
                e.getEmergencyContactName(),
                e.getEmergencyContactPhone(),
                e.getEmergencyContactRelation(),
                e.getStatus(),
                e.getNote(),
                e.getCreatedBy(),
                e.getCreatedDate(),
                e.getLastUpdatedBy(),
                e.getLastUpdatedDate()
        );
    }

    public PatientListResponse toListItem(PatientEntity e, boolean hasHistory) {
        return new PatientListResponse(
                e.getId(),
                e.getName(),
                e.getGender(),
                e.getBirthDate(),
                e.getPhoneNo(),
                e.getStatus(),
                hasHistory
        );
    }

    public PatientHistoryResponse toHistory(PatientHistoryEntity e) {
        return new PatientHistoryResponse(
                e.getFieldName(),
                e.getOldValue(),
                e.getNewValue(),
                e.getChangedBy(),
                e.getChangedDate()
        );
    }

    public PatientHistoryEntryResponse toHistoryEntry(PatientHistoryEntity e, String patientName) {
        return new PatientHistoryEntryResponse(
                e.getPatientId(),
                patientName,
                e.getFieldName(),
                e.getOldValue(),
                e.getNewValue(),
                e.getChangedBy(),
                e.getChangedDate()
        );
    }
}
