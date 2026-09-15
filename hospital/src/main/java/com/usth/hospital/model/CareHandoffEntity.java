package com.usth.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "CARE_HANDOFFS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareHandoffEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "care_handoff_seq")
    @SequenceGenerator(name = "care_handoff_seq", sequenceName = "care_handoff_seq", allocationSize = 1)
    @Column(name = "care_handoff_id", nullable = false)
    private Long id;

    @Column(name = "care_handoff_visit_id", length = 50, nullable = false)
    private String visitId;

    @Column(name = "care_handoff_from_user", nullable = true)
    private Long fromUserId;

    @Column(name = "care_handoff_to_user", nullable = false)
    private Long toUserId;

    @Column(name = "care_handoff_point_id", nullable = false)
    private Long pointId;

    @Column(name = "care_handoff_kind", length = 20, nullable = false)
    private String kind;

    @Column(name = "care_handoff_status", length = 20, nullable = false)
    private String status;

    @Column(name = "care_handoff_reason", length = 1000, nullable = false)
    private String reason;

    @Lob
    @Column(name = "care_handoff_response", nullable = true)
    private String response;

    @Column(name = "care_handoff_created_by", nullable = false)
    private Long createdBy;

    @Column(name = "care_handoff_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "care_handoff_responded_at", nullable = true)
    private LocalDateTime respondedAt;

}
