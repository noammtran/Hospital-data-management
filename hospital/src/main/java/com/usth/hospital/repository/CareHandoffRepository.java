package com.usth.hospital.repository;

import com.usth.hospital.model.CareHandoffEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareHandoffRepository extends JpaRepository<CareHandoffEntity, Long> {
    List<CareHandoffEntity> findByVisitIdOrderByCreatedAtAsc(String visitId);
    boolean existsByVisitIdAndKindAndStatus(String visitId, String kind, String status);
}
