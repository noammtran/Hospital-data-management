package com.usth.hospital.repository;

import com.usth.hospital.model.CarePointEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarePointRepository extends JpaRepository<CarePointEntity, Long> {
    List<CarePointEntity> findAllByOrderByNameAsc();
}
