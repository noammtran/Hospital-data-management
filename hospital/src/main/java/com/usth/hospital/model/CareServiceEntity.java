package com.usth.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "CARE_SERVICES")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareServiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "care_service_seq")
    @SequenceGenerator(name = "care_service_seq", sequenceName = "care_service_seq", allocationSize = 1)
    @Column(name = "care_service_id", nullable = false)
    private Long id;

    @Column(name = "care_service_code", length = 30, nullable = false)
    private String code;

    @Column(name = "care_service_name", length = 100, nullable = false)
    private String name;

    @Column(name = "care_service_point_id", nullable = false)
    private Long pointId;

    @Column(name = "care_service_active", nullable = false)
    private Integer active;

}
