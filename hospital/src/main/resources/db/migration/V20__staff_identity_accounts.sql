ALTER TABLE APP_USERS ADD (
    app_user_identity_no VARCHAR2(12)
);

CREATE UNIQUE INDEX ux_app_users_identity_no ON APP_USERS (app_user_identity_no);

ALTER TABLE APP_USERS ADD CONSTRAINT ck_app_users_identity_no CHECK (
    app_user_identity_no IS NULL
    OR REGEXP_LIKE(app_user_identity_no, '^[0-9]{12}$')
);

ALTER TABLE APP_USERS ADD CONSTRAINT ck_app_users_staff_identity CHECK (
    (app_user_role = 'PATIENT' AND app_user_identity_no IS NULL)
    OR (app_user_role <> 'PATIENT' AND (
        app_user_identity_no IS NULL
        OR app_user_username = app_user_identity_no
    ))
);

-- Backfill only the four immutable shipped demonstration accounts. Existing
-- user-created staff accounts remain readable and receive an identity number
-- the next time an Administrator edits them.
UPDATE APP_USERS
SET app_user_identity_no = '001201300001',
    app_user_username = '001201300001',
    app_user_password_hash = '$2a$12$3q.x9PaygB2aQ4tc2g22M.9k3VBIXqKxsYRMQibXOH1H6DYJhZrui',
    app_user_password_change_required = 1,
    app_user_token_version = app_user_token_version + 1,
    app_user_last_updated_by = 'SYSTEM',
    app_user_last_updated_date = SYSTIMESTAMP
WHERE app_user_id = 1 AND app_user_username = 'receptionist';

UPDATE APP_USERS
SET app_user_identity_no = '001201100001',
    app_user_username = '001201100001',
    app_user_password_hash = '$2a$12$A33Z5RxoXMYJOlXVtjsOC.rC8UaJ3P.rOsgJX5/z2s9loLrkn3Xj6',
    app_user_password_change_required = 1,
    app_user_token_version = app_user_token_version + 1,
    app_user_last_updated_by = 'SYSTEM',
    app_user_last_updated_date = SYSTIMESTAMP
WHERE app_user_id = 2 AND app_user_username = 'doctor';

UPDATE APP_USERS
SET app_user_identity_no = '001201200001',
    app_user_username = '001201200001',
    app_user_password_hash = '$2a$12$ln5isuXjOkBbDUme5h7v5e9J/JK0b/CzypIulHbbOGpW5nXvEaoHK',
    app_user_password_change_required = 1,
    app_user_token_version = app_user_token_version + 1,
    app_user_last_updated_by = 'SYSTEM',
    app_user_last_updated_date = SYSTIMESTAMP
WHERE app_user_id = 3 AND app_user_username = 'nurse';

UPDATE APP_USERS
SET app_user_identity_no = '001201900001',
    app_user_username = '001201900001',
    app_user_password_hash = '$2a$12$VKNZF/gEsaLHnOi5NgZnTuWxs8PaHHuaAsgb1ze5TvX7GfwZJg0L2',
    app_user_password_change_required = 1,
    app_user_token_version = app_user_token_version + 1,
    app_user_last_updated_by = 'SYSTEM',
    app_user_last_updated_date = SYSTIMESTAMP
WHERE app_user_id = 4 AND app_user_username = 'admin';

-- Each existing fictional doctor receives an individual login account. The
-- identity numbers are synthetic and are used only for local demonstration.
INSERT INTO APP_USERS (
    app_user_id, app_user_username, app_user_email, app_user_password_hash,
    app_user_identity_no, app_user_password_change_required,
    app_user_full_name, app_user_role, app_user_active_flag,
    app_user_created_by, app_user_created_date,
    app_user_last_updated_by, app_user_last_updated_date
)
SELECT app_user_seq.NEXTVAL, '001201100002', doctor_email,
       '$2a$12$2PqnAFJOqgQxwJGcOmqhIOAp8CHakwwJrUBNPt7O7K2a4hgjiECa.',
       '001201100002', 1, doctor_name, 'DOCTOR', doctor_status_flag,
       'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP
FROM DOCTORS
WHERE doctor_id = 1
  AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('an.nguyen@hospital.vn'));

INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100003', doctor_email, '$2a$12$b0dc1C8TZksd0R2iLbKZ/umHcRdT16Jaf/6hXwWJVjL5xzRyQaGjG', '001201100003', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 2 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('binh.tran@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100004', doctor_email, '$2a$12$Yxyf36szpYfewrdV2UjWduDgnYihUBgd4m3DoPPzG/mEz.MQABECO', '001201100004', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 3 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('cuong.le@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100005', doctor_email, '$2a$12$AaCK.mUpiLgp2.YW9n9k1OSSACTlSUsDblSdAscVwomA7wpebmkUW', '001201100005', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 4 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('dung.pham@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100006', doctor_email, '$2a$12$xIffHgjOSwSmyfrSFP7ej.ssMwOgHSOVMR2.sQTu8UI7CJcEAvjLy', '001201100006', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 5 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('em.hoang@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100007', doctor_email, '$2a$12$wtsLa6WFmQiHWk0xKZusxekn2HBocZqStDJJzbJ/dzeG9LgTQcgs2', '001201100007', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 6 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('phuong.vu@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100008', doctor_email, '$2a$12$r04o6N4ll8XX70rl9zVGMOtz.HOV6kXGm9VPv6I.1Wi4bC6Bp1hCS', '001201100008', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 7 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('giang.dang@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100009', doctor_email, '$2a$12$/YQWYMYQxn4pwjhFrBnmp.zU.5qk5an4vAvi4qVvCY2zRo1ebrGjG', '001201100009', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 8 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('huong.bui@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100010', doctor_email, '$2a$12$vH8cC6NWouQfuVh22ziU2.xTZZ6TQrpRk3LMaevTkKAN3DcKpsJSu', '001201100010', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 9 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('khoa.do@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100011', doctor_email, '$2a$12$2ZUTi67DQ7g0p0BUzbJpMelCzX4hGBz.Jb4ObR6s73akyu2AUDtbq', '001201100011', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 10 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('lan.ngo@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100012', doctor_email, '$2a$12$yhIGekjLDueM.oohdLH3f.foZGHhAY8qFwwzjdKt7eTlDq4891fPC', '001201100012', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 11 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('minh.ly@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100013', doctor_email, '$2a$12$yvOxL/Qyvktd2hkf3aRI1u5SUT0pY3VzCWvHeCSVbBiC5PhhPW9Ye', '001201100013', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 12 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('nga.truong@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100014', doctor_email, '$2a$12$EnKRTDzlbAVs5kreXsnL6O8.iEmr5MMvPDPibzdacl9.DjWWJW5NO', '001201100014', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 13 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('oanh.phan@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100015', doctor_email, '$2a$12$5dzi4J05hqW/Uz75yNvkwuWdQyWUVVFrlkFBRnyy1Rt84xqIOD15G', '001201100015', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 14 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('phuc.trinh@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100016', doctor_email, '$2a$12$egWGB7D6VSH5EJhKfncSleX/mmVAeZBJlXnazKULFasEY8WqCKHb2', '001201100016', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 15 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('quang.duong@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100017', doctor_email, '$2a$12$osx06c8tVAwqDoajYUr4UOvKtgxTlWOueUKFQUpyzQK1WtBJbh8TK', '001201100017', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 16 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('rang.mai@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100018', doctor_email, '$2a$12$/4qAkWoTK4MWn3kTsiJ2OuZwxgktKYiZv0Lh7Qf16qf/qU.BPSYE6', '001201100018', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 17 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('son.nguyen@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100019', doctor_email, '$2a$12$ZMdwQEg5Jw8VKofwcIkHheooG7F2BuUjUFMGloHzKMqMbH51EkDd2', '001201100019', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 18 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('tram.le@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100020', doctor_email, '$2a$12$ID3ry/CbjjImT4k.3ouOL.8vTeynt3LYgL1dVL4Rn8CRZX8deNO8C', '001201100020', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 19 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('uy.pham@hospital.vn'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201100021', doctor_email, '$2a$12$P/BJ/.JxvjCHJWlCJRwdbu1CJkD4b.JAcUCryoXHqwHsCUYzWhoDi', '001201100021', 1, doctor_name, 'DOCTOR', doctor_status_flag, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DOCTORS WHERE doctor_id = 20 AND NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('van.hoang@hospital.vn'));

-- Additional fictional nurses and receptionists complete the staff-account
-- demonstration without introducing real personal data.
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201200002', 'mai.nguyen.nurse@hospital.example', '$2a$12$dJRtAzvWQ/gPQHOmJCwJFeG.0y5Cz4ogZRWjEkwyVGE2YOn3cZ2ua', '001201200002', 1, 'Nguyễn Ngọc Mai', 'NURSE', 1, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('mai.nguyen.nurse@hospital.example'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201200003', 'anh.tran.nurse@hospital.example', '$2a$12$guLW2CdP79QX5v0F6S2q1OtUoTWpz17vtpnfrt57N64eLBj2WqkQO', '001201200003', 1, 'Trần Quỳnh Anh', 'NURSE', 1, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('anh.tran.nurse@hospital.example'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201200004', 'chau.pham.nurse@hospital.example', '$2a$12$A8HJTyUuSMc9ldk2kxaK/e.VMC3sX5UpA.hTnSg3dE8bSAzS7AcWa', '001201200004', 1, 'Phạm Minh Châu', 'NURSE', 1, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('chau.pham.nurse@hospital.example'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201200005', 'tam.vo.nurse@hospital.example', '$2a$12$CEHHrQbpXCJfby0b30dQB.q3hJ3HfwCvE0GA8b/.F/wbZ2VPvrW3G', '001201200005', 1, 'Võ Thanh Tâm', 'NURSE', 1, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('tam.vo.nurse@hospital.example'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201300002', 'linh.do.reception@hospital.example', '$2a$12$ay0Sj7L.jAad/fyUmBPR2OMUxRJiKtdoXSsSAQA4Jeh0JavOmmFym', '001201300002', 1, 'Đỗ Thùy Linh', 'RECEPTIONIST', 1, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('linh.do.reception@hospital.example'));
INSERT INTO APP_USERS (app_user_id, app_user_username, app_user_email, app_user_password_hash, app_user_identity_no, app_user_password_change_required, app_user_full_name, app_user_role, app_user_active_flag, app_user_created_by, app_user_created_date, app_user_last_updated_by, app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL, '001201300003', 'han.bui.reception@hospital.example', '$2a$12$SItoC4cmoHoqPO2nICy4i.01qAGmI8m82aoR.16BwC9FBa6VGUDFK', '001201300003', 1, 'Bùi Gia Hân', 'RECEPTIONIST', 1, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM APP_USERS WHERE LOWER(app_user_email) = LOWER('han.bui.reception@hospital.example'));
