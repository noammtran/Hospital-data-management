package com.usth.hospital.repository;

import com.usth.hospital.model.PatientEntity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read-only queries that assemble a patient's medical record: their treatment
 * episodes (ADMISSIONS) and, for each episode, the diagnoses and prescriptions
 * recorded during it. Clinical rows include visit and admission links so
 * outpatient care can be assembled without inventing an admission.
 */
public interface MedicalRecordRepository extends Repository<PatientEntity, String> {

    @Query(value = """
            SELECT a.admission_id AS "admissionId",
                   TO_CHAR(a.admission_date, 'YYYY-MM-DD') AS "admissionDate",
                   TO_CHAR(a.admission_discharge_date, 'YYYY-MM-DD') AS "dischargeDate",
                   dep.department_name AS "departmentName",
                   doc.doctor_name AS "doctorName",
                   r.room_name AS "roomName",
                   a.admission_reason AS "reason",
                   a.admission_type AS "type",
                   a.admission_status AS "status",
                   a.admission_notes AS "notes"
            FROM ADMISSIONS a
            JOIN DEPARTMENTS dep ON dep.department_id = a.admission_department_id
            JOIN DOCTORS doc ON doc.doctor_id = a.admission_doctor_id
            LEFT JOIN ROOMS r ON r.room_id = a.admission_room_id
            WHERE a.admission_patient_id = :patientId
            ORDER BY a.admission_date DESC
            """, nativeQuery = true)
    List<VisitRow> findVisits(@Param("patientId") String patientId);

    @Query(value = """
            SELECT d.diagnose_admission_id AS "admissionId",
                   d.diagnose_visit_id AS "visitId",
                   TO_CHAR(d.diagnose_date, 'YYYY-MM-DD') AS "diagnoseDate",
                   d.diagnose_icd_code AS "icdCode",
                   d.diagnose_desc AS "description",
                   d.diagnose_severity AS "severity",
                   doc.doctor_name AS "doctorName"
            FROM DIAGNOSES d
            JOIN DOCTORS doc ON doc.doctor_id = d.diagnose_doctor_id
            WHERE d.diagnose_patient_id = :patientId
            ORDER BY d.diagnose_date
            """, nativeQuery = true)
    List<DiagnosisRow> findDiagnoses(@Param("patientId") String patientId);

    @Query(value = """
            SELECT pr.prescription_admission_id AS "admissionId",
                   pr.prescription_visit_id AS "visitId",
                   m.medicine_name AS "medicineName",
                   pr.prescription_dosage AS "dosage",
                   pr.prescription_frequency AS "frequency",
                   pr.prescription_duration AS "duration",
                   pr.prescription_instructions AS "instructions"
            FROM PRESCRIPTIONS pr
            JOIN MEDICINES m ON m.medicine_id = pr.prescription_medicine_id
            WHERE pr.prescription_patient_id = :patientId
            ORDER BY m.medicine_name
            """, nativeQuery = true)
    List<PrescriptionRow> findPrescriptions(@Param("patientId") String patientId);

    interface VisitRow {
        String getAdmissionId();

        String getAdmissionDate();

        String getDischargeDate();

        String getDepartmentName();

        String getDoctorName();

        String getRoomName();

        String getReason();

        Integer getType();

        Integer getStatus();

        String getNotes();
    }

    interface DiagnosisRow {
        String getVisitId();
        String getAdmissionId();

        String getDiagnoseDate();

        String getIcdCode();

        String getDescription();

        Integer getSeverity();

        String getDoctorName();
    }

    interface PrescriptionRow {
        String getVisitId();
        String getAdmissionId();

        String getMedicineName();

        String getDosage();

        String getFrequency();

        String getDuration();

        String getInstructions();
    }
}
