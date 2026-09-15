package com.usth.hospital.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DOCTORS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "doctor_seq")
    @SequenceGenerator(name = "doctor_seq", sequenceName = "doctor_seq", allocationSize = 1)
    @Column(name = "doctor_id", nullable = false)
    private Long id;

    @Column(name = "doctor_code", length = 20, nullable = false)
    private String code;

    @Column(name = "doctor_name", length = 100, nullable = false)
    private String name;

    @Column(name = "doctor_specialization", length = 100)
    private String specialization;

    @Column(name = "doctor_phone_no", length = 20)
    private String phoneNo;

    @Column(name = "doctor_email", length = 50)
    private String email;

    @Column(name = "doctor_department_id", nullable = false)
    private Long departmentId;

    @Column(name = "doctor_status_flag", nullable = false)
    private Integer status;
}
