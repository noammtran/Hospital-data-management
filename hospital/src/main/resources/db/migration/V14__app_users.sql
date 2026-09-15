CREATE TABLE APP_USERS (
    app_user_id                 NUMBER(10,0)   NOT NULL,
    app_user_username           VARCHAR2(30)   NOT NULL,
    app_user_email              VARCHAR2(100)  NOT NULL,
    app_user_password_hash      VARCHAR2(100)  NOT NULL,
    app_user_token_version      NUMBER(10,0)   DEFAULT 0 NOT NULL,
    app_user_full_name          VARCHAR2(100)  NOT NULL,
    app_user_role               VARCHAR2(20)   NOT NULL,
    app_user_active_flag        NUMBER(1,0)    DEFAULT 1 NOT NULL,
    app_user_created_by         VARCHAR2(30)   NOT NULL,
    app_user_created_date       TIMESTAMP(6)   DEFAULT SYSTIMESTAMP NOT NULL,
    app_user_last_updated_by    VARCHAR2(30)   NOT NULL,
    app_user_last_updated_date  TIMESTAMP(6)   DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_app_users PRIMARY KEY (app_user_id),
    CONSTRAINT ck_app_users_role CHECK (
        app_user_role IN ('RECEPTIONIST', 'DOCTOR', 'NURSE', 'ADMIN')
    ),
    CONSTRAINT ck_app_users_active CHECK (app_user_active_flag IN (0, 1)),
    CONSTRAINT ck_app_users_token_version CHECK (app_user_token_version >= 0)
);

CREATE UNIQUE INDEX ux_app_users_username_ci ON APP_USERS (LOWER(app_user_username));
CREATE UNIQUE INDEX ux_app_users_email_ci ON APP_USERS (LOWER(app_user_email));
CREATE INDEX ix_app_users_role_active ON APP_USERS (app_user_role, app_user_active_flag);

CREATE SEQUENCE app_user_seq START WITH 100 INCREMENT BY 1 NOCACHE NOCYCLE;

INSERT ALL
    INTO APP_USERS (
        app_user_id, app_user_username, app_user_email, app_user_password_hash,
        app_user_full_name, app_user_role, app_user_active_flag,
        app_user_created_by, app_user_created_date,
        app_user_last_updated_by, app_user_last_updated_date
    ) VALUES (
        1, 'receptionist', 'receptionist@hospital.example',
        '$2a$12$8POofkGJcfRg5wX55GxBau2FhcYuUxlIne51y01/0r9t4nR/1OEIe',
        'Nguyễn Minh Anh', 'RECEPTIONIST', 1,
        'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP
    )
    INTO APP_USERS (
        app_user_id, app_user_username, app_user_email, app_user_password_hash,
        app_user_full_name, app_user_role, app_user_active_flag,
        app_user_created_by, app_user_created_date,
        app_user_last_updated_by, app_user_last_updated_date
    ) VALUES (
        2, 'doctor', 'doctor@hospital.example',
        '$2a$12$OmfVrLVxBcQaQjBrutrTfu4eF/QITuoc/X5HyXFFMhGD3eXQNh8b2',
        'Trần Quốc Bảo', 'DOCTOR', 1,
        'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP
    )
    INTO APP_USERS (
        app_user_id, app_user_username, app_user_email, app_user_password_hash,
        app_user_full_name, app_user_role, app_user_active_flag,
        app_user_created_by, app_user_created_date,
        app_user_last_updated_by, app_user_last_updated_date
    ) VALUES (
        3, 'nurse', 'nurse@hospital.example',
        '$2a$12$JiJK76mmvbjDQtc8YwQMyOzBBKzUS1YPvHkJ8vAN.jZG2SeWfsZnG',
        'Lê Thu Hà', 'NURSE', 1,
        'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP
    )
    INTO APP_USERS (
        app_user_id, app_user_username, app_user_email, app_user_password_hash,
        app_user_full_name, app_user_role, app_user_active_flag,
        app_user_created_by, app_user_created_date,
        app_user_last_updated_by, app_user_last_updated_date
    ) VALUES (
        4, 'admin', 'admin@hospital.example',
        '$2a$12$KNpbFcnJFnXFhopKgKyvEuunPNDQezEHg1dFit3h5ahxR.V/EiPbC',
        'Phạm Đức Long', 'ADMIN', 1,
        'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP
    )
SELECT 1 FROM DUAL;
