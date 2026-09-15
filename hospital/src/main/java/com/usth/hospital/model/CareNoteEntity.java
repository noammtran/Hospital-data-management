package com.usth.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "CARE_NOTES")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareNoteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "care_note_seq")
    @SequenceGenerator(name = "care_note_seq", sequenceName = "care_note_seq", allocationSize = 1)
    @Column(name = "care_note_id", nullable = false)
    private Long id;

    @Column(name = "care_note_visit_id", length = 50, nullable = false)
    private String visitId;

    @Column(name = "care_note_kind", length = 20, nullable = false)
    private String kind;

    @Lob
    @Column(name = "care_note_body", nullable = false)
    private String body;

    @Column(name = "care_note_created_by", nullable = false)
    private Long createdBy;

    @Column(name = "care_note_created_at", nullable = false)
    private LocalDateTime createdAt;

}
