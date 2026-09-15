package com.usth.hospital.repository;
import com.usth.hospital.model.DiagnosisEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DiagnosisRepository extends JpaRepository<DiagnosisEntity,String> {
    List<DiagnosisEntity> findByVisitIdOrderByDateAsc(String visitId);
}
