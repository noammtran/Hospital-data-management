-- Clinical visits and service queues are separate from inpatient bed rooms.
-- Existing clinical history is not backfilled with invented visits or results.

CREATE TABLE CARE_POINTS (
    care_point_id NUMBER(19,0) NOT NULL,
    care_point_code VARCHAR2(30 CHAR) NOT NULL,
    care_point_name VARCHAR2(100 CHAR) NOT NULL,
    care_point_kind VARCHAR2(20 CHAR) NOT NULL,
    care_point_department_id NUMBER(10,0) NOT NULL,
    care_point_active NUMBER(1,0) NOT NULL,
    CONSTRAINT pk_care_points PRIMARY KEY (care_point_id),
    CONSTRAINT uq_care_point_code UNIQUE (care_point_code),
    CONSTRAINT fk_care_point_department FOREIGN KEY (care_point_department_id) REFERENCES DEPARTMENTS(department_id),
    CONSTRAINT ck_care_point_kind CHECK (care_point_kind IN ('CLINIC','SERVICE','WARD')),
    CONSTRAINT ck_care_point_active CHECK (care_point_active IN (0,1))
);

CREATE SEQUENCE care_point_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE CARE_SERVICES (
    care_service_id NUMBER(19,0) NOT NULL,
    care_service_code VARCHAR2(30 CHAR) NOT NULL,
    care_service_name VARCHAR2(100 CHAR) NOT NULL,
    care_service_point_id NUMBER(19,0) NOT NULL,
    care_service_active NUMBER(1,0) NOT NULL,
    CONSTRAINT pk_care_services PRIMARY KEY (care_service_id),
    CONSTRAINT uq_care_service_code UNIQUE (care_service_code),
    CONSTRAINT fk_care_service_point FOREIGN KEY (care_service_point_id) REFERENCES CARE_POINTS(care_point_id),
    CONSTRAINT ck_care_service_active CHECK (care_service_active IN (0,1))
);

CREATE SEQUENCE care_service_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE CARE_ASSIGNMENTS (
    care_assignment_id NUMBER(19,0) NOT NULL,
    care_assignment_user_id NUMBER(10,0) NOT NULL,
    care_assignment_point_id NUMBER(19,0) NOT NULL,
    care_assignment_perform NUMBER(1,0) NOT NULL,
    care_assignment_verify NUMBER(1,0) NOT NULL,
    care_assignment_active NUMBER(1,0) NOT NULL,
    CONSTRAINT pk_care_assignments PRIMARY KEY (care_assignment_id),
    CONSTRAINT uq_care_assignment UNIQUE (care_assignment_user_id,care_assignment_point_id),
    CONSTRAINT fk_care_assignment_user FOREIGN KEY (care_assignment_user_id) REFERENCES APP_USERS(app_user_id),
    CONSTRAINT fk_care_assignment_point FOREIGN KEY (care_assignment_point_id) REFERENCES CARE_POINTS(care_point_id),
    CONSTRAINT ck_care_assignment_flags CHECK (care_assignment_perform IN (0,1) AND care_assignment_verify IN (0,1) AND care_assignment_active IN (0,1))
);

CREATE SEQUENCE care_assignment_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE CARE_VISITS (
    care_visit_id VARCHAR2(50 CHAR) NOT NULL,
    care_visit_patient_id VARCHAR2(50) NOT NULL,
    care_visit_status VARCHAR2(30 CHAR) NOT NULL,
    care_visit_previous_status NUMBER(2,0) NOT NULL,
    care_visit_original_point NUMBER(19,0) NOT NULL,
    care_visit_point_id NUMBER(19,0) NOT NULL,
    care_visit_location_point NUMBER(19,0),
    care_visit_owner_id NUMBER(10,0),
    care_visit_complaint VARCHAR2(500 CHAR) NOT NULL,
    care_visit_priority NUMBER(1,0) NOT NULL,
    care_visit_created_at TIMESTAMP(6) NOT NULL,
    care_visit_created_by NUMBER(10,0) NOT NULL,
    care_visit_started_at TIMESTAMP(6),
    care_visit_closed_at TIMESTAMP(6),
    care_visit_conclusion CLOB,
    care_visit_plan CLOB,
    care_visit_outcome VARCHAR2(30 CHAR),
    care_visit_followup_plan VARCHAR2(1000 CHAR),
    care_visit_followup_at TIMESTAMP(6),
    care_visit_followup_owner NUMBER(10,0),
    care_visit_admission_id VARCHAR2(50),
    care_visit_ward_point NUMBER(19,0),
    CONSTRAINT pk_care_visits PRIMARY KEY (care_visit_id),
    CONSTRAINT fk_care_visit_patient FOREIGN KEY (care_visit_patient_id) REFERENCES PATIENTS(patient_id) ON DELETE CASCADE,
    CONSTRAINT fk_care_visit_original_point FOREIGN KEY (care_visit_original_point) REFERENCES CARE_POINTS(care_point_id),
    CONSTRAINT fk_care_visit_point FOREIGN KEY (care_visit_point_id) REFERENCES CARE_POINTS(care_point_id),
    CONSTRAINT fk_care_visit_location FOREIGN KEY (care_visit_location_point) REFERENCES CARE_POINTS(care_point_id),
    CONSTRAINT fk_care_visit_owner FOREIGN KEY (care_visit_owner_id) REFERENCES APP_USERS(app_user_id),
    CONSTRAINT fk_care_visit_creator FOREIGN KEY (care_visit_created_by) REFERENCES APP_USERS(app_user_id),
    CONSTRAINT fk_care_visit_followup_owner FOREIGN KEY (care_visit_followup_owner) REFERENCES APP_USERS(app_user_id),
    CONSTRAINT fk_care_visit_admission FOREIGN KEY (care_visit_admission_id) REFERENCES ADMISSIONS(admission_id) ON DELETE SET NULL,
    CONSTRAINT fk_care_visit_ward FOREIGN KEY (care_visit_ward_point) REFERENCES CARE_POINTS(care_point_id),
    CONSTRAINT ck_care_visit_status CHECK (care_visit_status IN ('WAITING','ACTIVE','AWAITING_ADMISSION','INPATIENT','COMPLETED','CANCELLED')),
    CONSTRAINT ck_care_visit_closed CHECK ((care_visit_status IN ('COMPLETED','CANCELLED') AND care_visit_closed_at IS NOT NULL) OR (care_visit_status NOT IN ('COMPLETED','CANCELLED') AND care_visit_closed_at IS NULL)),
    CONSTRAINT ck_care_visit_priority CHECK (care_visit_priority BETWEEN 0 AND 2),
    CONSTRAINT ck_care_visit_previous CHECK (care_visit_previous_status BETWEEN 0 AND 3)
);

CREATE TABLE CARE_TASKS (
    care_task_id NUMBER(19,0) NOT NULL,
    care_task_visit_id VARCHAR2(50 CHAR) NOT NULL,
    care_task_point_id NUMBER(19,0) NOT NULL,
    care_task_kind VARCHAR2(20 CHAR) NOT NULL,
    care_task_service_id NUMBER(19,0),
    care_task_status VARCHAR2(30 CHAR) NOT NULL,
    care_task_arrival VARCHAR2(20 CHAR) NOT NULL,
    care_task_reason VARCHAR2(1000 CHAR),
    care_task_created_by NUMBER(10,0) NOT NULL,
    care_task_created_at TIMESTAMP(6) NOT NULL,
    care_task_arrived_at TIMESTAMP(6),
    care_task_started_at TIMESTAMP(6),
    care_task_performed_by NUMBER(10,0),
    care_task_closed_at TIMESTAMP(6),
    care_task_resolution VARCHAR2(1000 CHAR),
    care_task_resolved_by NUMBER(10,0),
    care_task_resolved_at TIMESTAMP(6),
    care_task_request_key VARCHAR2(80 CHAR),
    CONSTRAINT pk_care_tasks PRIMARY KEY (care_task_id),
    CONSTRAINT fk_care_task_visit FOREIGN KEY (care_task_visit_id) REFERENCES CARE_VISITS(care_visit_id) ON DELETE CASCADE,
    CONSTRAINT fk_care_task_point FOREIGN KEY (care_task_point_id) REFERENCES CARE_POINTS(care_point_id),
    CONSTRAINT fk_care_task_service FOREIGN KEY (care_task_service_id) REFERENCES CARE_SERVICES(care_service_id),
    CONSTRAINT fk_care_task_creator FOREIGN KEY (care_task_created_by) REFERENCES APP_USERS(app_user_id),
    CONSTRAINT fk_care_task_performer FOREIGN KEY (care_task_performed_by) REFERENCES APP_USERS(app_user_id),
    CONSTRAINT fk_care_task_resolver FOREIGN KEY (care_task_resolved_by) REFERENCES APP_USERS(app_user_id),
    CONSTRAINT uq_care_task_request UNIQUE (care_task_visit_id,care_task_request_key),
    CONSTRAINT ck_care_task_kind CHECK (care_task_kind IN ('EXAM','ORDER')),
    CONSTRAINT ck_care_task_service CHECK ((care_task_kind='ORDER' AND care_task_service_id IS NOT NULL) OR (care_task_kind='EXAM' AND care_task_service_id IS NULL)),
    CONSTRAINT ck_care_task_status CHECK (care_task_status IN ('QUEUED','IN_PROGRESS','WAITING_RESULTS','AWAITING_VALIDATION','RESULT_READY','COMPLETED','CANCEL_REQUESTED','CANCELLED','DECLINED')),
    CONSTRAINT ck_care_task_arrival CHECK (care_task_arrival IN ('EXPECTED','ARRIVED','CALLED','ABSENT','LEFT'))
);

CREATE SEQUENCE care_task_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE CARE_RESULTS (
    care_result_id NUMBER(19,0) NOT NULL,
    care_result_task_id NUMBER(19,0) NOT NULL,
    care_result_revision NUMBER(10,0) NOT NULL,
    care_result_body CLOB NOT NULL,
    care_result_critical NUMBER(1,0) NOT NULL,
    care_result_correction VARCHAR2(1000 CHAR),
    care_result_created_by NUMBER(10,0) NOT NULL,
    care_result_created_at TIMESTAMP(6) NOT NULL,
    care_result_published_by NUMBER(10,0),
    care_result_published_at TIMESTAMP(6),
    care_result_read_by NUMBER(10,0),
    care_result_read_at TIMESTAMP(6),
    CONSTRAINT pk_care_results PRIMARY KEY (care_result_id),
    CONSTRAINT fk_care_result_task FOREIGN KEY (care_result_task_id) REFERENCES CARE_TASKS(care_task_id) ON DELETE CASCADE,
    CONSTRAINT fk_care_result_creator FOREIGN KEY (care_result_created_by) REFERENCES APP_USERS(app_user_id),
    CONSTRAINT fk_care_result_publisher FOREIGN KEY (care_result_published_by) REFERENCES APP_USERS(app_user_id),
    CONSTRAINT fk_care_result_reader FOREIGN KEY (care_result_read_by) REFERENCES APP_USERS(app_user_id),
    CONSTRAINT uq_care_result_revision UNIQUE (care_result_task_id,care_result_revision),
    CONSTRAINT ck_care_result_revision CHECK (care_result_revision > 0),
    CONSTRAINT ck_care_result_critical CHECK (care_result_critical IN (0,1)),
    CONSTRAINT ck_care_result_published CHECK ((care_result_published_at IS NULL AND care_result_published_by IS NULL) OR (care_result_published_at IS NOT NULL AND care_result_published_by IS NOT NULL)),
    CONSTRAINT ck_care_result_read CHECK ((care_result_read_at IS NULL AND care_result_read_by IS NULL) OR (care_result_read_at IS NOT NULL AND care_result_read_by IS NOT NULL AND care_result_published_at IS NOT NULL))
);

CREATE SEQUENCE care_result_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE CARE_NOTES (
    care_note_id NUMBER(19,0) NOT NULL,
    care_note_visit_id VARCHAR2(50 CHAR) NOT NULL,
    care_note_kind VARCHAR2(20 CHAR) NOT NULL,
    care_note_body CLOB NOT NULL,
    care_note_created_by NUMBER(10,0) NOT NULL,
    care_note_created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_care_notes PRIMARY KEY (care_note_id),
    CONSTRAINT fk_care_note_visit FOREIGN KEY (care_note_visit_id) REFERENCES CARE_VISITS(care_visit_id) ON DELETE CASCADE,
    CONSTRAINT fk_care_note_creator FOREIGN KEY (care_note_created_by) REFERENCES APP_USERS(app_user_id),
    CONSTRAINT ck_care_note_kind CHECK (care_note_kind IN ('VITALS','EXAM','REVIEW','CONCLUSION','COORDINATION'))
);

CREATE SEQUENCE care_note_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE CARE_HANDOFFS (
    care_handoff_id NUMBER(19,0) NOT NULL,
    care_handoff_visit_id VARCHAR2(50 CHAR) NOT NULL,
    care_handoff_from_user NUMBER(10,0),
    care_handoff_to_user NUMBER(10,0) NOT NULL,
    care_handoff_point_id NUMBER(19,0) NOT NULL,
    care_handoff_kind VARCHAR2(20 CHAR) NOT NULL,
    care_handoff_status VARCHAR2(20 CHAR) NOT NULL,
    care_handoff_reason VARCHAR2(1000 CHAR) NOT NULL,
    care_handoff_response CLOB,
    care_handoff_created_by NUMBER(10,0) NOT NULL,
    care_handoff_created_at TIMESTAMP(6) NOT NULL,
    care_handoff_responded_at TIMESTAMP(6),
    CONSTRAINT pk_care_handoffs PRIMARY KEY (care_handoff_id),
    CONSTRAINT fk_care_handoff_visit FOREIGN KEY (care_handoff_visit_id) REFERENCES CARE_VISITS(care_visit_id) ON DELETE CASCADE,
    CONSTRAINT fk_care_handoff_from FOREIGN KEY (care_handoff_from_user) REFERENCES APP_USERS(app_user_id),
    CONSTRAINT fk_care_handoff_to FOREIGN KEY (care_handoff_to_user) REFERENCES APP_USERS(app_user_id),
    CONSTRAINT fk_care_handoff_creator FOREIGN KEY (care_handoff_created_by) REFERENCES APP_USERS(app_user_id),
    CONSTRAINT fk_care_handoff_point FOREIGN KEY (care_handoff_point_id) REFERENCES CARE_POINTS(care_point_id),
    CONSTRAINT ck_care_handoff_kind CHECK (care_handoff_kind IN ('TRANSFER','CONSULT')),
    CONSTRAINT ck_care_handoff_status CHECK (care_handoff_status IN ('PENDING','ACCEPTED','DECLINED'))
);

CREATE SEQUENCE care_handoff_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE UNIQUE INDEX ux_care_visit_active_patient ON CARE_VISITS
    (CASE WHEN care_visit_closed_at IS NULL THEN care_visit_patient_id END);
CREATE UNIQUE INDEX ux_care_task_active_exam ON CARE_TASKS
    (CASE WHEN care_task_kind='EXAM' AND care_task_closed_at IS NULL THEN care_task_visit_id END);
CREATE UNIQUE INDEX ux_care_result_draft ON CARE_RESULTS
    (CASE WHEN care_result_published_at IS NULL THEN care_result_task_id END);
CREATE UNIQUE INDEX ux_care_handoff_pending ON CARE_HANDOFFS
    (CASE WHEN care_handoff_kind='TRANSFER' AND care_handoff_status='PENDING' THEN care_handoff_visit_id END);
CREATE INDEX ix_care_point_department ON CARE_POINTS(care_point_department_id);
CREATE INDEX ix_care_service_point ON CARE_SERVICES(care_service_point_id);
CREATE INDEX ix_care_assignment_point ON CARE_ASSIGNMENTS(care_assignment_point_id,care_assignment_active);
CREATE INDEX ix_care_visit_patient ON CARE_VISITS(care_visit_patient_id,care_visit_created_at);
CREATE INDEX ix_care_visit_owner ON CARE_VISITS(care_visit_owner_id,care_visit_closed_at);
CREATE INDEX ix_care_visit_point ON CARE_VISITS(care_visit_point_id,care_visit_closed_at);
CREATE INDEX ix_care_visit_admission ON CARE_VISITS(care_visit_admission_id);
CREATE INDEX ix_care_task_queue ON CARE_TASKS(care_task_point_id,care_task_closed_at,care_task_created_at);
CREATE INDEX ix_care_task_visit ON CARE_TASKS(care_task_visit_id,care_task_created_at);
CREATE INDEX ix_care_task_service ON CARE_TASKS(care_task_service_id);
CREATE INDEX ix_care_note_visit ON CARE_NOTES(care_note_visit_id,care_note_created_at);
CREATE INDEX ix_care_handoff_visit ON CARE_HANDOFFS(care_handoff_visit_id);
CREATE INDEX ix_care_handoff_recipient ON CARE_HANDOFFS(care_handoff_to_user,care_handoff_status);

ALTER TABLE APP_USERS ADD app_user_doctor_id NUMBER(10,0);
ALTER TABLE APP_USERS ADD CONSTRAINT fk_app_users_doctor FOREIGN KEY (app_user_doctor_id) REFERENCES DOCTORS(doctor_id);
ALTER TABLE APP_USERS ADD CONSTRAINT uq_app_users_doctor UNIQUE (app_user_doctor_id);
ALTER TABLE APP_USERS DROP CONSTRAINT ck_app_users_role;
ALTER TABLE APP_USERS ADD CONSTRAINT ck_app_users_role CHECK
    (app_user_role IN ('RECEPTIONIST','DOCTOR','NURSE','ADMIN','PATIENT','TECHNICIAN'));
ALTER TABLE ADMISSIONS ADD admission_inpatient_flag NUMBER(1,0) DEFAULT 0 NOT NULL;
ALTER TABLE ADMISSIONS ADD CONSTRAINT ck_admission_inpatient CHECK (admission_inpatient_flag IN (0,1));
ALTER TABLE DIAGNOSES ADD diagnose_visit_id VARCHAR2(50 CHAR);
ALTER TABLE DIAGNOSES ADD CONSTRAINT fk_diagnose_care_visit FOREIGN KEY (diagnose_visit_id) REFERENCES CARE_VISITS(care_visit_id) ON DELETE CASCADE;
CREATE INDEX ix_diagnose_care_visit ON DIAGNOSES(diagnose_visit_id);
ALTER TABLE PRESCRIPTIONS ADD prescription_visit_id VARCHAR2(50 CHAR);
ALTER TABLE PRESCRIPTIONS ADD CONSTRAINT fk_prescription_care_visit FOREIGN KEY (prescription_visit_id) REFERENCES CARE_VISITS(care_visit_id) ON DELETE CASCADE;
CREATE INDEX ix_prescription_care_visit ON PRESCRIPTIONS(prescription_visit_id);
ALTER TABLE AUDIT_LOGS DROP CONSTRAINT ck_audit_logs_entity_type;
ALTER TABLE AUDIT_LOGS ADD CONSTRAINT ck_audit_logs_entity_type CHECK
    (audit_log_entity_type IN ('PATIENT','ADMISSION','USER','CARE_VISIT','CARE_TASK','CARE_RESULT','CARE_ASSIGNMENT'));
