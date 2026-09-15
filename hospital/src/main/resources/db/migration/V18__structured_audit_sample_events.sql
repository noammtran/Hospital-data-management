-- Synthetic version 2 events for the local Activity history demonstration.
-- They are snapshots only and do not represent real staff or patient activity.

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_actor_display_name,
    audit_log_actor_role, audit_log_action, audit_log_entity_type, audit_log_entity_id,
    audit_log_target_label, audit_log_details, audit_log_event_version,
    audit_log_subject_type, audit_log_subject_id, audit_log_subject_label,
    audit_log_summary, audit_log_change_count, audit_log_target_count,
    audit_log_sample_flag
) VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(180, 'MINUTE'),
    'receptionist', 'Nguyễn Minh Anh', 'RECEPTIONIST', 'CREATE', 'PATIENT', 'P26UR901',
    'Đào Minh Khôi · P26UR901', 'Created patient Đào Minh Khôi · P26UR901', 2,
    NULL, NULL, NULL, 'Record created', 2, 0, 1
);

INSERT INTO AUDIT_LOG_CHANGES (
    audit_log_change_id, audit_log_id, audit_log_change_field_key,
    audit_log_change_value_type, audit_log_change_old_value, audit_log_change_new_value,
    audit_log_change_old_display, audit_log_change_new_display,
    audit_log_change_sensitivity, audit_log_change_sort_order
) VALUES (
    audit_log_change_seq.NEXTVAL, audit_log_seq.CURRVAL, 'name', 'TEXT',
    NULL, 'Đào Minh Khôi', NULL, 'Đào Minh Khôi', 'GENERAL', 0
);

INSERT INTO AUDIT_LOG_CHANGES (
    audit_log_change_id, audit_log_id, audit_log_change_field_key,
    audit_log_change_value_type, audit_log_change_old_value, audit_log_change_new_value,
    audit_log_change_old_display, audit_log_change_new_display,
    audit_log_change_sensitivity, audit_log_change_sort_order
) VALUES (
    audit_log_change_seq.NEXTVAL, audit_log_seq.CURRVAL, 'status', 'REFERENCE',
    NULL, '0', NULL, 'Waiting', 'GENERAL', 1
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_actor_display_name,
    audit_log_actor_role, audit_log_action, audit_log_entity_type, audit_log_entity_id,
    audit_log_target_label, audit_log_details, audit_log_event_version,
    audit_log_subject_type, audit_log_subject_id, audit_log_subject_label,
    audit_log_summary, audit_log_change_count, audit_log_target_count,
    audit_log_sample_flag
) VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(160, 'MINUTE'),
    'receptionist', 'Nguyễn Minh Anh', 'RECEPTIONIST', 'UPDATE', 'PATIENT', 'P26MB001',
    'Nguyễn Thị Anh · P26MB001', 'Updated patient Nguyễn Thị Anh · P26MB001', 2,
    NULL, NULL, NULL, '2 fields changed', 2, 0, 1
);

INSERT INTO AUDIT_LOG_CHANGES (
    audit_log_change_id, audit_log_id, audit_log_change_field_key,
    audit_log_change_value_type, audit_log_change_old_value, audit_log_change_new_value,
    audit_log_change_old_display, audit_log_change_new_display,
    audit_log_change_sensitivity, audit_log_change_sort_order
) VALUES (
    audit_log_change_seq.NEXTVAL, audit_log_seq.CURRVAL, 'status', 'REFERENCE',
    '0', '1', 'Waiting', 'Examining', 'GENERAL', 0
);

INSERT INTO AUDIT_LOG_CHANGES (
    audit_log_change_id, audit_log_id, audit_log_change_field_key,
    audit_log_change_value_type, audit_log_change_old_value, audit_log_change_new_value,
    audit_log_change_old_display, audit_log_change_new_display,
    audit_log_change_sensitivity, audit_log_change_sort_order
) VALUES (
    audit_log_change_seq.NEXTVAL, audit_log_seq.CURRVAL, 'phoneNo', 'TEXT',
    '[REDACTED]', '[REDACTED]', '[REDACTED]', '[REDACTED]', 'PERSONAL', 1
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_actor_display_name,
    audit_log_actor_role, audit_log_action, audit_log_entity_type, audit_log_entity_id,
    audit_log_target_label, audit_log_details, audit_log_event_version,
    audit_log_subject_type, audit_log_subject_id, audit_log_subject_label,
    audit_log_summary, audit_log_change_count, audit_log_target_count,
    audit_log_sample_flag
) VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(140, 'MINUTE'),
    'doctor', 'Trần Quốc Bảo', 'DOCTOR', 'CREATE', 'ADMISSION',
    'a9000000-0000-0000-0000-000000000901', 'Admission 2026-08-23 · a9000000',
    'Created admission for Lê Văn Tùng · P26MX023', 2,
    'PATIENT', 'P26MX023', 'Lê Văn Tùng · P26MX023',
    'Record created', 2, 0, 1
);

INSERT INTO AUDIT_LOG_CHANGES (
    audit_log_change_id, audit_log_id, audit_log_change_field_key,
    audit_log_change_value_type, audit_log_change_old_value, audit_log_change_new_value,
    audit_log_change_old_display, audit_log_change_new_display,
    audit_log_change_sensitivity, audit_log_change_sort_order
) VALUES (
    audit_log_change_seq.NEXTVAL, audit_log_seq.CURRVAL, 'departmentId', 'REFERENCE',
    NULL, '1', NULL, 'Nội khoa', 'GENERAL', 0
);

INSERT INTO AUDIT_LOG_CHANGES (
    audit_log_change_id, audit_log_id, audit_log_change_field_key,
    audit_log_change_value_type, audit_log_change_old_value, audit_log_change_new_value,
    audit_log_change_old_display, audit_log_change_new_display,
    audit_log_change_sensitivity, audit_log_change_sort_order
) VALUES (
    audit_log_change_seq.NEXTVAL, audit_log_seq.CURRVAL, 'admissionDate', 'DATE',
    NULL, '2026-08-23', NULL, '2026-08-23', 'GENERAL', 1
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_actor_display_name,
    audit_log_actor_role, audit_log_action, audit_log_entity_type, audit_log_entity_id,
    audit_log_target_label, audit_log_details, audit_log_event_version,
    audit_log_subject_type, audit_log_subject_id, audit_log_subject_label,
    audit_log_summary, audit_log_change_count, audit_log_target_count,
    audit_log_sample_flag
) VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(120, 'MINUTE'),
    'doctor', 'Trần Quốc Bảo', 'DOCTOR', 'UPDATE', 'ADMISSION',
    'a9000000-0000-0000-0000-000000000901', 'Admission 2026-08-23 · a9000000',
    'Updated admission for Lê Văn Tùng · P26MX023', 2,
    'PATIENT', 'P26MX023', 'Lê Văn Tùng · P26MX023',
    '1 field changed', 1, 0, 1
);

INSERT INTO AUDIT_LOG_CHANGES (
    audit_log_change_id, audit_log_id, audit_log_change_field_key,
    audit_log_change_value_type, audit_log_change_old_value, audit_log_change_new_value,
    audit_log_change_old_display, audit_log_change_new_display,
    audit_log_change_sensitivity, audit_log_change_sort_order
) VALUES (
    audit_log_change_seq.NEXTVAL, audit_log_seq.CURRVAL, 'roomId', 'REFERENCE',
    '12', '18', 'P-601', 'P-1001', 'GENERAL', 0
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_actor_display_name,
    audit_log_actor_role, audit_log_action, audit_log_entity_type, audit_log_entity_id,
    audit_log_target_label, audit_log_details, audit_log_event_version,
    audit_log_subject_type, audit_log_subject_id, audit_log_subject_label,
    audit_log_summary, audit_log_change_count, audit_log_target_count,
    audit_log_sample_flag
) VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(100, 'MINUTE'),
    'admin', 'Phạm Đức Long', 'ADMIN', 'DELETE', 'ADMISSION',
    'a9000000-0000-0000-0000-000000000903', 'Admission 2026-08-21 · a9000000',
    'Deleted admission for Vũ Minh Châu · P26UT903', 2,
    'PATIENT', 'P26UT903', 'Vũ Minh Châu · P26UT903',
    'Permanently deleted', 2, 0, 1
);

INSERT INTO AUDIT_LOG_CHANGES (
    audit_log_change_id, audit_log_id, audit_log_change_field_key,
    audit_log_change_value_type, audit_log_change_old_value, audit_log_change_new_value,
    audit_log_change_old_display, audit_log_change_new_display,
    audit_log_change_sensitivity, audit_log_change_sort_order
) VALUES (
    audit_log_change_seq.NEXTVAL, audit_log_seq.CURRVAL, 'departmentId', 'REFERENCE',
    '2', NULL, 'Ngoại khoa', NULL, 'GENERAL', 0
);

INSERT INTO AUDIT_LOG_CHANGES (
    audit_log_change_id, audit_log_id, audit_log_change_field_key,
    audit_log_change_value_type, audit_log_change_old_value, audit_log_change_new_value,
    audit_log_change_old_display, audit_log_change_new_display,
    audit_log_change_sensitivity, audit_log_change_sort_order
) VALUES (
    audit_log_change_seq.NEXTVAL, audit_log_seq.CURRVAL, 'notes', 'TEXT',
    '[REDACTED]', NULL, '[REDACTED]', NULL, 'MEDICAL', 1
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_actor_display_name,
    audit_log_actor_role, audit_log_action, audit_log_entity_type, audit_log_entity_id,
    audit_log_target_label, audit_log_details, audit_log_event_version,
    audit_log_subject_type, audit_log_subject_id, audit_log_subject_label,
    audit_log_summary, audit_log_change_count, audit_log_target_count,
    audit_log_sample_flag
) VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(80, 'MINUTE'),
    'admin', 'Phạm Đức Long', 'ADMIN', 'BULK_DELETE', 'PATIENT', NULL,
    '2 patients', 'Deleted 2 patients', 2,
    NULL, NULL, NULL, '2 records deleted', 0, 2, 1
);

INSERT INTO AUDIT_LOG_TARGETS (
    audit_log_target_id, audit_log_id, audit_log_target_entity_type,
    audit_log_target_entity_id, audit_log_target_label, audit_log_target_sort_order
) VALUES (
    audit_log_target_seq.NEXTVAL, audit_log_seq.CURRVAL, 'PATIENT',
    'P26UU904', 'Nguyễn Thu Lam · P26UU904', 0
);

INSERT INTO AUDIT_LOG_TARGETS (
    audit_log_target_id, audit_log_id, audit_log_target_entity_type,
    audit_log_target_entity_id, audit_log_target_label, audit_log_target_sort_order
) VALUES (
    audit_log_target_seq.NEXTVAL, audit_log_seq.CURRVAL, 'PATIENT',
    'P26UV905', 'Trần Minh Sơn · P26UV905', 1
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_actor_display_name,
    audit_log_actor_role, audit_log_action, audit_log_entity_type, audit_log_entity_id,
    audit_log_target_label, audit_log_details, audit_log_event_version,
    audit_log_subject_type, audit_log_subject_id, audit_log_subject_label,
    audit_log_summary, audit_log_change_count, audit_log_target_count,
    audit_log_sample_flag
) VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(65, 'MINUTE'),
    'admin', 'Phạm Đức Long', 'ADMIN', 'CREATE', 'USER', '105',
    'Hoàng Minh Tú · @lab.coordinator', 'Created staff account Hoàng Minh Tú · @lab.coordinator', 2,
    NULL, NULL, NULL, 'Record created', 2, 0, 1
);

INSERT INTO AUDIT_LOG_CHANGES (
    audit_log_change_id, audit_log_id, audit_log_change_field_key,
    audit_log_change_value_type, audit_log_change_old_value, audit_log_change_new_value,
    audit_log_change_old_display, audit_log_change_new_display,
    audit_log_change_sensitivity, audit_log_change_sort_order
) VALUES (
    audit_log_change_seq.NEXTVAL, audit_log_seq.CURRVAL, 'username', 'TEXT',
    NULL, 'lab.coordinator', NULL, 'lab.coordinator', 'GENERAL', 0
);

INSERT INTO AUDIT_LOG_CHANGES (
    audit_log_change_id, audit_log_id, audit_log_change_field_key,
    audit_log_change_value_type, audit_log_change_old_value, audit_log_change_new_value,
    audit_log_change_old_display, audit_log_change_new_display,
    audit_log_change_sensitivity, audit_log_change_sort_order
) VALUES (
    audit_log_change_seq.NEXTVAL, audit_log_seq.CURRVAL, 'email', 'TEXT',
    NULL, '[REDACTED]', NULL, '[REDACTED]', 'PERSONAL', 1
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_actor_display_name,
    audit_log_actor_role, audit_log_action, audit_log_entity_type, audit_log_entity_id,
    audit_log_target_label, audit_log_details, audit_log_event_version,
    audit_log_subject_type, audit_log_subject_id, audit_log_subject_label,
    audit_log_summary, audit_log_change_count, audit_log_target_count,
    audit_log_sample_flag
) VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(50, 'MINUTE'),
    'admin', 'Phạm Đức Long', 'ADMIN', 'UPDATE', 'USER', '105',
    'Hoàng Minh Tú · @lab.coordinator', 'Updated staff account Hoàng Minh Tú · @lab.coordinator', 2,
    NULL, NULL, NULL, '1 field changed', 1, 0, 1
);

INSERT INTO AUDIT_LOG_CHANGES (
    audit_log_change_id, audit_log_id, audit_log_change_field_key,
    audit_log_change_value_type, audit_log_change_old_value, audit_log_change_new_value,
    audit_log_change_old_display, audit_log_change_new_display,
    audit_log_change_sensitivity, audit_log_change_sort_order
) VALUES (
    audit_log_change_seq.NEXTVAL, audit_log_seq.CURRVAL, 'role', 'REFERENCE',
    'NURSE', 'DOCTOR', 'Nurse', 'Doctor', 'GENERAL', 0
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_actor_display_name,
    audit_log_actor_role, audit_log_action, audit_log_entity_type, audit_log_entity_id,
    audit_log_target_label, audit_log_details, audit_log_event_version,
    audit_log_subject_type, audit_log_subject_id, audit_log_subject_label,
    audit_log_summary, audit_log_change_count, audit_log_target_count,
    audit_log_sample_flag
) VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(30, 'MINUTE'),
    'admin', 'Phạm Đức Long', 'ADMIN', 'STATUS_UPDATE', 'USER', '3',
    'Lê Thu Hà · @nurse', 'Deactivated staff account Lê Thu Hà · @nurse', 2,
    NULL, NULL, NULL, 'Active to Locked', 1, 0, 1
);

INSERT INTO AUDIT_LOG_CHANGES (
    audit_log_change_id, audit_log_id, audit_log_change_field_key,
    audit_log_change_value_type, audit_log_change_old_value, audit_log_change_new_value,
    audit_log_change_old_display, audit_log_change_new_display,
    audit_log_change_sensitivity, audit_log_change_sort_order
) VALUES (
    audit_log_change_seq.NEXTVAL, audit_log_seq.CURRVAL, 'active', 'BOOLEAN',
    'true', 'false', 'Active', 'Locked', 'GENERAL', 0
);

INSERT INTO AUDIT_LOGS (
    audit_log_id, audit_log_occurred_at, audit_log_actor, audit_log_actor_display_name,
    audit_log_actor_role, audit_log_action, audit_log_entity_type, audit_log_entity_id,
    audit_log_target_label, audit_log_details, audit_log_event_version,
    audit_log_subject_type, audit_log_subject_id, audit_log_subject_label,
    audit_log_summary, audit_log_change_count, audit_log_target_count,
    audit_log_sample_flag
) VALUES (
    audit_log_seq.NEXTVAL, LOCALTIMESTAMP - NUMTODSINTERVAL(10, 'MINUTE'),
    'admin', 'Phạm Đức Long', 'ADMIN', 'PASSWORD_RESET', 'USER', '2',
    'Trần Quốc Bảo · @doctor',
    'Credentials reset and existing sessions revoked for Trần Quốc Bảo · @doctor', 2,
    NULL, NULL, NULL,
    'Credentials reset and existing sessions revoked', 0, 0, 1
);
