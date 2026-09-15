-- Fictional training configuration only. No patient visits/results are fabricated.
-- Individual doctor accounts retain their identity-based temporary credential policy.
UPDATE APP_USERS u SET app_user_doctor_id = (
 SELECT d.doctor_id FROM DOCTORS d WHERE u.app_user_username = '0012011' || LPAD(d.doctor_id + 1,5,'0')
 AND u.app_user_email=d.doctor_email)
WHERE u.app_user_role='DOCTOR' AND EXISTS (
 SELECT 1 FROM DOCTORS d WHERE u.app_user_username = '0012011' || LPAD(d.doctor_id + 1,5,'0')
 AND u.app_user_email=d.doctor_email);

INSERT INTO CARE_POINTS (care_point_id,care_point_code,care_point_name,care_point_kind,care_point_department_id,care_point_active)
SELECT care_point_seq.NEXTVAL,'CLINIC_' || department_code,'Phòng khám ' || department_name,'CLINIC',department_id,1 FROM DEPARTMENTS;
INSERT INTO CARE_POINTS (care_point_id,care_point_code,care_point_name,care_point_kind,care_point_department_id,care_point_active)
SELECT care_point_seq.NEXTVAL,'WARD_' || department_code,'Điều trị ' || department_name,'WARD',department_id,1 FROM DEPARTMENTS;

INSERT INTO DEPARTMENTS (department_id,department_code,department_name,department_description)
VALUES (department_seq.NEXTVAL,'XN','Xét nghiệm','Danh mục cấu hình thực hành; không chứa dữ liệu người bệnh');
INSERT INTO DEPARTMENTS (department_id,department_code,department_name,department_description)
VALUES (department_seq.NEXTVAL,'CDHA','Chẩn đoán hình ảnh','Danh mục cấu hình thực hành; không chứa dữ liệu người bệnh');
INSERT INTO CARE_POINTS (care_point_id,care_point_code,care_point_name,care_point_kind,care_point_department_id,care_point_active)
SELECT care_point_seq.NEXTVAL,'LAB','Phòng xét nghiệm','SERVICE',department_id,1 FROM DEPARTMENTS WHERE department_code='XN';
INSERT INTO CARE_POINTS (care_point_id,care_point_code,care_point_name,care_point_kind,care_point_department_id,care_point_active)
SELECT care_point_seq.NEXTVAL,'XRAY','Phòng X-quang','SERVICE',department_id,1 FROM DEPARTMENTS WHERE department_code='CDHA';
INSERT INTO CARE_POINTS (care_point_id,care_point_code,care_point_name,care_point_kind,care_point_department_id,care_point_active)
SELECT care_point_seq.NEXTVAL,'ULTRASOUND','Phòng siêu âm','SERVICE',department_id,1 FROM DEPARTMENTS WHERE department_code='CDHA';
INSERT INTO CARE_SERVICES (care_service_id,care_service_code,care_service_name,care_service_point_id,care_service_active) SELECT care_service_seq.NEXTVAL,'LAB_CBC','Tổng phân tích tế bào máu',care_point_id,1 FROM CARE_POINTS WHERE care_point_code='LAB';
INSERT INTO CARE_SERVICES (care_service_id,care_service_code,care_service_name,care_service_point_id,care_service_active) SELECT care_service_seq.NEXTVAL,'LAB_BIOCHEM','Xét nghiệm sinh hóa theo chỉ định',care_point_id,1 FROM CARE_POINTS WHERE care_point_code='LAB';
INSERT INTO CARE_SERVICES (care_service_id,care_service_code,care_service_name,care_service_point_id,care_service_active) SELECT care_service_seq.NEXTVAL,'XRAY_REQUEST','Chụp X-quang theo chỉ định',care_point_id,1 FROM CARE_POINTS WHERE care_point_code='XRAY';
INSERT INTO CARE_SERVICES (care_service_id,care_service_code,care_service_name,care_service_point_id,care_service_active) SELECT care_service_seq.NEXTVAL,'US_REQUEST','Siêu âm theo chỉ định',care_point_id,1 FROM CARE_POINTS WHERE care_point_code='ULTRASOUND';

INSERT INTO CARE_ASSIGNMENTS (care_assignment_id,care_assignment_user_id,care_assignment_point_id,care_assignment_perform,care_assignment_verify,care_assignment_active)
SELECT care_assignment_seq.NEXTVAL,u.app_user_id,p.care_point_id,1,0,1 FROM APP_USERS u
JOIN DOCTORS d ON d.doctor_id=u.app_user_doctor_id JOIN CARE_POINTS p ON p.care_point_department_id=d.doctor_department_id
WHERE p.care_point_kind IN ('CLINIC','WARD') AND u.app_user_role='DOCTOR' AND u.app_user_active_flag=1;

INSERT INTO CARE_ASSIGNMENTS (care_assignment_id,care_assignment_user_id,care_assignment_point_id,care_assignment_perform,care_assignment_verify,care_assignment_active)
SELECT care_assignment_seq.NEXTVAL,u.app_user_id,p.care_point_id,0,0,1 FROM APP_USERS u JOIN CARE_POINTS p
ON p.care_point_department_id=CASE SUBSTR(u.app_user_username,-1) WHEN '1' THEN 1 WHEN '2' THEN 2 WHEN '3' THEN 5 WHEN '4' THEN 15 ELSE 1 END
WHERE u.app_user_role='NURSE' AND u.app_user_username LIKE '00120120000%' AND p.care_point_kind IN ('CLINIC','WARD');

INSERT INTO APP_USERS (app_user_id,app_user_username,app_user_identity_no,app_user_email,app_user_password_hash,
 app_user_full_name,app_user_role,app_user_active_flag,app_user_password_change_required,app_user_token_version,
 app_user_created_by,app_user_created_date,app_user_last_updated_by,app_user_last_updated_date)
VALUES (app_user_seq.NEXTVAL,'001201400001','001201400001','care.technician1@example.invalid','$2b$10$4IzeU3PBnRtjhKiMVVgx6ep3ynESAZIxuzW31Qd7d/8dBVmpNvuZC',
 'Kỹ thuật viên mẫu Xét nghiệm','TECHNICIAN',1,1,0,'system',SYSTIMESTAMP,'system',SYSTIMESTAMP);
INSERT INTO CARE_ASSIGNMENTS (care_assignment_id,care_assignment_user_id,care_assignment_point_id,care_assignment_perform,care_assignment_verify,care_assignment_active)
SELECT care_assignment_seq.NEXTVAL,u.app_user_id,p.care_point_id,1,1,1 FROM APP_USERS u CROSS JOIN CARE_POINTS p
WHERE u.app_user_username='001201400001' AND p.care_point_code='LAB';

INSERT INTO APP_USERS (app_user_id,app_user_username,app_user_identity_no,app_user_email,app_user_password_hash,
 app_user_full_name,app_user_role,app_user_active_flag,app_user_password_change_required,app_user_token_version,
 app_user_created_by,app_user_created_date,app_user_last_updated_by,app_user_last_updated_date)
VALUES (app_user_seq.NEXTVAL,'001201400002','001201400002','care.technician2@example.invalid','$2b$10$W6ObAqFCQ80y9MFFw/6bUe.lqXqpBd0YcbzTtlxwlhHP34FtzMqoG',
 'Kỹ thuật viên mẫu Hình ảnh','TECHNICIAN',1,1,0,'system',SYSTIMESTAMP,'system',SYSTIMESTAMP);
INSERT INTO CARE_ASSIGNMENTS (care_assignment_id,care_assignment_user_id,care_assignment_point_id,care_assignment_perform,care_assignment_verify,care_assignment_active)
SELECT care_assignment_seq.NEXTVAL,u.app_user_id,p.care_point_id,1,0,1 FROM APP_USERS u CROSS JOIN CARE_POINTS p
WHERE u.app_user_username='001201400002' AND p.care_point_code='XRAY';
INSERT INTO CARE_ASSIGNMENTS (care_assignment_id,care_assignment_user_id,care_assignment_point_id,care_assignment_perform,care_assignment_verify,care_assignment_active)
SELECT care_assignment_seq.NEXTVAL,u.app_user_id,p.care_point_id,1,0,1 FROM APP_USERS u CROSS JOIN CARE_POINTS p
WHERE u.app_user_username='001201400002' AND p.care_point_code='ULTRASOUND';

-- Image interpretation is assigned to a linked doctor account.
INSERT INTO DOCTORS (doctor_id,doctor_code,doctor_name,doctor_specialization,doctor_email,doctor_department_id,doctor_status_flag)
SELECT doctor_seq.NEXTVAL,'DEMO_CDHA','Bác sĩ mẫu Chẩn đoán hình ảnh','Chẩn đoán hình ảnh','care.radiology@example.invalid',department_id,1
FROM DEPARTMENTS WHERE department_code='CDHA';
INSERT INTO APP_USERS (app_user_id,app_user_username,app_user_identity_no,app_user_email,app_user_password_hash,app_user_doctor_id,
 app_user_full_name,app_user_role,app_user_active_flag,app_user_password_change_required,app_user_token_version,
 app_user_created_by,app_user_created_date,app_user_last_updated_by,app_user_last_updated_date)
SELECT app_user_seq.NEXTVAL,'001201100022','001201100022','care.radiology@example.invalid','$2b$10$kABYQM7YnlZBQxCTnTMcOuvBBUmG4E3HwP6nqLr8oJh/WMYWgLVly',doctor_id,
 'Bác sĩ mẫu Chẩn đoán hình ảnh','DOCTOR',1,1,0,'system',SYSTIMESTAMP,'system',SYSTIMESTAMP FROM DOCTORS WHERE doctor_code='DEMO_CDHA';
INSERT INTO CARE_ASSIGNMENTS (care_assignment_id,care_assignment_user_id,care_assignment_point_id,care_assignment_perform,care_assignment_verify,care_assignment_active)
SELECT care_assignment_seq.NEXTVAL,u.app_user_id,p.care_point_id,1,1,1 FROM APP_USERS u CROSS JOIN CARE_POINTS p
WHERE u.app_user_username='001201100022' AND p.care_point_code IN ('XRAY','ULTRASOUND');
