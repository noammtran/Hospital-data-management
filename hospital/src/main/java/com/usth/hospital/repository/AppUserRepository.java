package com.usth.hospital.repository;

import com.usth.hospital.model.AppUserEntity;
import com.usth.hospital.security.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {

    Optional<AppUserEntity> findByUsernameIgnoreCase(String username);

    Optional<AppUserEntity> findByPatientId(String patientId);

    List<AppUserEntity> findByPatientIdIsNullOrderByFullNameAsc();

    boolean existsByDoctorIdAndIdNot(Long doctorId, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUserEntity u where u.id=:id")
    Optional<AppUserEntity> findByIdForUpdate(@Param("id") Long id);

    boolean existsByPatientId(String patientId);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    void deleteByPatientId(String patientId);

    void deleteAllByPatientIdIn(List<String> patientIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT user FROM AppUserEntity user
            WHERE user.role = :role AND user.activeFlag = 1
            ORDER BY user.id
            """)
    List<AppUserEntity> lockActiveUsersByRole(@Param("role") UserRole role);

    @Query("""
            SELECT user FROM AppUserEntity user
            WHERE user.patientId IS NULL
              AND (:search IS NULL
                   OR LOWER(user.username) LIKE LOWER(CONCAT('%', CONCAT(:search, '%')))
                   OR LOWER(user.identityNo) LIKE LOWER(CONCAT('%', CONCAT(:search, '%')))
                   OR LOWER(user.email) LIKE LOWER(CONCAT('%', CONCAT(:search, '%')))
                   OR LOWER(user.fullName) LIKE LOWER(CONCAT('%', CONCAT(:search, '%'))))
              AND (:role IS NULL OR user.role = :role)
              AND (:activeFlag IS NULL OR user.activeFlag = :activeFlag)
            """)
    Page<AppUserEntity> search(String search, UserRole role, Integer activeFlag, Pageable pageable);
}
