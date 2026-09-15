package com.usth.hospital.repository;

import com.usth.hospital.model.PatientEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read-only reporting queries. They join straight to the seeded transactional
 * tables (ADMISSIONS, DIAGNOSES, PRESCRIPTIONS) and return lightweight
 * projections, so no JPA entities are needed for those tables.
 */
public interface StatisticsRepository extends Repository<PatientEntity, String> {

    /** Patients matching every supplied cohort criterion with activity counts. */
    @Query(value = """
            SELECT p.patient_id AS id,
                   p.patient_name AS name,
                   p.patient_identity_no AS "identityNo",
                   p.patient_birth_date AS "birthDate",
                   p.patient_gender AS gender,
                   p.patient_status AS status,
                   (SELECT COUNT(*)
                      FROM ADMISSIONS ca
                     WHERE ca.admission_patient_id = p.patient_id
                       AND (:fromDate IS NULL OR TRUNC(ca.admission_date) BETWEEN :fromDate AND :toDate))
                       AS "admissionCount",
                   (SELECT COUNT(*)
                      FROM DIAGNOSES cd
                     WHERE cd.diagnose_patient_id = p.patient_id
                       AND (:fromDate IS NULL OR TRUNC(cd.diagnose_date) BETWEEN :fromDate AND :toDate))
                       AS "diagnosisCount",
                   (SELECT COUNT(*)
                      FROM PRESCRIPTIONS cp
                     WHERE cp.prescription_patient_id = p.patient_id
                       AND (:fromDate IS NULL OR TRUNC(cp.prescription_date) BETWEEN :fromDate AND :toDate))
                       AS "prescriptionCount"
            FROM PATIENTS p
            WHERE (:fromDate IS NULL
                   OR EXISTS (
                       SELECT 1
                         FROM ADMISSIONS a
                        WHERE a.admission_patient_id = p.patient_id
                          AND TRUNC(a.admission_date) BETWEEN :fromDate AND :toDate)
                   OR EXISTS (
                       SELECT 1
                         FROM DIAGNOSES activity_d
                        WHERE activity_d.diagnose_patient_id = p.patient_id
                          AND TRUNC(activity_d.diagnose_date) BETWEEN :fromDate AND :toDate)
                   OR EXISTS (
                       SELECT 1
                         FROM PRESCRIPTIONS activity_p
                        WHERE activity_p.prescription_patient_id = p.patient_id
                          AND TRUNC(activity_p.prescription_date) BETWEEN :fromDate AND :toDate))
              AND (:icdCode IS NULL OR EXISTS (
                       SELECT 1 FROM DIAGNOSES d
                       WHERE d.diagnose_patient_id = p.patient_id
                         AND UPPER(d.diagnose_icd_code) = UPPER(:icdCode)
                         AND (:fromDate IS NULL OR TRUNC(d.diagnose_date) BETWEEN :fromDate AND :toDate)))
              AND (:medicineId IS NULL OR EXISTS (
                       SELECT 1 FROM PRESCRIPTIONS pr
                       WHERE pr.prescription_patient_id = p.patient_id
                         AND pr.prescription_medicine_id = :medicineId
                         AND (:fromDate IS NULL OR TRUNC(pr.prescription_date) BETWEEN :fromDate AND :toDate)))
            ORDER BY p.patient_name
            """, nativeQuery = true)
    List<PatientRow> filterPatients(@Param("fromDate") LocalDate fromDate,
                                    @Param("toDate") LocalDate toDate,
                                    @Param("icdCode") String icdCode,
                                    @Param("medicineId") Long medicineId);

    @Query(value = """
            SELECT d.diagnose_icd_code AS code,
                   NVL(MAX(c.short_desc), d.diagnose_icd_code) AS description
            FROM DIAGNOSES d
            LEFT JOIN ICD10_CODES c ON c.icd_code = d.diagnose_icd_code
            WHERE d.diagnose_icd_code IS NOT NULL
            GROUP BY d.diagnose_icd_code
            ORDER BY d.diagnose_icd_code
            """, nativeQuery = true)
    List<DiagnosisRow> findDiagnosisOptions();

    @Query(value = """
            SELECT DISTINCT m.medicine_id AS id, m.medicine_name AS name
            FROM MEDICINES m
            JOIN PRESCRIPTIONS pr ON pr.prescription_medicine_id = m.medicine_id
            ORDER BY m.medicine_name
            """, nativeQuery = true)
    List<MedicineRow> findMedicineOptions();

    interface PatientRow {
        String getId();

        String getName();

        String getIdentityNo();

        LocalDate getBirthDate();

        Integer getGender();

        Integer getStatus();

        Long getAdmissionCount();

        Long getDiagnosisCount();

        Long getPrescriptionCount();
    }

    interface DiagnosisRow {
        String getCode();

        String getDescription();
    }

    interface MedicineRow {
        Long getId();

        String getName();
    }
}
