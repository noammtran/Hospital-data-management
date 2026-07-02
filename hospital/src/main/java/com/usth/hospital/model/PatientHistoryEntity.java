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
@Table(name = "PATIENT_HISTORY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientHistoryEntity {

    @Id
    @Column(name = "patient_history_id", length = 50, nullable = false)
    private String id;

    @Column(name = "patient_history_patient_id", length = 50, nullable = false)
    private String patientId;

    @Column(name = "patient_history_field_name", length = 50, nullable = false)
    private String fieldName;

    @Column(name = "patient_history_old_value", length = 255)
    private String oldValue;

    @Column(name = "patient_history_new_value", length = 255)
    private String newValue;

    @Column(name = "patient_history_changed_by", length = 30, nullable = false)
    private String changedBy;

    @Column(name = "patient_history_changed_date", nullable = false)
    private LocalDate changedDate;
}
