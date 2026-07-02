package com.usth.hospital.repository;

import com.usth.hospital.model.PatientHistoryEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientHistoryRepository extends JpaRepository<PatientHistoryEntity, String> {

    Page<PatientHistoryEntity> findByPatientIdOrderByChangedDateDesc(String patientId, Pageable pageable);

    Page<PatientHistoryEntity> findAllByOrderByChangedDateDesc(Pageable pageable);

    @Query("SELECT DISTINCT h.patientId FROM PatientHistoryEntity h WHERE h.patientId IN :ids")
    List<String> findDistinctPatientIdsByPatientIdIn(Collection<String> ids);
}
