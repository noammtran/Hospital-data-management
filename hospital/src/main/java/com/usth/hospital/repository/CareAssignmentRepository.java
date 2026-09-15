package com.usth.hospital.repository;

import com.usth.hospital.model.CareAssignmentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareAssignmentRepository extends JpaRepository<CareAssignmentEntity, Long> {
    List<CareAssignmentEntity> findByUserIdOrderByIdAsc(Long userId);
    List<CareAssignmentEntity> findByUserIdAndActive(Long userId, Integer active);
    List<CareAssignmentEntity> findByActive(Integer active);
    Optional<CareAssignmentEntity> findByUserIdAndPointId(Long userId, Long pointId);
}
