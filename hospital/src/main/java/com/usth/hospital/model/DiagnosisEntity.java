package com.usth.hospital.model;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;
@Entity
@Table(name="DIAGNOSES")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DiagnosisEntity {
    @Id
    @Column(name="diagnose_id", length=50)
    private String id;
    @Column(name="diagnose_patient_id", length=50)
    private String patientId;
    @Column(name="diagnose_admission_id", length=50)
    private String admissionId;
    @Column(name="diagnose_visit_id", length=50)
    private String visitId;
    @Column(name="diagnose_doctor_id")
    private Long doctorId;
    @Column(name="diagnose_date")
    private LocalDate date;
    @Column(name="diagnose_icd_code", length=10)
    private String icdCode;
    @Column(name="diagnose_desc", length=500)
    private String description;
    @Column(name="diagnose_notes", length=500)
    private String notes;
}
