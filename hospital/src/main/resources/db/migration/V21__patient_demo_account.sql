-- Provide one fictional Patient login whose linked record already has three
-- admissions, diagnoses, and prescriptions in the shipped demonstration data.
-- The 12-digit identity number is a one-time credential input. Only its BCrypt
-- hash is stored, and the Patient must replace it after signing in.
MERGE INTO APP_USERS target
USING (
    SELECT patient_id, patient_identity_no, patient_name
    FROM PATIENTS patient
    WHERE patient_id = 'P26ML011'
      AND patient_identity_no = '001068011234'
      AND (
          SELECT COUNT(*)
          FROM ADMISSIONS admission
          WHERE admission.admission_patient_id = patient.patient_id
      ) > 2
) source
ON (target.app_user_patient_id = source.patient_id)
WHEN MATCHED THEN UPDATE SET
    target.app_user_username = source.patient_identity_no,
    target.app_user_email = NULL,
    target.app_user_password_hash = '$2a$12$PmhPBNh18rRvX2IkBivfVuZrhivcuMMScsvDHvwrqCudVGLGSLjqe',
    target.app_user_identity_no = NULL,
    target.app_user_token_version = target.app_user_token_version + 1,
    target.app_user_full_name = source.patient_name,
    target.app_user_role = 'PATIENT',
    target.app_user_password_change_required = 1,
    target.app_user_active_flag = 1,
    target.app_user_last_updated_by = 'SYSTEM',
    target.app_user_last_updated_date = SYSTIMESTAMP
WHEN NOT MATCHED THEN INSERT (
    app_user_id,
    app_user_username,
    app_user_email,
    app_user_password_hash,
    app_user_identity_no,
    app_user_token_version,
    app_user_full_name,
    app_user_role,
    app_user_patient_id,
    app_user_password_change_required,
    app_user_active_flag,
    app_user_created_by,
    app_user_created_date,
    app_user_last_updated_by,
    app_user_last_updated_date
) VALUES (
    app_user_seq.NEXTVAL,
    source.patient_identity_no,
    NULL,
    '$2a$12$PmhPBNh18rRvX2IkBivfVuZrhivcuMMScsvDHvwrqCudVGLGSLjqe',
    NULL,
    0,
    source.patient_name,
    'PATIENT',
    source.patient_id,
    1,
    1,
    'SYSTEM',
    SYSTIMESTAMP,
    'SYSTEM',
    SYSTIMESTAMP
);
