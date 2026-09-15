package com.usth.hospital.repository;

import com.usth.hospital.model.CareResultEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareResultRepository extends JpaRepository<CareResultEntity, Long> {
    List<CareResultEntity> findByTaskIdOrderByRevisionAsc(Long taskId);
    List<CareResultEntity> findByTaskIdInOrderByRevisionAsc(List<Long> taskIds);
}
