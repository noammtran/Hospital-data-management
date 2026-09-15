package com.usth.hospital.repository;

import com.usth.hospital.model.CareNoteEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareNoteRepository extends JpaRepository<CareNoteEntity, Long> {
    List<CareNoteEntity> findByVisitIdOrderByCreatedAtAscIdAsc(String visitId);
}
