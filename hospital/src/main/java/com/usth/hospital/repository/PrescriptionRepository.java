package com.usth.hospital.repository;
import com.usth.hospital.model.PrescriptionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PrescriptionRepository extends JpaRepository<PrescriptionEntity,String> {
    List<PrescriptionEntity> findByVisitIdOrderByDateAsc(String visitId);
    List<PrescriptionEntity> findByDiagnosisId(String diagnosisId);
    void deleteByDiagnosisId(String diagnosisId);
}
