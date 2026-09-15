-- Synthetic events for the local Activity history demonstration.
-- Relative timestamps keep the examples recent whenever a fresh schema is created.

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_action,
    audit_log_entity_type, audit_log_entity_id, audit_log_target_label, audit_log_details
)
VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(2880, 'MINUTE'),
    'admin', 'CREATE', 'USER', '1', 'receptionist', 'Synthetic demo event'
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_action,
    audit_log_entity_type, audit_log_entity_id, audit_log_target_label, audit_log_details
)
VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(1440, 'MINUTE'),
    'receptionist', 'CREATE', 'PATIENT', 'P26MX023', 'P26MX023',
    'Synthetic demo event'
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_action,
    audit_log_entity_type, audit_log_entity_id, audit_log_target_label, audit_log_details
)
VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(720, 'MINUTE'),
    'receptionist', 'UPDATE', 'PATIENT', 'P26MX023', 'P26MX023',
    'Synthetic demo event'
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_action,
    audit_log_entity_type, audit_log_entity_id, audit_log_target_label, audit_log_details
)
VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(480, 'MINUTE'),
    'doctor', 'CREATE', 'ADMISSION',
    'a00000000-0000-0000-0000-000000000023', 'P26MX023', 'Synthetic demo event'
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_action,
    audit_log_entity_type, audit_log_entity_id, audit_log_target_label, audit_log_details
)
VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(390, 'MINUTE'),
    'doctor', 'UPDATE', 'ADMISSION',
    'a00000000-0000-0000-0000-000000000023', 'P26MX023', 'Synthetic demo event'
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_action,
    audit_log_entity_type, audit_log_entity_id, audit_log_target_label, audit_log_details
)
VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(300, 'MINUTE'),
    'admin', 'STATUS_UPDATE', 'USER', '1', 'receptionist',
    'Synthetic demo event, active=true'
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_action,
    audit_log_entity_type, audit_log_entity_id, audit_log_target_label, audit_log_details
)
VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(240, 'MINUTE'),
    'admin', 'UPDATE', 'USER', '2', 'doctor', 'Synthetic demo event'
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_action,
    audit_log_entity_type, audit_log_entity_id, audit_log_target_label, audit_log_details
)
VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(190, 'MINUTE'),
    'admin', 'BULK_DELETE', 'PATIENT', NULL, '3 patients',
    'Synthetic demo event, count=3, ids=P26US902,P26UT903,P26UU904'
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_action,
    audit_log_entity_type, audit_log_entity_id, audit_log_target_label, audit_log_details
)
VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(165, 'MINUTE'),
    'admin', 'DELETE', 'ADMISSION',
    'a90000000-0000-0000-0000-000000000901', 'P26UR901', 'Synthetic demo event'
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_action,
    audit_log_entity_type, audit_log_entity_id, audit_log_target_label, audit_log_details
)
VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(145, 'MINUTE'),
    'admin', 'DELETE', 'PATIENT', 'P26UR901', 'P26UR901', 'Synthetic demo event'
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_action,
    audit_log_entity_type, audit_log_entity_id, audit_log_target_label, audit_log_details
)
VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(110, 'MINUTE'),
    'receptionist', 'CREATE', 'PATIENT', 'P26ND029', 'P26ND029',
    'Synthetic demo event'
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_action,
    audit_log_entity_type, audit_log_entity_id, audit_log_target_label, audit_log_details
)
VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(80, 'MINUTE'),
    'receptionist', 'UPDATE', 'PATIENT', 'P26ND029', 'P26ND029',
    'Synthetic demo event'
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_action,
    audit_log_entity_type, audit_log_entity_id, audit_log_target_label, audit_log_details
)
VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(55, 'MINUTE'),
    'doctor', 'CREATE', 'ADMISSION',
    'a00000000-0000-0000-0000-000000000029', 'P26ND029', 'Synthetic demo event'
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_action,
    audit_log_entity_type, audit_log_entity_id, audit_log_target_label, audit_log_details
)
VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(35, 'MINUTE'),
    'doctor', 'UPDATE', 'ADMISSION',
    'a00000000-0000-0000-0000-000000000021', 'P26MV021', 'Synthetic demo event'
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_action,
    audit_log_entity_type, audit_log_entity_id, audit_log_target_label, audit_log_details
)
VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(18, 'MINUTE'),
    'admin', 'STATUS_UPDATE', 'USER', '3', 'nurse',
    'Synthetic demo event, active=true'
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_action,
    audit_log_entity_type, audit_log_entity_id, audit_log_target_label, audit_log_details
)
VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(5, 'MINUTE'),
    'admin', 'PASSWORD_RESET', 'USER', '2', 'doctor', 'Synthetic demo event'
);
