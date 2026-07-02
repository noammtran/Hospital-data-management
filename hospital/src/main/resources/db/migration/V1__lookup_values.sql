INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (1, 'Nam', 1, 'GENDER', 1, 'Giới tính nam');
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (2, 'Nữ', 2, 'GENDER', 1, 'Giới tính nữ');
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (3, 'Khác', 3, 'GENDER', 1, 'Giới tính khác');

INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (4, 'A+', 1, 'BLOOD_TYPE', 1, NULL);
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (5, 'A-', 2, 'BLOOD_TYPE', 1, NULL);
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (6, 'B+', 3, 'BLOOD_TYPE', 1, NULL);
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (7, 'B-', 4, 'BLOOD_TYPE', 1, NULL);
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (8, 'AB+', 5, 'BLOOD_TYPE', 1, NULL);
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (9, 'AB-', 6, 'BLOOD_TYPE', 1, NULL);
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (10, 'O+', 7, 'BLOOD_TYPE', 1, NULL);
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (11, 'O-', 8, 'BLOOD_TYPE', 1, NULL);

INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (12, 'Phòng thường', 1, 'ROOM_TYPE', 1, 'Phòng bệnh thường, nhiều giường');
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (13, 'Phòng VIP', 2, 'ROOM_TYPE', 1, 'Phòng dịch vụ, 1-2 giường');
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (14, 'Phòng ICU', 3, 'ROOM_TYPE', 1, 'Phòng hồi sức tích cực');
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (15, 'Phòng cấp cứu', 4, 'ROOM_TYPE', 1, 'Phòng cấp cứu ngắn hạn');

INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (16, 'Nhẹ', 1, 'SEVERITY', 1, NULL);
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (17, 'Trung bình', 2, 'SEVERITY', 1, NULL);
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (18, 'Nặng', 3, 'SEVERITY', 1, NULL);
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (19, 'Nguy kịch', 4, 'SEVERITY', 1, NULL);

INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (20, 'Viên nén', 1, 'MEDICINE_TYPE', 1, 'Tablet');
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (21, 'Viên nang', 2, 'MEDICINE_TYPE', 1, 'Capsule');
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (22, 'Siro', 3, 'MEDICINE_TYPE', 1, 'Syrup');
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (23, 'Tiêm', 4, 'MEDICINE_TYPE', 1, 'Injection');
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (24, 'Kem/Mỡ', 5, 'MEDICINE_TYPE', 1, 'Cream/Ointment');
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (25, 'Nhỏ', 6, 'MEDICINE_TYPE', 1, 'Drops (nhỏ mắt, nhỏ mũi)');

INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (26, 'Đang chờ', 0, 'PATIENT_STATUS', 1, 'Bệnh nhân đã đăng ký, chưa được khám');
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (27, 'Đang khám', 1, 'PATIENT_STATUS', 1, 'Đang khám ngoại trú');
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (28, 'Nhập viện', 2, 'PATIENT_STATUS', 1, 'Đã nhập viện, đang điều trị nội trú');
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (29, 'Xuất viện', 3, 'PATIENT_STATUS', 1, 'Đã xuất viện');

INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (30, 'Đã đặt lịch', 0, 'APPOINTMENT_STATUS', 1, NULL);
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (31, 'Hoàn thành', 1, 'APPOINTMENT_STATUS', 1, NULL);
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (32, 'Đã hủy', 2, 'APPOINTMENT_STATUS', 1, NULL);
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (33, 'Không đến', 3, 'APPOINTMENT_STATUS', 1, NULL);

INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (34, 'Cấp cứu', 1, 'ADMISSION_TYPE', 1, 'Emergency — nguy kịch, ưu tiên cao nhất');
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (35, 'Khẩn cấp', 2, 'ADMISSION_TYPE', 1, 'Urgent — cần điều trị sớm');
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (36, 'Tự nguyện', 3, 'ADMISSION_TYPE', 1, 'Elective — nhập viện theo kế hoạch');

INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (37, 'Đang điều trị', 1, 'ADMISSION_STATUS', 1, 'Đợt điều trị còn mở');
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (38, 'Đã xuất viện', 2, 'ADMISSION_STATUS', 1, 'Đợt điều trị kết thúc bình thường');
INSERT INTO LOOKUP_VALUE (id, lookup_value, lookup_cd, lookup_group, lookup_status_flag, lookup_description)
VALUES (39, 'Chuyển khoa', 3, 'ADMISSION_STATUS', 1, 'Đợt này đã đóng do chuyển sang khoa khác');

COMMIT;
