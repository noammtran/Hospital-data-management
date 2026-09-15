package com.usth.hospital.model;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;
@Entity
@Table(name="PRESCRIPTIONS")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PrescriptionEntity {
    @Id
    @Column(name="prescription_id", length=50)
    private String id;
    @Column(name="prescription_patient_id", length=50)
    private String patientId;
    @Column(name="prescription_admission_id", length=50)
    private String admissionId;
    @Column(name="prescription_visit_id", length=50)
    private String visitId;
    @Column(name="prescription_doctor_id")
    private Long doctorId;
    @Column(name="prescription_diagnose_id", length=50)
    private String diagnosisId;
    @Column(name="prescription_medicine_id")
    private Long medicineId;
    @Column(name="prescription_date")
    private LocalDate date;
    @Column(name="prescription_dosage", length=100)
    private String dosage;
    @Column(name="prescription_frequency", length=100)
    private String frequency;
    @Column(name="prescription_duration", length=50)
    private String duration;
    @Column(name="prescription_instructions", length=500)
    private String instructions;
}
