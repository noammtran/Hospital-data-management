package com.usth.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "CARE_POINTS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarePointEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "care_point_seq")
    @SequenceGenerator(name = "care_point_seq", sequenceName = "care_point_seq", allocationSize = 1)
    @Column(name = "care_point_id", nullable = false)
    private Long id;

    @Column(name = "care_point_code", length = 30, nullable = false)
    private String code;

    @Column(name = "care_point_name", length = 100, nullable = false)
    private String name;

    @Column(name = "care_point_kind", length = 20, nullable = false)
    private String kind;

    @Column(name = "care_point_department_id", nullable = false)
    private Long departmentId;

    @Column(name = "care_point_active", nullable = false)
    private Integer active;

}
