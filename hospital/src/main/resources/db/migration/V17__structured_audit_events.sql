ALTER TABLE AUDIT_LOGS ADD (
    audit_log_event_version       NUMBER(3,0)    DEFAULT 1 NOT NULL,
    audit_log_sample_flag         NUMBER(1,0)    DEFAULT 0 NOT NULL,
    audit_log_actor_display_name  VARCHAR2(100),
    audit_log_actor_role          VARCHAR2(20),
    audit_log_subject_type        VARCHAR2(30),
    audit_log_subject_id          VARCHAR2(100),
    audit_log_subject_label       VARCHAR2(200),
    audit_log_summary             VARCHAR2(1000),
    audit_log_change_count        NUMBER(10,0)   DEFAULT 0 NOT NULL,
    audit_log_target_count        NUMBER(10,0)   DEFAULT 0 NOT NULL,
    CONSTRAINT ck_audit_event_version CHECK (audit_log_event_version >= 1),
    CONSTRAINT ck_audit_sample_flag CHECK (audit_log_sample_flag IN (0, 1)),
    CONSTRAINT ck_audit_subject_type CHECK (
        audit_log_subject_type IS NULL
        OR audit_log_subject_type IN ('PATIENT', 'ADMISSION', 'USER')
    ),
    CONSTRAINT ck_audit_change_count CHECK (audit_log_change_count >= 0),
    CONSTRAINT ck_audit_target_count CHECK (audit_log_target_count >= 0)
);

UPDATE AUDIT_LOGS
SET audit_log_sample_flag = 1
WHERE audit_log_details LIKE 'Synthetic demo event%';

CREATE TABLE AUDIT_LOG_CHANGES (
    audit_log_change_id           NUMBER(19,0)   NOT NULL,
    audit_log_id                  NUMBER(19,0)   NOT NULL,
    audit_log_change_field_key    VARCHAR2(80)   NOT NULL,
    audit_log_change_value_type   VARCHAR2(20)   NOT NULL,
    audit_log_change_old_value    VARCHAR2(2000),
    audit_log_change_new_value    VARCHAR2(2000),
    audit_log_change_old_display  VARCHAR2(500),
    audit_log_change_new_display  VARCHAR2(500),
    audit_log_change_sensitivity  VARCHAR2(20)   DEFAULT 'GENERAL' NOT NULL,
    audit_log_change_sort_order   NUMBER(5,0)    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_audit_log_changes PRIMARY KEY (audit_log_change_id),
    CONSTRAINT fk_audit_change_log FOREIGN KEY (audit_log_id)
        REFERENCES AUDIT_LOGS (audit_log_id) ON DELETE CASCADE,
    CONSTRAINT ck_audit_change_value_type CHECK (
        audit_log_change_value_type IN ('TEXT', 'DATE', 'NUMBER', 'BOOLEAN', 'REFERENCE')
    ),
    CONSTRAINT ck_audit_change_sensitivity CHECK (
        audit_log_change_sensitivity IN ('GENERAL', 'PERSONAL', 'MEDICAL', 'SECRET')
    ),
    CONSTRAINT ck_audit_change_sort_order CHECK (audit_log_change_sort_order >= 0)
);

CREATE SEQUENCE audit_log_change_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE INDEX ix_audit_changes_log_order
    ON AUDIT_LOG_CHANGES (audit_log_id, audit_log_change_sort_order, audit_log_change_id);

CREATE TABLE AUDIT_LOG_TARGETS (
    audit_log_target_id          NUMBER(19,0)   NOT NULL,
    audit_log_id                 NUMBER(19,0)   NOT NULL,
    audit_log_target_entity_type VARCHAR2(30)   NOT NULL,
    audit_log_target_entity_id   VARCHAR2(100),
    audit_log_target_label       VARCHAR2(200),
    audit_log_target_sort_order  NUMBER(5,0)    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_audit_log_targets PRIMARY KEY (audit_log_target_id),
    CONSTRAINT fk_audit_target_log FOREIGN KEY (audit_log_id)
        REFERENCES AUDIT_LOGS (audit_log_id) ON DELETE CASCADE,
    CONSTRAINT ck_audit_target_entity CHECK (
        audit_log_target_entity_type IN ('PATIENT', 'ADMISSION', 'USER')
    ),
    CONSTRAINT ck_audit_target_order CHECK (audit_log_target_sort_order >= 0)
);

CREATE SEQUENCE audit_log_target_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE INDEX ix_audit_targets_log_order
    ON AUDIT_LOG_TARGETS (audit_log_id, audit_log_target_sort_order, audit_log_target_id);

CREATE INDEX ix_audit_logs_resource
    ON AUDIT_LOGS (audit_log_entity_type, audit_log_entity_id, audit_log_occurred_at DESC);

CREATE INDEX ix_audit_logs_subject
    ON AUDIT_LOGS (audit_log_subject_type, audit_log_subject_id, audit_log_occurred_at DESC);
