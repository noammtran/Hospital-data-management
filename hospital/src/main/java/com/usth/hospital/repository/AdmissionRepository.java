package com.usth.hospital.repository;

import com.usth.hospital.model.AdmissionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdmissionRepository extends JpaRepository<AdmissionEntity, String> {

    boolean existsByPatientIdAndDischargeDateIsNull(String patientId);

    Optional<AdmissionEntity> findByPatientIdAndDischargeDateIsNull(String patientId);

    Optional<AdmissionEntity> findByIdAndPatientId(String id, String patientId);

    @Query("select a.patientId from AdmissionEntity a where a.id=:id")
    Optional<String> patientId(String id);

    @Query(value = """
            SELECT admission_room_id
            FROM ADMISSIONS
            WHERE admission_patient_id IN (:patientIds)
              AND admission_discharge_date IS NULL
              AND admission_room_id IS NOT NULL
            """, nativeQuery = true)
    List<Long> findActiveRoomIdsByPatientIds(@Param("patientIds") List<String> patientIds);

    @Query(value = """
            SELECT department_id AS "id",
                   department_code AS "code",
                   department_name AS "name"
            FROM DEPARTMENTS
            ORDER BY department_name
            """, nativeQuery = true)
    List<DepartmentRow> findDepartmentOptions();

    @Query(value = """
            SELECT department_id AS "id",
                   department_code AS "code",
                   department_name AS "name"
            FROM DEPARTMENTS
            WHERE department_id = :departmentId
            """, nativeQuery = true)
    Optional<DepartmentRow> findDepartment(@Param("departmentId") Long departmentId);

    @Query(value = """
            SELECT doctor_id AS "id",
                   doctor_code AS "code",
                   doctor_name AS "name",
                   doctor_department_id AS "departmentId",
                   doctor_status_flag AS "status"
            FROM DOCTORS
            WHERE doctor_status_flag = 1
            ORDER BY doctor_name
            """, nativeQuery = true)
    List<DoctorRow> findActiveDoctorOptions();

    @Query(value = """
            SELECT doctor_id AS "id",
                   doctor_code AS "code",
                   doctor_name AS "name",
                   doctor_department_id AS "departmentId",
                   doctor_status_flag AS "status"
            FROM DOCTORS
            WHERE doctor_id = :doctorId
            """, nativeQuery = true)
    Optional<DoctorRow> findDoctor(@Param("doctorId") Long doctorId);

    @Query(value = """
            SELECT room_id AS "id",
                   room_name AS "name",
                   department_id AS "departmentId",
                   bed_count AS "bedCount",
                   current_occupancy AS "currentOccupancy",
                   status_flag AS "status"
            FROM ROOMS
            WHERE status_flag = 1
              AND current_occupancy < bed_count
            ORDER BY room_name
            """, nativeQuery = true)
    List<RoomRow> findAvailableRoomOptions();

    @Query(value = """
            SELECT room_id AS "id",
                   room_name AS "name",
                   department_id AS "departmentId",
                   bed_count AS "bedCount",
                   current_occupancy AS "currentOccupancy",
                   status_flag AS "status"
            FROM ROOMS
            WHERE room_id = :roomId
            """, nativeQuery = true)
    Optional<RoomRow> findRoom(@Param("roomId") Long roomId);

    @Query(value = """
            SELECT lookup_cd AS "value",
                   lookup_value AS "label"
            FROM LOOKUP_VALUE
            WHERE lookup_group = 'ADMISSION_TYPE'
              AND lookup_status_flag = 1
            ORDER BY lookup_cd
            """, nativeQuery = true)
    List<LookupRow> findActiveAdmissionTypes();

    @Query(value = """
            SELECT lookup_cd AS "value",
                   lookup_value AS "label"
            FROM LOOKUP_VALUE
            WHERE lookup_group = 'ADMISSION_TYPE'
              AND lookup_cd = :type
            """, nativeQuery = true)
    Optional<LookupRow> findAdmissionType(@Param("type") Integer type);

    @Query(value = """
            SELECT COUNT(*)
            FROM LOOKUP_VALUE
            WHERE lookup_group = 'ADMISSION_TYPE'
              AND lookup_cd = :type
              AND lookup_status_flag = 1
            """, nativeQuery = true)
    long countActiveAdmissionType(@Param("type") Integer type);

    @Modifying
    @Query(value = """
            UPDATE ROOMS
            SET current_occupancy = current_occupancy + 1,
                status_flag = CASE
                    WHEN current_occupancy + 1 >= bed_count THEN 0
                    ELSE 1
                END
            WHERE room_id = :roomId
              AND department_id = :departmentId
              AND status_flag = 1
              AND current_occupancy < bed_count
            """, nativeQuery = true)
    int occupyRoom(
            @Param("roomId") Long roomId,
            @Param("departmentId") Long departmentId
    );

    @Modifying
    @Query(value = """
            UPDATE ROOMS
            SET current_occupancy = GREATEST(current_occupancy - 1, 0),
                status_flag = 1
            WHERE room_id = :roomId
            """, nativeQuery = true)
    int releaseRoom(@Param("roomId") Long roomId);

    interface DepartmentRow {
        Long getId();

        String getCode();

        String getName();
    }

    interface DoctorRow {
        Long getId();

        String getCode();

        String getName();

        Long getDepartmentId();

        Integer getStatus();
    }

    interface RoomRow {
        Long getId();

        String getName();

        Long getDepartmentId();

        Integer getBedCount();

        Integer getCurrentOccupancy();

        Integer getStatus();
    }

    interface LookupRow {
        Integer getValue();

        String getLabel();
    }
}
