package com.usth.hospital.repository;

import com.usth.hospital.model.PatientEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface CareReferenceRepository extends Repository<PatientEntity, String> {
    interface Ref { Long getId(); String getName(); }
    interface InpatientRow {
        String getAdmissionId(); String getPatientId(); String getPatientName(); Integer getPatientStatus();
        String getDepartmentName(); String getDoctorName(); String getRoomName(); Long getDepartmentId(); String getVisitId();
    }
    @Query(value="""
        SELECT a.admission_id AS "admissionId", p.patient_id AS "patientId", p.patient_name AS "patientName",
          p.patient_status AS "patientStatus", d.department_name AS "departmentName", dr.doctor_name AS "doctorName",
          r.room_name AS "roomName", d.department_id AS "departmentId", v.care_visit_id AS "visitId"
        FROM ADMISSIONS a JOIN PATIENTS p ON p.patient_id=a.admission_patient_id
        JOIN DEPARTMENTS d ON d.department_id=a.admission_department_id
        JOIN DOCTORS dr ON dr.doctor_id=a.admission_doctor_id
        LEFT JOIN ROOMS r ON r.room_id=a.admission_room_id
        LEFT JOIN CARE_VISITS v ON v.care_visit_admission_id=a.admission_id AND v.care_visit_closed_at IS NULL
        WHERE a.admission_discharge_date IS NULL AND (a.admission_inpatient_flag=1 OR p.patient_status=2)
          AND (:departmentId IS NULL OR d.department_id=:departmentId)
          AND (:allStaff=1 OR EXISTS (SELECT 1 FROM CARE_ASSIGNMENTS ca JOIN CARE_POINTS cp
            ON cp.care_point_id=ca.care_assignment_point_id WHERE ca.care_assignment_user_id=:userId
            AND ca.care_assignment_active=1 AND cp.care_point_kind='WARD' AND cp.care_point_department_id=d.department_id))
          AND (:search IS NULL OR LOWER(p.patient_name) LIKE '%' || LOWER(:search) || '%'
            OR LOWER(p.patient_id) LIKE '%' || LOWER(:search) || '%')
        ORDER BY a.admission_date, a.admission_id
        """, nativeQuery=true)
    Page<InpatientRow> inpatients(int allStaff, Long userId, Long departmentId, String search, Pageable pageable);
    @Query(value="SELECT department_id AS \"id\", department_name AS \"name\" FROM DEPARTMENTS ORDER BY department_name", nativeQuery=true)
    List<Ref> departments();
    @Query(value="SELECT doctor_id AS \"id\", doctor_name AS \"name\" FROM DOCTORS WHERE doctor_status_flag=1 ORDER BY doctor_name", nativeQuery=true)
    List<Ref> doctors();
    @Query(value="""
        SELECT medicine_id AS "id", medicine_name AS "name" FROM MEDICINES
        WHERE medicine_status_flag=1 AND (:search IS NULL OR LOWER(medicine_name) LIKE '%' || LOWER(:search) || '%')
        ORDER BY medicine_name
        """, countQuery="SELECT COUNT(*) FROM MEDICINES WHERE medicine_status_flag=1 AND (:search IS NULL OR LOWER(medicine_name) LIKE '%' || LOWER(:search) || '%')", nativeQuery=true)
    Page<Ref> medicines(String search, Pageable pageable);
    @Query(value="SELECT medicine_name FROM MEDICINES WHERE medicine_id=:id", nativeQuery=true)
    String medicineName(Long id);
    @Query(value="SELECT COUNT(*) FROM MEDICINES WHERE medicine_id=:id AND medicine_status_flag=1", nativeQuery=true)
    int activeMedicine(Long id);
    @Query(value="SELECT COUNT(*) FROM ICD10_CODES WHERE icd_code=:code AND status_flag=1", nativeQuery=true)
    int activeIcd(String code);
    @Query(value="SELECT doctor_name FROM DOCTORS WHERE doctor_id=:id", nativeQuery=true)
    String doctorName(Long id);
}
