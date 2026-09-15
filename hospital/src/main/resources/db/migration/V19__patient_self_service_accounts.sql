ALTER TABLE APP_USERS ADD (
    app_user_patient_id               VARCHAR2(50),
    app_user_password_change_required NUMBER(1,0) DEFAULT 0 NOT NULL
);

ALTER TABLE APP_USERS DROP CONSTRAINT ck_app_users_role;

ALTER TABLE APP_USERS ADD CONSTRAINT ck_app_users_role CHECK (
    app_user_role IN ('RECEPTIONIST', 'DOCTOR', 'NURSE', 'ADMIN', 'PATIENT')
);

ALTER TABLE APP_USERS MODIFY (app_user_email NULL);

ALTER TABLE APP_USERS ADD CONSTRAINT fk_app_users_patient
    FOREIGN KEY (app_user_patient_id)
    REFERENCES PATIENTS (patient_id)
    ON DELETE CASCADE;

ALTER TABLE APP_USERS ADD CONSTRAINT uq_app_users_patient UNIQUE (app_user_patient_id);

ALTER TABLE APP_USERS ADD CONSTRAINT ck_app_users_patient_role CHECK (
    (app_user_role = 'PATIENT' AND app_user_patient_id IS NOT NULL)
    OR (app_user_role <> 'PATIENT' AND app_user_patient_id IS NULL)
);

ALTER TABLE APP_USERS ADD CONSTRAINT ck_app_users_staff_email CHECK (
    app_user_role = 'PATIENT' OR app_user_email IS NOT NULL
);

ALTER TABLE APP_USERS ADD CONSTRAINT ck_app_users_password_change CHECK (
    app_user_password_change_required IN (0, 1)
);
