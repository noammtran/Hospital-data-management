package com.usth.hospital.repository;

import com.usth.hospital.model.DoctorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<DoctorEntity, Long> {
    boolean existsByCodeIgnoreCase(String code);
}
