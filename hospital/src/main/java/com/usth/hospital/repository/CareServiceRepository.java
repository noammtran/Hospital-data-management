package com.usth.hospital.repository;

import com.usth.hospital.model.CareServiceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareServiceRepository extends JpaRepository<CareServiceEntity, Long> {
    List<CareServiceEntity> findAllByOrderByNameAsc();
}
