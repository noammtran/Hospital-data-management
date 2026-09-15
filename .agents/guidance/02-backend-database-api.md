# Backend, database, and API guidance

Read this guide before changing Java, Spring Boot, JPA, Oracle, Flyway,
validation, authentication, authorization, admissions, patient history,
business audit, medical-record queries, statistics, or REST contracts. It
replaces the backend structure, Oracle schema, API design, backend coding
convention, and database convention sections of the former `CLAUDE.md`.

The executable sources of truth remain:

- `hospital/src/main/java/`
- `hospital/src/main/resources/db/migration/`
- `hospital/src/test/`
- `hospital/pom.xml`

This guide is a current map for Codex. Reinspect those sources before editing.

## Active backend stack

- Java 21
- Spring Boot 3.5.14
- Spring Web MVC
- Spring Data JPA with Hibernate
- Spring Security with method-level authorization
- Spring Security OAuth2 Resource Server and Nimbus JOSE for JWT
- BCrypt password hashing
- Jakarta Validation
- Oracle JDBC
- Flyway Core plus Flyway Oracle support
- Maven Wrapper
- JUnit/Mockito through Spring Boot Test
- JaCoCo report generation without an enforced minimum threshold

The application is deliberately a small monolith. Do not introduce
microservices, WebFlux, cloud infrastructure, or a separate identity provider
unless the user explicitly expands the scope. Preserve the current stateless,
database-backed Spring Security and JWT design when extending authorization.

## Current package structure

```text
hospital/src/main/java/com/usth/hospital/
├── config/              security, JWT, CORS, and application configuration
├── controller/          REST controllers with permission checks
├── dto/
│   ├── request/         validated inbound records
│   └── response/        API response records
├── exception/           domain exceptions and global HTTP mapping
├── mapper/              entity/DTO conversion
├── model/               mapped patient, admission, account, and audit entities
├── repository/          JPA and native-query repositories
├── security/            roles, permissions, JWT conversion, and current user
├── service/             service contracts and PatientIdGenerator
└── service/impl/        transactional business logic
```

Do not create a separate historical planned `audit/` or `validation/` package
without a concrete need. Validation currently lives on request DTOs and in
service logic. Security infrastructure lives in `config/` and `security/`.
Field-level patient history lives in the patient service, while action-level
business audit is centralized in `AuditLogService`.

## Architecture and coding rules

- Keep the controller → service → repository flow.
- Controllers validate and translate HTTP input; business decisions belong in
  services.
- Return request/response DTOs rather than JPA entities.
- DTOs are Java records. Preserve existing field names because Angular models
  mirror them.
- Use constructor injection; the existing code uses Lombok
  `@RequiredArgsConstructor`.
- Put transaction boundaries on service methods.
- `spring.jpa.open-in-view=false`; map required data within a transaction or use
  explicit projections/queries.
- Prefer JPA for mapped aggregate write operations. Native Oracle queries are
  acceptable for medical-record, statistics, and reference-option read models
  when they provide a clear, tested projection that would be awkward or
  inefficient in JPQL.
- Avoid unbounded list APIs. Preserve one-based external pagination and
  zero-based Spring `Pageable` conversion.
- Preserve the response envelope and centralized exception mapping.
- Do not expose stack traces, SQL, credentials, or internal exception details to
  API clients.

## Flyway and schema rules

- Oracle and Flyway are the supported persistence path.
- `V0__schema.sql` defines the baseline physical schema.
- Applied migrations are immutable. Add a new ordered migration for an
  intentional schema or shipped-seed change.
- `hospital/src/main/resources/db/migration/` is the shipped source of truth.
  Files under `data/seed/` are staging/history and currently differ from the
  shipped migrations.
- Keep Oracle naming, data types, sequences, constraints, indexes, function-based
  indexes, and triggers compatible with the existing schema.
- Do not rely on Hibernate schema generation;
  `spring.jpa.hibernate.ddl-auto=validate`.
- Verify cascade behavior before changing a foreign key or permanent deletion.
- Do not add a hard foreign key to `LOOKUP_VALUE` codes without assessing all
  existing numeric-code columns and migrations; the current schema deliberately
  stores those codes without such FKs.

## Physical schema overview

The physical schema contains 16 tables through Flyway V17. Conceptual diagrams may intentionally
show smaller views. Do not infer physical deletion from a conceptual omission.

### `LOOKUP_VALUE`

Generic numeric code lists used for gender, blood type, patient/admission status,
room type, severity, medicine type, appointment status, and related groups.

| Column | Type | Constraint or meaning |
|---|---|---|
| `id` | `NUMBER(10,0)` | Primary key; `lookup_value_seq` exists |
| `lookup_value` | `VARCHAR2(255)` | Required display value |
| `lookup_cd` | `NUMBER(2,0)` | Required numeric code |
| `lookup_group` | `VARCHAR2(30)` | Required group name |
| `lookup_status_flag` | `NUMBER(1,0)` | Required, checked to 0 or 1 |
| `lookup_description` | `VARCHAR2(255)` | Optional explanation |

`(lookup_group, lookup_cd)` is unique and `lookup_group` is indexed. Business
tables currently store numeric codes without foreign keys to this table.

### `DEPARTMENTS`

| Column | Type | Constraint or meaning |
|---|---|---|
| `department_id` | `NUMBER(10,0)` | Primary key; `department_seq` exists |
| `department_code` | `VARCHAR2(20)` | Required and unique |
| `department_name` | `VARCHAR2(100)` | Required and unique |
| `department_description` | `VARCHAR2(255)` | Optional |

### `DOCTORS`

| Column | Type | Constraint or meaning |
|---|---|---|
| `doctor_id` | `NUMBER(10,0)` | Primary key; `doctor_seq` exists |
| `doctor_code` | `VARCHAR2(20)` | Required and unique |
| `doctor_name` | `VARCHAR2(100)` | Required; indexed |
| `doctor_specialization` | `VARCHAR2(100)` | Optional |
| `doctor_phone_no` | `VARCHAR2(20)` | Optional |
| `doctor_email` | `VARCHAR2(50)` | Optional |
| `doctor_department_id` | `NUMBER(10,0)` | Required FK to `DEPARTMENTS`; indexed |
| `doctor_status_flag` | `NUMBER(1,0)` | Required availability flag, 0 or 1 |

An inactive doctor remains resolvable in historical records but should not be
offered for a new clinical assignment.

### `ROOMS`

| Column | Type | Constraint or meaning |
|---|---|---|
| `room_id` | `NUMBER(10,0)` | Primary key; `room_seq` exists |
| `room_name` | `VARCHAR2(50)` | Required and unique |
| `room_type` | `NUMBER(2,0)` | Required lookup code |
| `department_id` | `NUMBER(10,0)` | Required FK to `DEPARTMENTS`; indexed |
| `bed_count` | `NUMBER(3,0)` | Required and greater than zero |
| `current_occupancy` | `NUMBER(3,0)` | Required, between zero and `bed_count` |
| `status_flag` | `NUMBER(1,0)` | Required room-availability state, 0 or 1 |

The shipped room seed uses `status_flag = 0` for full rooms and `1` for rooms
with capacity. No trigger automatically recomputes the flag. Do not describe it
as the same soft-delete mechanism used by doctors or medicines.

### `MEDICINES`

| Column | Type | Constraint or meaning |
|---|---|---|
| `medicine_id` | `NUMBER(10,0)` | Primary key; `medicine_seq` exists |
| `medicine_code` | `VARCHAR2(20)` | Required and unique |
| `medicine_name` | `VARCHAR2(200)` | Required and unique |
| `medicine_generic_name` | `VARCHAR2(200)` | Optional |
| `medicine_type` | `NUMBER(2,0)` | Required lookup code |
| `medicine_unit` | `VARCHAR2(20)` | Required |
| `medicine_manufacturer` | `VARCHAR2(200)` | Optional |
| `medicine_description` | `VARCHAR2(500)` | Optional |
| `medicine_side_effects` | `VARCHAR2(500)` | Optional |
| `medicine_status_flag` | `NUMBER(1,0)` | Required availability flag, 0 or 1 |

### `ICD10_CODES`

| Column | Type | Constraint or meaning |
|---|---|---|
| `id` | `NUMBER(10,0)` | Primary key; `icd10_seq` exists |
| `icd_code` | `VARCHAR2(10)` | Required and unique |
| `short_desc` | `VARCHAR2(100)` | Required and indexed |
| `long_desc` | `VARCHAR2(255)` | Optional |
| `hipaa_covered` | `NUMBER(1,0)` | Required, checked to 0 or 1 |
| `status_flag` | `NUMBER(1,0)` | Required availability flag, 0 or 1 |

`DIAGNOSES.diagnose_icd_code` stores a text code but currently has no physical
foreign key to `ICD10_CODES`.

### `PATIENTS`

| Column | Type | Constraint or meaning |
|---|---|---|
| `patient_id` | `VARCHAR2(50)` | Primary key; format `PYYLLNNN` |
| `patient_name` | `VARCHAR2(100)` | Required |
| `patient_birth_date` | `DATE` | Optional; age is derived |
| `patient_gender` | `NUMBER(2,0)` | Optional lookup code, currently 1–3 in DTOs |
| `patient_address` | `VARCHAR2(255)` | Optional |
| `patient_phone_no` | `VARCHAR2(20)` | Optional; indexed |
| `patient_email` | `VARCHAR2(50)` | Optional; not unique |
| `patient_identity_no` | `VARCHAR2(20)` | Required as exactly 12 digits for new records; legacy rows may be null; indexed and uniquely constrained by V13 |
| `patient_blood_type` | `NUMBER(2,0)` | Optional lookup code, currently 1–8 in DTOs |
| `patient_allergies` | `VARCHAR2(500)` | Optional free text |
| `patient_medical_history` | `VARCHAR2(1000)` | Optional free text |
| `patient_insurance_no` | `VARCHAR2(20)` | Optional |
| `patient_insurance_expiry` | `DATE` | Optional |
| `patient_emergency_contact_name` | `VARCHAR2(100)` | Optional |
| `patient_emergency_contact_phone` | `VARCHAR2(20)` | Optional |
| `patient_emergency_contact_relation` | `VARCHAR2(50)` | Optional |
| `patient_status` | `NUMBER(2,0)` | Required, checked from 0 through 3; indexed |
| `patient_note` | `VARCHAR2(1000)` | Optional |
| `patient_created_by` | `VARCHAR2(30)` | Required; current authenticated username for new writes |
| `patient_created_date` | `DATE` | Required |
| `patient_last_updated_by` | `VARCHAR2(30)` | Required; current authenticated username for writes |
| `patient_last_updated_date` | `DATE` | Required |

`V12__patient_audit_trigger.sql` supplies/updates date columns as a database
safety net. It does not create field-history records.

### `ADMISSIONS`

| Column | Type | Constraint or meaning |
|---|---|---|
| `admission_id` | `VARCHAR2(50)` | Primary key |
| `admission_patient_id` | `VARCHAR2(50)` | Required FK to `PATIENTS`, cascade on patient deletion |
| `admission_department_id` | `NUMBER(10,0)` | Required FK to `DEPARTMENTS` |
| `admission_doctor_id` | `NUMBER(10,0)` | Required FK to `DOCTORS`; indexed |
| `admission_room_id` | `NUMBER(10,0)` | Optional FK to `ROOMS` |
| `admission_date` | `DATE` | Required and indexed |
| `admission_discharge_date` | `DATE` | Null while the episode is active |
| `admission_reason` | `VARCHAR2(500)` | Optional |
| `admission_type` | `NUMBER(2,0)` | Optional lookup code |
| `admission_status` | `NUMBER(2,0)` | Required lookup code; no DB range check |
| `admission_notes` | `VARCHAR2(500)` | Optional |

A function-based unique index permits at most one admission with a null
discharge date per patient. The admission service locks the patient, validates
reference choices, and updates room occupancy during create, edit, discharge,
delete, and patient deletion. Completing an episode writes today's discharge
date, changes admission status to 2, releases its room when present, and changes
the patient to Discharged in one transaction. Direct completion cannot bypass
an open care visit. Accepted doctor handoff is implemented in the care workflow;
inter-ward transfer is not implemented.

### `DIAGNOSES`

| Column | Type | Constraint or meaning |
|---|---|---|
| `diagnose_id` | `VARCHAR2(50)` | Primary key |
| `diagnose_patient_id` | `VARCHAR2(50)` | Required FK to `PATIENTS`, cascade on patient deletion |
| `diagnose_admission_id` | `VARCHAR2(50)` | Optional FK to `ADMISSIONS`, cascade on admission deletion |
| `diagnose_doctor_id` | `NUMBER(10,0)` | Required FK to `DOCTORS` |
| `diagnose_date` | `DATE` | Required |
| `diagnose_icd_code` | `VARCHAR2(10)` | Optional text ICD code; indexed, no FK |
| `diagnose_desc` | `VARCHAR2(500)` | Required |
| `diagnose_severity` | `NUMBER(2,0)` | Optional lookup code |
| `diagnose_notes` | `VARCHAR2(500)` | Optional |

### `PRESCRIPTIONS`

| Column | Type | Constraint or meaning |
|---|---|---|
| `prescription_id` | `VARCHAR2(50)` | Primary key |
| `prescription_patient_id` | `VARCHAR2(50)` | Required FK to `PATIENTS`, cascade on patient deletion |
| `prescription_admission_id` | `VARCHAR2(50)` | Optional FK to `ADMISSIONS`, cascade on admission deletion |
| `prescription_doctor_id` | `NUMBER(10,0)` | Required FK to `DOCTORS` |
| `prescription_diagnose_id` | `VARCHAR2(50)` | Required FK to `DIAGNOSES`, cascade on diagnosis deletion |
| `prescription_medicine_id` | `NUMBER(10,0)` | Required FK to `MEDICINES`; indexed |
| `prescription_date` | `DATE` | Required |
| `prescription_dosage` | `VARCHAR2(100)` | Required |
| `prescription_frequency` | `VARCHAR2(100)` | Required |
| `prescription_duration` | `VARCHAR2(50)` | Required |
| `prescription_instructions` | `VARCHAR2(500)` | Optional |

### `APPOINTMENTS`

| Column | Type | Constraint or meaning |
|---|---|---|
| `appointment_id` | `VARCHAR2(50)` | Primary key |
| `appointment_patient_id` | `VARCHAR2(50)` | Required FK to `PATIENTS`, cascade on patient deletion |
| `appointment_doctor_id` | `NUMBER(10,0)` | Required FK to `DOCTORS` |
| `appointment_department_id` | `NUMBER(10,0)` | Required FK to `DEPARTMENTS` |
| `appointment_date` | `DATE` | Required and indexed |
| `appointment_time` | `VARCHAR2(10)` | Optional |
| `appointment_reason` | `VARCHAR2(500)` | Optional |
| `appointment_status` | `NUMBER(2,0)` | Required and checked from 0 through 3 |
| `appointment_notes` | `VARCHAR2(500)` | Optional |

The table and seed data exist, but there is currently no appointment controller,
service, or Angular feature.

### `PATIENT_HISTORY`

| Column | Type | Constraint or meaning |
|---|---|---|
| `patient_history_id` | `VARCHAR2(50)` | Primary key; application currently uses UUID text |
| `patient_history_patient_id` | `VARCHAR2(50)` | Required FK to `PATIENTS`, cascade on deletion; indexed |
| `patient_history_field_name` | `VARCHAR2(50)` | Required changed-field name |
| `patient_history_old_value` | `VARCHAR2(255)` | Optional prior value |
| `patient_history_new_value` | `VARCHAR2(255)` | Optional new value |
| `patient_history_changed_by` | `VARCHAR2(30)` | Required authenticated username; older seed data may use `SYSTEM` |
| `patient_history_changed_date` | `DATE` | Required and indexed |

This table is strictly per-patient field history. The service inserts rows only
for fields whose values differ during a patient update. Patient create, delete,
bulk delete, and a successful update with no changed tracked field do not create
`PATIENT_HISTORY` rows.

### `APP_USERS`

| Column | Type | Constraint or meaning |
|---|---|---|
| `app_user_id` | `NUMBER(10,0)` | Primary key; `app_user_seq` exists |
| `app_user_username` | `VARCHAR2(30)` | Required; case-insensitive unique index |
| `app_user_email` | `VARCHAR2(100)` | Required for staff, null for Patient accounts; case-insensitive unique index |
| `app_user_password_hash` | `VARCHAR2(100)` | Required BCrypt hash; never expose or audit |
| `app_user_identity_no` | `VARCHAR2(12)` | Unique synthetic or staff-provided identity number; required by staff create/update APIs and used as username and initial/reset password input |
| `app_user_token_version` | `NUMBER(10,0)` | Required nonnegative token-revocation version |
| `app_user_full_name` | `VARCHAR2(100)` | Required display name |
| `app_user_role` | `VARCHAR2(20)` | Required `RECEPTIONIST`, `DOCTOR`, `NURSE`, `ADMIN`, or `PATIENT` |
| `app_user_patient_id` | `VARCHAR2(50)` | Null for staff; required and unique for Patient accounts; FK to `PATIENTS` with cascade delete |
| `app_user_password_change_required` | `NUMBER(1,0)` | Required flag; a generated Patient account has only password-change permission while set |
| `app_user_active_flag` | `NUMBER(1,0)` | Required and checked to 0 or 1 |
| `app_user_created_by` | `VARCHAR2(30)` | Required actor username |
| `app_user_created_date` | `TIMESTAMP(6)` | Required creation time |
| `app_user_last_updated_by` | `VARCHAR2(30)` | Required last actor username |
| `app_user_last_updated_date` | `TIMESTAMP(6)` | Required last update time |

`V14__app_users.sql` creates the account store, indexes, sequence, and four
synthetic staff accounts. Account status and token version are checked from the
database when a JWT is converted, so deactivation and password reset invalidate
previously issued access tokens.

`V19__patient_self_service_accounts.sql` extends the store with the Patient role,
the unique patient foreign key, and the forced-password-change flag. It leaves
existing patients unchanged rather than seeding identity numbers or passwords.
Runtime patient create, identity update, and permanent delete operations manage
the linked account transactionally.

`V20__staff_identity_accounts.sql` adds the staff identity column, changes the
four original demonstration usernames and temporary credentials to synthetic
12-digit values, and creates individual fictional accounts for all seeded
doctors plus the demonstration nurses and receptionists. Legacy runtime staff
rows may remain null until an Administrator edits them; reset is rejected until
a valid identity number has been assigned.

`V21__patient_demo_account.sql` merges one active Patient demonstration account
for fictional patient `P26ML011`. Its identity number `001068011234` is the
username and one-time password input, only the BCrypt hash is stored, and the
forced-change flag is set. The linked V8 record has exactly three admissions.
The migration intentionally resets a pre-existing linked demo account so the
documented local credential remains reproducible and revokes its older tokens.

### `AUDIT_LOGS`

| Column | Type | Constraint or meaning |
|---|---|---|
| `audit_log_id` | `NUMBER(19,0)` | Primary key; `audit_log_seq` exists |
| `audit_log_occurred_at` | `TIMESTAMP(6)` | Required event time |
| `audit_log_actor` | `VARCHAR2(30)` | Required authenticated username |
| `audit_log_action` | `VARCHAR2(30)` | Required constrained business action |
| `audit_log_entity_type` | `VARCHAR2(30)` | Required constrained entity type |
| `audit_log_entity_id` | `VARCHAR2(100)` | Optional stable target identifier |
| `audit_log_target_label` | `VARCHAR2(200)` | Optional readable target label |
| `audit_log_details` | `VARCHAR2(1000)` | Optional non-secret event summary |
| `audit_log_event_version` | `NUMBER(3,0)` | Required event format version, default 1 |
| `audit_log_sample_flag` | `NUMBER(1,0)` | Required provenance flag, 1 sample and 0 operational, default 0 |
| `audit_log_actor_display_name` | `VARCHAR2(100)` | Optional actor display-name snapshot |
| `audit_log_actor_role` | `VARCHAR2(20)` | Optional actor-role snapshot |
| `audit_log_subject_type` | `VARCHAR2(30)` | Optional business-subject type |
| `audit_log_subject_id` | `VARCHAR2(100)` | Optional business-subject identifier |
| `audit_log_subject_label` | `VARCHAR2(200)` | Optional business-subject label |
| `audit_log_summary` | `VARCHAR2(1000)` | Optional concise change summary that does not repeat the event or target |
| `audit_log_change_count` | `NUMBER(10,0)` | Required non-negative child-change count |
| `audit_log_target_count` | `NUMBER(10,0)` | Required non-negative affected-target count |

The newest-first index covers event time and ID in descending order. Resource
and subject indexes support later targeted review. The header deliberately has
no foreign key to an account or domain row so actor and target snapshots remain
readable after permanent deletion.

### `AUDIT_LOG_CHANGES` and `AUDIT_LOG_TARGETS`

`AUDIT_LOG_CHANGES` stores ordered field differences for one event. Each row
records a field key, a value type, optional raw and display values for both
sides, a sensitivity classification, and a display order. The allowed value
types are `TEXT`, `DATE`, `NUMBER`, `BOOLEAN`, and `REFERENCE`. The allowed
sensitivity values are `GENERAL`, `PERSONAL`, `MEDICAL`, and `SECRET`.

`AUDIT_LOG_TARGETS` stores ordered entity type, identifier, and label snapshots
for multi-record operations such as patient bulk deletion. Both child tables
reference only `AUDIT_LOGS` and use cascading deletion. Their ordered indexes
use event ID, display order, and child ID.

`AuditLogService` replaces classified personal and medical change values with
`[REDACTED]` and omits secret changes before persistence. It retains general
values and readable reference display values. Resource and subject snapshot
labels also remain readable. Never pass a password, hash, token, or credential into
an event command.

`V16__audit_log_sample_events.sql` adds sixteen synthetic action-level events
for local demonstration. The timestamps are relative to migration time so they
appear as recent examples after a fresh installation. The rows use the shipped
test-account usernames, cover every delivered audit action and entity type, and
contain no password, token, hash, or real personal data. They are demonstration
data rather than evidence of actions performed in a real hospital. Every row
uses a details prefix that identifies it as a synthetic demonstration event.

`V17__structured_audit_events.sql` adds version 2 format support, sample
provenance, and the ordered change and affected-target tables. Existing V15 and
V16 rows retain version 1 and are returned with `legacy` set to true. V17 marks
the synthetic V16 rows as samples from their stable demonstration marker while
leaving genuine legacy and runtime rows operational. No historical child value
is reconstructed or guessed.

`V18__structured_audit_sample_events.sql` adds ten fictional version 2 examples.
The sample covers patient, admission, and staff-account actions. It demonstrates
typed changes, redacted values, admission patient-subject context, bulk-delete
targets, actor snapshots, and a password-reset summary with no password data.
Every V18 header sets the sample flag. Runtime event recording sets it to false.
Representative summary values are `2 fields changed`, `Permanently deleted`,
`2 records deleted`, `Active to Locked`, and `Credentials reset and existing
sessions revoked`.

## Migration inventory

| Migration | Purpose |
|---|---|
| `V0__schema.sql` | Tables, constraints, indexes, and sequences |
| `V1__lookup_values.sql` | Lookup code seed |
| `V2__departments.sql` | Department seed |
| `V3__medicines.sql` | Medicine seed |
| `V4__icd10_codes.sql` | ICD-10 seed |
| `V5__doctors.sql` | Synthetic doctor seed |
| `V6__rooms.sql` | Room/capacity seed |
| `V7__patients.sql` | Synthetic patient seed |
| `V8__admissions.sql` | Admission seed |
| `V9__diagnoses.sql` | Diagnosis seed |
| `V10__prescriptions.sql` | Prescription seed |
| `V11__appointments.sql` | Appointment seed |
| `V12__patient_audit_trigger.sql` | Patient create/update date trigger |
| `V13__patient_identity_unique.sql` | Unique constraint for non-null patient identity numbers |
| `V14__app_users.sql` | Database-backed staff accounts, roles, token version, indexes, and seed accounts |
| `V15__audit_logs.sql` | Action-level business audit table, constraints, sequence, and newest-first index |
| `V16__audit_log_sample_events.sql` | Sixteen synthetic events for the local Activity history demonstration |
| `V17__structured_audit_events.sql` | Versioned event snapshots, ordered change and affected-target tables, constraints, sequences, and indexes |
| `V18__structured_audit_sample_events.sql` | Ten structured synthetic events for list and detail demonstration |
| `V19__patient_self_service_accounts.sql` | Patient role, one-to-one patient account link, nullable Patient email, and forced password-change flag |
| `V20__staff_identity_accounts.sql` | Staff identity number, identity-backed temporary credentials, and individual fictional staff accounts |
| `V21__patient_demo_account.sql` | Reproducible forced-change Patient demo login linked to the seeded record with three admissions |

Do not hard-code this count in future policy. Inspect the directory because new
migrations must extend the sequence.

## JPA and query model

`PatientEntity`, `PatientHistoryEntity`, `AdmissionEntity`, `AppUserEntity`, and
`AuditLogEntity` are mapped JPA entities. Their write workflows use Spring Data
repositories. Medical-record and statistics views still query the remaining
clinical and reference tables through native SQL and interface projections.

Care visits, tasks, results, notes, handoffs, diagnoses, and prescriptions also
have mapped entities. Clinical writes run through the care workflow service.
Appointments and master-data CRUD remain outside the delivered write workflow.

Medical-record queries attach diagnoses and prescriptions by their explicit
care-visit link first and otherwise by admission. The response combines each
visible care visit with its linked admission, includes outpatient clinical rows,
and retains independent historical admissions. A hidden care visit must not leak
through an admission fallback. My Record returns profile plus the same single
record list after account-derived ownership resolution; only completed episodes
release clinical content, and service results require review of the latest
published revision. Drafts, internal notes, handoffs and admission notes are
excluded from the Patient response.

## Authentication and authorization

All `/api/v1/**` endpoints require authentication except
`POST /api/v1/auth/login`. The backend is stateless and returns the normal
`ApiResponse` envelope for authentication and authorization failures.

| Method and path | Permission | Behavior |
|---|---|---|
| `POST /api/v1/auth/login` | Public | Verifies username and BCrypt password, then issues an HS256 bearer JWT |
| `GET /api/v1/auth/me` | Authenticated | Returns the current account, role, effective sorted permissions, and forced-password-change flag |
| `POST /api/v1/auth/change-password` | `CHANGE_OWN_PASSWORD` | Verifies the current password, stores a new BCrypt hash, clears the one-time flag, revokes the current token, and requires login again |
| `/api/v1/users/**` | `MANAGE_USERS` | Administrator-only staff account listing, detail, create, edit, status, and password reset; reset has no password body and restores the stored identity number |

The JWT issuer is `hospital-api`. Its lifetime comes from
`hospital.security.jwt.access-token-ttl`, and production-style runs must supply
a secret of at least 32 UTF-8 bytes through `JWT_SECRET`. Every authenticated
request reloads the active account and compares the token-version claim with
the database value. Password reset increments the version. Never log or return
passwords, hashes, JWT secrets, or bearer tokens.

The delivered role-to-permission mapping is:

| Role | Permissions |
|---|---|
| `RECEPTIONIST` | `VIEW_PATIENT`, `CREATE_PATIENT`, `UPDATE_PATIENT`, `PATIENT_CHECK_IN`, `CHANGE_OWN_PASSWORD` |
| `DOCTOR` | `VIEW_PATIENT`, `VIEW_MEDICAL_RECORD`, `VIEW_STATISTICS`, `PATIENT_START_EXAMINATION`, `PATIENT_COMPLETE_EXAMINATION`, `ADMISSION_CREATE`, `ADMISSION_UPDATE`, `ADMISSION_DISCHARGE`, `CHANGE_OWN_PASSWORD` |
| `NURSE` | `VIEW_PATIENT`, `VIEW_MEDICAL_RECORD`, `CHANGE_OWN_PASSWORD` |
| `PATIENT` | `VIEW_OWN_RECORD`, `CHANGE_OWN_PASSWORD`; before the one-time password is replaced, only `CHANGE_OWN_PASSWORD` is effective |
| `ADMIN` | Every staff-facing value in `AppPermission`, including deletion, account management, patient field history, and business audit; no Patient-only record route |

Extend `AppPermission` and this mapping deliberately when adding a protected
feature. Controller annotations are the enforced API boundary. Frontend guards
are usability controls and are not a security boundary.

## Patient API

All active endpoints return JSON under `/api/v1`.

| Method and path | Input | Output and behavior |
|---|---|---|
| `GET /api/v1/patients` | `page=1`, `size=20`, optional `search`, `searchField`, `status`, `gender` | Paginated patient summaries sorted by name |
| `GET /api/v1/patients/history` | `page=1`, `size=20` | Paginated global field-history entries with patient identity |
| `GET /api/v1/patients/{id}` | Path ID | Full patient detail or 404 |
| `GET /api/v1/patients/{id}/history` | Path ID, pagination | Paginated history for one patient or 404 |
| `GET /api/v1/patients/{id}/medical-record` | Path ID | Unified care-visit records with linked admission, diagnoses, prescriptions and published results; independent historical admissions retained |
| `POST /api/v1/patients` | Valid create body | HTTP 201 and created patient detail |
| `PUT /api/v1/patients/{id}` | Valid full update body without status | HTTP 200 and updated detail; records changed fields |
| `POST /api/v1/patients/{id}/check-in` | Path ID | `PATIENT_CHECK_IN`; changes Discharged to Waiting |
| `POST /api/v1/patients/{id}/start-examination` | Path ID | `PATIENT_START_EXAMINATION`; changes Waiting to Examining |
| `POST /api/v1/patients/{id}/complete-examination` | Path ID | `PATIENT_COMPLETE_EXAMINATION`; changes Examining to Discharged only when no episode is active |
| `DELETE /api/v1/patients/{id}` | Path ID | Permanent cascade delete |
| `POST /api/v1/patients/bulk-delete` | `{ "ids": ["..."] }` | Permanent bulk delete |
| `GET /api/v1/patient-portal/me` | No patient ID input | `VIEW_OWN_RECORD`; returns only the profile and unified examination/treatment records linked to the authenticated Patient account |

Accepted `searchField` values in the repository are effectively `name`,
`phone`, and `identity`; unknown values fall back to name behavior. Current list
filters are status and gender. Do not document department filtering until it
exists.

The delete methods currently return HTTP 200 with an envelope whose internal
`statusCode` is 204; they do not produce an empty HTTP 204 response. Preserve
the current frontend contract or change the controller and tests deliberately.

`GET /api/v1/patients/history` and
`GET /api/v1/patients/{id}/history` expose field-level `PATIENT_HISTORY` data
under `VIEW_PATIENT_HISTORY`. They do not back the global Activity history
page, which uses the business-audit endpoint below.

## Admission API

| Method and path | Permission | Behavior |
|---|---|---|
| `GET /api/v1/admissions/options` | `ADMISSION_CREATE` | Active doctor, available room, department, and active admission-type options |
| `POST /api/v1/patients/{patientId}/admissions` | `ADMISSION_CREATE` | Creates an active admission and returns HTTP 201 |
| `GET /api/v1/patients/{patientId}/admissions/{admissionId}/edit-data` | `ADMISSION_UPDATE` | Returns the current active admission and edit options |
| `PUT /api/v1/patients/{patientId}/admissions/{admissionId}` | `ADMISSION_UPDATE` | Edits an active admission |
| `POST /api/v1/patients/{patientId}/admissions/{admissionId}/discharge` | `ADMISSION_DISCHARGE` | Completes the active episode, releases its room, and discharges the patient |
| `DELETE /api/v1/patients/{patientId}/admissions/{admissionId}` | `ADMISSION_DELETE` | Permanently deletes an admission and its cascaded clinical rows |

Doctor and Administrator can create, edit, and complete active treatment
episodes. Only Administrator can delete them. Create requires an Examining
patient and assigns admission status code 1 with a null discharge date. An
optional room controls whether patient status remains Examining or becomes
Admitted. Room changes synchronize the patient status. Completion assigns
admission status 2 and today's discharge date, releases any room, and changes
the patient to Discharged. Patient locking plus the function-based unique index
protects the one-active-episode rule. These room-dependent rules apply to direct
operations without an open care visit. `admission_inpatient_flag = 1` records
confirmed ward reception and preserves Admitted when the bed is removed.
Care-linked edits require the responsible doctor and preserve doctor/department;
completion must use the care endpoint, and admission deletion is blocked while
the care visit is open. Doctor handoff is delivered; inter-ward transfer remains future work.

## Care workflow extension

The shipped `V22__care_workflow.sql` defines eight `CARE_*` tables, a unique
optional account-to-doctor link, visit links for diagnoses/prescriptions, and
`admission_inpatient_flag`. `V23__care_demo_configuration.sql` supplies fictional
points, services, staff assignments, two Technician accounts, and a linked
imaging doctor account. No patient visits or results are backfilled.

`CareController` exposes paginated visits, tasks, and inpatients and explicit
registration, arrival, examination, ordering, result, review, reception,
completion, consultation, handoff, and configuration commands under
`/api/v1/care`. Services enforce current role permissions plus active point
assignments and clinical ownership. All care writes lock the patient first.
Published corrections append revisions; stale result acknowledgement is a
conflict. Read queries redact clinical content according to care access.
See `docs/care-workflow.md` for the current role workflow and setup.

## Business audit API and scope

| Method and path | Permission | Output and behavior |
|---|---|---|
| `GET /api/v1/audit-logs` | `VIEW_AUDIT_LOG` | One-based paginated and filtered action-level events, default size 20 and maximum size 100, sorted by `occurredAt` then `id` descending |
| `GET /api/v1/audit-logs/{id}` | `VIEW_AUDIT_LOG` | One event header with ordered `changes` and ordered `affectedTargets`, or a not-found response |

The endpoint accepts optional `actor`, `search`, `action`, `entityType`, `from`,
and `to` parameters. Actor is an exact case-insensitive match against username
or actor display name. Search is case-insensitive across actor username and
display name, resource and subject identifiers and labels, details, and summary.
Actor accepts at most 100 characters and search accepts at most 200 characters.
Action and entity type are exact enum values. `from` starts
at the beginning of that calendar date, while `to` includes the whole calendar
date through the exclusive start of the next day. One-sided date ranges are
valid. A starting date after the ending date returns a field-level 400 error on
`to`. Apply every criterion before pagination so page totals describe the
matching result set.

Only Administrator has `VIEW_AUDIT_LOG`. `AuditLogService.record` uses
`Propagation.MANDATORY`, so the business write and its success event commit or
roll back together. A failed write leaves no success event. A successful
patient update records one action-level event even when no tracked field value
changed and therefore no `PATIENT_HISTORY` row was needed.

The list response retains the original version 1 fields and adds event version,
legacy state, sample provenance, actor snapshots, optional subject snapshots,
summary, change count, and affected-target count. The detail response wraps that
event with ordered change and target collections. A missing version is
interpreted as version 1. Missing structured collections are returned empty
rather than fabricated.

The current action and entity scope is:

| Entity | Recorded actions |
|---|---|
| `PATIENT` | `CREATE`, `UPDATE`, `DELETE`, `BULK_DELETE` |
| `ADMISSION` | `CREATE`, `UPDATE`, `DELETE` |
| `USER` | `CREATE`, `UPDATE`, `STATUS_UPDATE`, `PASSWORD_RESET` |

Do not generate business events for reads, searches, filtering, pagination,
page navigation, login, logout, or other non-writing interactions. Audit details
must never contain passwords, password hashes, JWTs, refresh material, or other
secrets.

The broad keyword predicate uses case-normalized leading-wildcard matching. It
can scan at larger volumes, so do not add speculative indexes without an Oracle
EXPLAIN PLAN and representative load evidence. The API exposes no audit mutation
operation, but the tables are not append-only or tamper-evident. Production
deployment requires restricted database grants and a retention policy. Source
and isolated tests for V17 and V18 do not replace live Oracle migration and
rollback integration evidence.

## Statistics API

| Method and path | Input | Output |
|---|---|---|
| `GET /api/v1/statistics/patients` | Optional ISO dates `from`, `to`, `icdCode`, `medicineId` | `{ total, patients }` |
| `GET /api/v1/statistics/diagnosis-options` | None | ICD filter options that occur in diagnosis data |
| `GET /api/v1/statistics/medicine-options` | None | Medicine filter options that occur in prescription data |

The former Claude plan listed CRUD APIs for departments, doctors, rooms,
medicines, diagnoses, prescriptions, appointments, and master data. Those APIs
do not currently exist and must never be described as implemented.

## Request validation

Create and update use the same current field constraints:

| Field | Current API rule |
|---|---|
| `name` | Required, nonblank, max 100 |
| `birthDate` | Optional, past or present |
| `gender` | Optional, 1 through 3 |
| `address` | Optional, max 255 |
| `phoneNo` | Optional, max 20 |
| `email` | Optional valid email, max 50; not unique |
| `identityNo` | Create: required and exactly 12 digits. Update: null/blank only for legacy compatibility, otherwise exactly 12 digits. Unique when nonblank and also used as the generated Patient username. |
| `bloodType` | Optional, 1 through 8 |
| `allergies` | Optional, max 500 |
| `medicalHistory` | Optional, max 1000 |
| `insuranceNo` | Optional, max 20 |
| `insuranceExpiry` | Optional date |
| `emergencyContactName` | Optional, max 100 |
| `emergencyContactPhone` | Optional, max 20 |
| `emergencyContactRelation` | Optional, max 50 |
| `status` | Required, 0 through 3 |
| `note` | Optional, max 1000 |

Frontend validation must remain aligned. The generated Patient username is the
unique identity-card number. Duplicate patient email is still not rejected, so
do not claim email uniqueness.

Bulk delete requires a nonempty list but does not currently validate every ID
with the same not-found behavior as single delete.

## Response envelope and errors

The envelope shape is:

```text
success: boolean
statusCode: number
message: string
data: T or omitted
errors: field-to-message map or omitted
timestamp: instant
```

Success helpers use `Success`, `Created`, and `No Content`. Error handling maps:

- unauthenticated API access → 401;
- authenticated access without the required permission → 403;
- missing patient → 404;
- duplicate identity number → 400 with a field error;
- malformed or missing body → 400;
- Jakarta Validation failures → 400 with field errors;
- unexpected exception → logged server-side and generic 500 response.

Paginated data is:

```text
items
pagination:
  page
  pageSize
  totalItems
  totalPages
  hasNext
  hasPrev
```

The external page number is one-based.

## Permanent deletion and cascades

Deleting a patient is permanent. Physical foreign keys cascade to admissions,
diagnoses, prescriptions, appointments, and patient history. Deleting an
admission cascades to linked diagnoses and prescriptions; deleting a diagnosis
cascades to linked prescriptions.

The service explicitly removes linked Patient accounts before single and bulk
patient deletion in the same transaction. The `APP_USERS` foreign key also
cascades on patient deletion as a database integrity safeguard.

Always inspect cascade effects and obtain the existing UI confirmation. Do not
replace patient deletion with a status flag, archive flow, or soft-delete-only
design.

## Field-level patient-history implementation

On update, the service:

1. loads the existing patient;
2. validates identity-number uniqueness;
3. snapshots tracked fields;
4. maps the request onto the entity;
5. updates patient metadata with the authenticated username and saves;
6. creates or synchronizes the linked Patient account, updating its username
   and revoking tokens when the identity number changed;
7. compares before and after values;
8. inserts one `PATIENT_HISTORY` row for each changed field.

This runs in one service transaction. Values longer than 255 characters are
truncated for the history row. No history row is written when no tracked field
changed.

Do not describe this mechanism as JPA entity-listener auditing or as the global
business audit. The database trigger only manages date columns.

## Verification

During iteration, run focused tests from `hospital/`:

```powershell
.\mvnw.cmd -Dtest=ClassName test
```

For a completed or cross-cutting backend change:

```powershell
.\mvnw.cmd verify
```

The current suite contains controller tests, patient and medical-record service
tests, patient-ID tests, and a context test. JaCoCo generates a report, but the
build does not enforce a 70% minimum. Report actual measured coverage rather
than treating the legacy target as a gate.

## Known implementation boundaries

- Authentication is database-backed and enforced by Spring Security; it is no
  longer a frontend mock.
- `PATIENT_HISTORY` remains field-level and records changed values only for
  patient updates.
- `AUDIT_LOGS` records the defined successful write operations only; it is not a
  request log and intentionally excludes login, logout, reads, and navigation.
- Business audit currently covers `PATIENT`, `ADMISSION`, and `USER`; extend the
  action and entity constraints through a new migration when future write
  modules enter scope.
- No status-transition state machine is enforced.
- No master-data CRUD API exists.
- No appointment/diagnosis/prescription write API exists.
- Duplicate email is not enforced.
- Existing seeded patients do not receive accounts until a later update supplies
  a valid identity number; no self-service recovery workflow is delivered.
- Room availability has no automatic occupancy/flag synchronization trigger.
- The conceptual ERD and physical schema intentionally have different scopes.

Treat these as verified boundaries, not automatic requests to add features.
