CREATE TABLE LOOKUP_VALUE (
    id                  NUMBER(10,0)  NOT NULL,
    lookup_value        VARCHAR2(255) NOT NULL,
    lookup_cd           NUMBER(2,0)   NOT NULL,
    lookup_group        VARCHAR2(30)  NOT NULL,
    lookup_status_flag  NUMBER(1,0)   NOT NULL,
    lookup_description  VARCHAR2(255),
    CONSTRAINT pk_lookup_value           PRIMARY KEY (id),
    CONSTRAINT uq_lookup_value_group_cd  UNIQUE (lookup_group, lookup_cd),
    CONSTRAINT ck_lookup_status_flag     CHECK (lookup_status_flag IN (0,1))
);

CREATE INDEX ix_lookup_value_group ON LOOKUP_VALUE (lookup_group);

CREATE SEQUENCE lookup_value_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE DEPARTMENTS (
    department_id           NUMBER(10,0)  NOT NULL,
    department_code         VARCHAR2(20)  NOT NULL,
    department_name         VARCHAR2(100) NOT NULL,
    department_description  VARCHAR2(255),
    CONSTRAINT pk_departments       PRIMARY KEY (department_id),
    CONSTRAINT uq_departments_code  UNIQUE (department_code),
    CONSTRAINT uq_departments_name  UNIQUE (department_name)
);

CREATE SEQUENCE department_seq START WITH 100 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE DOCTORS (
    doctor_id              NUMBER(10,0)  NOT NULL,
    doctor_code            VARCHAR2(20)  NOT NULL,
    doctor_name            VARCHAR2(100) NOT NULL,
    doctor_specialization  VARCHAR2(100),
    doctor_phone_no        VARCHAR2(20),
    doctor_email           VARCHAR2(50),
    doctor_department_id   NUMBER(10,0)  NOT NULL,
    doctor_status_flag     NUMBER(1,0)   NOT NULL,
    CONSTRAINT pk_doctors             PRIMARY KEY (doctor_id),
    CONSTRAINT uq_doctors_code        UNIQUE (doctor_code),
    CONSTRAINT fk_doctors_department  FOREIGN KEY (doctor_department_id) REFERENCES DEPARTMENTS (department_id),
    CONSTRAINT ck_doctors_status_flag CHECK (doctor_status_flag IN (0,1))
);

CREATE INDEX ix_doctors_name          ON DOCTORS (doctor_name);
CREATE INDEX ix_doctors_department_id ON DOCTORS (doctor_department_id);

CREATE SEQUENCE doctor_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE ROOMS (
    room_id            NUMBER(10,0)  NOT NULL,
    room_name          VARCHAR2(50)  NOT NULL,
    room_type          NUMBER(2,0)   NOT NULL,
    department_id      NUMBER(10,0)  NOT NULL,
    bed_count          NUMBER(3,0)   NOT NULL,
    current_occupancy  NUMBER(3,0)   NOT NULL,
    status_flag        NUMBER(1,0)   NOT NULL,
    CONSTRAINT pk_rooms              PRIMARY KEY (room_id),
    CONSTRAINT uq_rooms_name         UNIQUE (room_name),
    CONSTRAINT fk_rooms_department   FOREIGN KEY (department_id) REFERENCES DEPARTMENTS (department_id),
    CONSTRAINT ck_rooms_bed_count    CHECK (bed_count > 0),
    CONSTRAINT ck_rooms_occupancy    CHECK (current_occupancy BETWEEN 0 AND bed_count),
    CONSTRAINT ck_rooms_status_flag  CHECK (status_flag IN (0,1))
);

CREATE INDEX ix_rooms_department_id ON ROOMS (department_id);

CREATE SEQUENCE room_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE MEDICINES (
    medicine_id            NUMBER(10,0)  NOT NULL,
    medicine_code          VARCHAR2(20)  NOT NULL,
    medicine_name          VARCHAR2(200) NOT NULL,
    medicine_generic_name  VARCHAR2(200),
    medicine_type          NUMBER(2,0)   NOT NULL,
    medicine_unit          VARCHAR2(20)  NOT NULL,
    medicine_manufacturer  VARCHAR2(200),
    medicine_description   VARCHAR2(500),
    medicine_side_effects  VARCHAR2(500),
    medicine_status_flag   NUMBER(1,0)   NOT NULL,
    CONSTRAINT pk_medicines             PRIMARY KEY (medicine_id),
    CONSTRAINT uq_medicines_code        UNIQUE (medicine_code),
    CONSTRAINT uq_medicines_name        UNIQUE (medicine_name),
    CONSTRAINT ck_medicines_status_flag CHECK (medicine_status_flag IN (0,1))
);

CREATE SEQUENCE medicine_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE ICD10_CODES (
    id              NUMBER(10,0)  NOT NULL,
    icd_code        VARCHAR2(10)  NOT NULL,
    short_desc      VARCHAR2(100) NOT NULL,
    long_desc       VARCHAR2(255),
    hipaa_covered   NUMBER(1,0)   NOT NULL,
    status_flag     NUMBER(1,0)   NOT NULL,
    CONSTRAINT pk_icd10_codes           PRIMARY KEY (id),
    CONSTRAINT uq_icd10_codes_code      UNIQUE (icd_code),
    CONSTRAINT ck_icd10_hipaa_covered   CHECK (hipaa_covered IN (0,1)),
    CONSTRAINT ck_icd10_status_flag     CHECK (status_flag IN (0,1))
);

CREATE INDEX ix_icd10_codes_short_desc ON ICD10_CODES (short_desc);

CREATE SEQUENCE icd10_seq START WITH 10000 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE PATIENTS (
    patient_id                          VARCHAR2(50)   NOT NULL,
    patient_name                        VARCHAR2(100)  NOT NULL,
    patient_birth_date                  DATE,
    patient_gender                      NUMBER(2,0),
    patient_address                     VARCHAR2(255),
    patient_phone_no                    VARCHAR2(20),
    patient_email                       VARCHAR2(50),
    patient_identity_no                 VARCHAR2(20),
    patient_blood_type                  NUMBER(2,0),
    patient_allergies                   VARCHAR2(500),
    patient_medical_history             VARCHAR2(1000),
    patient_insurance_no                VARCHAR2(20),
    patient_insurance_expiry            DATE,
    patient_emergency_contact_name      VARCHAR2(100),
    patient_emergency_contact_phone     VARCHAR2(20),
    patient_emergency_contact_relation  VARCHAR2(50),
    patient_status                      NUMBER(2,0)    NOT NULL,
    patient_note                        VARCHAR2(1000),
    patient_created_by                  VARCHAR2(30)   NOT NULL,
    patient_created_date                DATE           NOT NULL,
    patient_last_updated_by             VARCHAR2(30)   NOT NULL,
    patient_last_updated_date           DATE           NOT NULL,
    CONSTRAINT pk_patients         PRIMARY KEY (patient_id),
    CONSTRAINT ck_patient_id_format CHECK (REGEXP_LIKE(patient_id, '^P[0-9]{2}[A-Z]{2}[0-9]{3}$')),
    CONSTRAINT ck_patient_status   CHECK (patient_status BETWEEN 0 AND 3)
);

CREATE INDEX ix_patients_name        ON PATIENTS (patient_name);
CREATE INDEX ix_patients_status      ON PATIENTS (patient_status);
CREATE INDEX ix_patients_phone_no    ON PATIENTS (patient_phone_no);
CREATE INDEX ix_patients_identity_no ON PATIENTS (patient_identity_no);

CREATE TABLE ADMISSIONS (
    admission_id              VARCHAR2(50)  NOT NULL,
    admission_patient_id      VARCHAR2(50)  NOT NULL,
    admission_department_id   NUMBER(10,0)  NOT NULL,
    admission_doctor_id       NUMBER(10,0)  NOT NULL,
    admission_room_id         NUMBER(10,0),
    admission_date            DATE          NOT NULL,
    admission_discharge_date  DATE,
    admission_reason          VARCHAR2(500),
    admission_type            NUMBER(2,0),
    admission_status          NUMBER(2,0)   NOT NULL,
    admission_notes           VARCHAR2(500),
    CONSTRAINT pk_admissions             PRIMARY KEY (admission_id),
    CONSTRAINT fk_admissions_patient     FOREIGN KEY (admission_patient_id)    REFERENCES PATIENTS (patient_id) ON DELETE CASCADE,
    CONSTRAINT fk_admissions_department  FOREIGN KEY (admission_department_id) REFERENCES DEPARTMENTS (department_id),
    CONSTRAINT fk_admissions_doctor      FOREIGN KEY (admission_doctor_id)     REFERENCES DOCTORS (doctor_id),
    CONSTRAINT fk_admissions_room        FOREIGN KEY (admission_room_id)       REFERENCES ROOMS (room_id)
);

CREATE INDEX ix_admissions_patient_id ON ADMISSIONS (admission_patient_id);
CREATE INDEX ix_admissions_doctor_id  ON ADMISSIONS (admission_doctor_id);
CREATE INDEX ix_admissions_date       ON ADMISSIONS (admission_date);

CREATE UNIQUE INDEX ux_admissions_active_patient
    ON ADMISSIONS (CASE WHEN admission_discharge_date IS NULL THEN admission_patient_id END);

CREATE TABLE DIAGNOSES (
    diagnose_id           VARCHAR2(50)  NOT NULL,
    diagnose_patient_id   VARCHAR2(50)  NOT NULL,
    diagnose_admission_id VARCHAR2(50),
    diagnose_doctor_id    NUMBER(10,0)  NOT NULL,
    diagnose_date         DATE          NOT NULL,
    diagnose_icd_code     VARCHAR2(10),
    diagnose_desc         VARCHAR2(500) NOT NULL,
    diagnose_severity     NUMBER(2,0),
    diagnose_notes        VARCHAR2(500),
    CONSTRAINT pk_diagnoses           PRIMARY KEY (diagnose_id),
    CONSTRAINT fk_diagnoses_patient   FOREIGN KEY (diagnose_patient_id)   REFERENCES PATIENTS (patient_id) ON DELETE CASCADE,
    CONSTRAINT fk_diagnoses_admission FOREIGN KEY (diagnose_admission_id) REFERENCES ADMISSIONS (admission_id) ON DELETE CASCADE,
    CONSTRAINT fk_diagnoses_doctor    FOREIGN KEY (diagnose_doctor_id)    REFERENCES DOCTORS (doctor_id)
);

CREATE INDEX ix_diagnoses_patient_id   ON DIAGNOSES (diagnose_patient_id);
CREATE INDEX ix_diagnoses_admission_id ON DIAGNOSES (diagnose_admission_id);
CREATE INDEX ix_diagnoses_doctor_id    ON DIAGNOSES (diagnose_doctor_id);
CREATE INDEX ix_diagnoses_icd_code     ON DIAGNOSES (diagnose_icd_code);

CREATE TABLE PRESCRIPTIONS (
    prescription_id            VARCHAR2(50)  NOT NULL,
    prescription_patient_id    VARCHAR2(50)  NOT NULL,
    prescription_admission_id  VARCHAR2(50),
    prescription_doctor_id     NUMBER(10,0)  NOT NULL,
    prescription_diagnose_id   VARCHAR2(50)  NOT NULL,
    prescription_medicine_id   NUMBER(10,0)  NOT NULL,
    prescription_date          DATE          NOT NULL,
    prescription_dosage        VARCHAR2(100) NOT NULL,
    prescription_frequency     VARCHAR2(100) NOT NULL,
    prescription_duration      VARCHAR2(50)  NOT NULL,
    prescription_instructions  VARCHAR2(500),
    CONSTRAINT pk_prescriptions           PRIMARY KEY (prescription_id),
    CONSTRAINT fk_prescriptions_patient   FOREIGN KEY (prescription_patient_id)   REFERENCES PATIENTS (patient_id) ON DELETE CASCADE,
    CONSTRAINT fk_prescriptions_admission FOREIGN KEY (prescription_admission_id) REFERENCES ADMISSIONS (admission_id) ON DELETE CASCADE,
    CONSTRAINT fk_prescriptions_doctor    FOREIGN KEY (prescription_doctor_id)    REFERENCES DOCTORS (doctor_id),
    CONSTRAINT fk_prescriptions_diagnose  FOREIGN KEY (prescription_diagnose_id)  REFERENCES DIAGNOSES (diagnose_id) ON DELETE CASCADE,
    CONSTRAINT fk_prescriptions_medicine  FOREIGN KEY (prescription_medicine_id)  REFERENCES MEDICINES (medicine_id)
);

CREATE INDEX ix_prescriptions_patient_id   ON PRESCRIPTIONS (prescription_patient_id);
CREATE INDEX ix_prescriptions_admission_id ON PRESCRIPTIONS (prescription_admission_id);
CREATE INDEX ix_prescriptions_doctor_id    ON PRESCRIPTIONS (prescription_doctor_id);
CREATE INDEX ix_prescriptions_medicine_id  ON PRESCRIPTIONS (prescription_medicine_id);

CREATE TABLE APPOINTMENTS (
    appointment_id             VARCHAR2(50)  NOT NULL,
    appointment_patient_id     VARCHAR2(50)  NOT NULL,
    appointment_doctor_id      NUMBER(10,0)  NOT NULL,
    appointment_department_id  NUMBER(10,0)  NOT NULL,
    appointment_date           DATE          NOT NULL,
    appointment_time           VARCHAR2(10),
    appointment_reason         VARCHAR2(500),
    appointment_status         NUMBER(2,0)   NOT NULL,
    appointment_notes          VARCHAR2(500),
    CONSTRAINT pk_appointments            PRIMARY KEY (appointment_id),
    CONSTRAINT fk_appointments_patient    FOREIGN KEY (appointment_patient_id)    REFERENCES PATIENTS (patient_id) ON DELETE CASCADE,
    CONSTRAINT fk_appointments_doctor     FOREIGN KEY (appointment_doctor_id)     REFERENCES DOCTORS (doctor_id),
    CONSTRAINT fk_appointments_department FOREIGN KEY (appointment_department_id) REFERENCES DEPARTMENTS (department_id),
    CONSTRAINT ck_appointment_status      CHECK (appointment_status BETWEEN 0 AND 3)
);

CREATE INDEX ix_appointments_patient_id    ON APPOINTMENTS (appointment_patient_id);
CREATE INDEX ix_appointments_doctor_id     ON APPOINTMENTS (appointment_doctor_id);
CREATE INDEX ix_appointments_department_id ON APPOINTMENTS (appointment_department_id);
CREATE INDEX ix_appointments_date          ON APPOINTMENTS (appointment_date);

CREATE TABLE PATIENT_HISTORY (
    patient_history_id            VARCHAR2(50)   NOT NULL,
    patient_history_patient_id    VARCHAR2(50)   NOT NULL,
    patient_history_field_name    VARCHAR2(50)   NOT NULL,
    patient_history_old_value     VARCHAR2(255),
    patient_history_new_value     VARCHAR2(255),
    patient_history_changed_by    VARCHAR2(30)   NOT NULL,
    patient_history_changed_date  DATE           NOT NULL,
    CONSTRAINT pk_patient_history          PRIMARY KEY (patient_history_id),
    CONSTRAINT fk_patient_history_patient  FOREIGN KEY (patient_history_patient_id) REFERENCES PATIENTS (patient_id) ON DELETE CASCADE
);

CREATE INDEX ix_patient_history_patient_id   ON PATIENT_HISTORY (patient_history_patient_id);
CREATE INDEX ix_patient_history_changed_date ON PATIENT_HISTORY (patient_history_changed_date);
