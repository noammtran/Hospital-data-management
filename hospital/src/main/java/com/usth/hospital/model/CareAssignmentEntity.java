package com.usth.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "CARE_ASSIGNMENTS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareAssignmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "care_assignment_seq")
    @SequenceGenerator(name = "care_assignment_seq", sequenceName = "care_assignment_seq", allocationSize = 1)
    @Column(name = "care_assignment_id", nullable = false)
    private Long id;

    @Column(name = "care_assignment_user_id", nullable = false)
    private Long userId;

    @Column(name = "care_assignment_point_id", nullable = false)
    private Long pointId;

    @Column(name = "care_assignment_perform", nullable = false)
    private Integer canPerform;

    @Column(name = "care_assignment_verify", nullable = false)
    private Integer canVerify;

    @Column(name = "care_assignment_active", nullable = false)
    private Integer active;

}
