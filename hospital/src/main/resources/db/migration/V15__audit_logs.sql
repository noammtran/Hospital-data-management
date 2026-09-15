CREATE TABLE AUDIT_LOGS (
    audit_log_id            NUMBER(19,0)   NOT NULL,
    audit_log_occurred_at   TIMESTAMP(6)   DEFAULT SYSTIMESTAMP NOT NULL,
    audit_log_actor         VARCHAR2(30)   NOT NULL,
    audit_log_action        VARCHAR2(30)   NOT NULL,
    audit_log_entity_type   VARCHAR2(30)   NOT NULL,
    audit_log_entity_id     VARCHAR2(100),
    audit_log_target_label  VARCHAR2(200),
    audit_log_details       VARCHAR2(1000),
    CONSTRAINT pk_audit_logs PRIMARY KEY (audit_log_id),
    CONSTRAINT ck_audit_logs_action CHECK (
        audit_log_action IN (
            'CREATE', 'UPDATE', 'DELETE', 'BULK_DELETE',
            'STATUS_UPDATE', 'PASSWORD_RESET'
        )
    ),
    CONSTRAINT ck_audit_logs_entity_type CHECK (
        audit_log_entity_type IN ('PATIENT', 'ADMISSION', 'USER')
    )
);

CREATE SEQUENCE audit_log_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE INDEX ix_audit_logs_occurred_id
    ON AUDIT_LOGS (audit_log_occurred_at DESC, audit_log_id DESC);
