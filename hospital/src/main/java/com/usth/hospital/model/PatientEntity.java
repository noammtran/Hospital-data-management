package com.usth.hospital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PATIENTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientEntity {

    @Id
    @Column(name = "patient_id", length = 50, nullable = false)
    private String id;

    @Column(name = "patient_name", length = 100, nullable = false)
    private String name;

    @Column(name = "patient_birth_date")
    private LocalDate birthDate;

    @Column(name = "patient_gender")
    private Integer gender;

    @Column(name = "patient_address", length = 255)
    private String address;

    @Column(name = "patient_phone_no", length = 20)
    private String phoneNo;

    @Column(name = "patient_email", length = 50)
    private String email;

    @Column(name = "patient_identity_no", length = 20)
    private String identityNo;

    @Column(name = "patient_blood_type")
    private Integer bloodType;

    @Column(name = "patient_allergies", length = 500)
    private String allergies;

    @Column(name = "patient_medical_history", length = 1000)
    private String medicalHistory;

    @Column(name = "patient_insurance_no", length = 20)
    private String insuranceNo;

    @Column(name = "patient_insurance_expiry")
    private LocalDate insuranceExpiry;

    @Column(name = "patient_emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Column(name = "patient_emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "patient_emergency_contact_relation", length = 50)
    private String emergencyContactRelation;

    @Column(name = "patient_status", nullable = false)
    private Integer status;

    @Column(name = "patient_note", length = 1000)
    private String note;

    @Column(name = "patient_created_by", length = 30, nullable = false)
    private String createdBy;

    @Column(name = "patient_created_date", nullable = false)
    private LocalDate createdDate;

    @Column(name = "patient_last_updated_by", length = 30, nullable = false)
    private String lastUpdatedBy;

    @Column(name = "patient_last_updated_date", nullable = false)
    private LocalDate lastUpdatedDate;
}
