package com.usth.hospital.repository;

import com.usth.hospital.model.PatientEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, String> {

    @Query("""
            SELECT p FROM PatientEntity p
            WHERE (:search IS NULL
                   OR (:searchField = 'name' AND LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
                   OR (:searchField = 'id' AND LOWER(p.id) LIKE LOWER(CONCAT('%', :search, '%')))
                   OR (:searchField = 'phone' AND p.phoneNo LIKE CONCAT('%', :search, '%'))
                   OR (:searchField = 'identity' AND p.identityNo LIKE CONCAT('%', :search, '%')))
              AND (:status IS NULL OR p.status = :status)
              AND (:gender IS NULL OR p.gender = :gender)
            """)
    Page<PatientEntity> search(@Param("search") String search,
                               @Param("searchField") String searchField,
                               @Param("status") Integer status,
                               @Param("gender") Integer gender,
                               Pageable pageable);

    boolean existsByIdentityNo(String identityNo);

    boolean existsByIdentityNoAndIdNot(String identityNo, String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PatientEntity p WHERE p.id = :id")
    Optional<PatientEntity> findByIdForUpdate(@Param("id") String id);

    @Query("SELECT p.id FROM PatientEntity p WHERE p.id LIKE CONCAT(:prefix, '%')")
    List<String> findIdsByPrefix(@Param("prefix") String prefix);
}
