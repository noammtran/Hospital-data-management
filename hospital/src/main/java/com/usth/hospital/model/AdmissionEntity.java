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
@Table(name = "ADMISSIONS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmissionEntity {

    @Id
    @Column(name = "admission_id", length = 50, nullable = false)
    private String id;

    @Column(name = "admission_patient_id", length = 50, nullable = false)
    private String patientId;

    @Column(name = "admission_department_id", nullable = false)
    private Long departmentId;

    @Column(name = "admission_doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "admission_room_id")
    private Long roomId;

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    @Column(name = "admission_discharge_date")
    private LocalDate dischargeDate;

    @Column(name = "admission_reason", length = 500)
    private String reason;

    @Column(name = "admission_type")
    private Integer type;

    @Column(name = "admission_status", nullable = false)
    private Integer status;

    @Column(name = "admission_notes", length = 500)
    private String notes;

    @Builder.Default
    @Column(name = "admission_inpatient_flag", nullable = false)
    private Integer inpatientFlag = 0;
}
