# Project and requirements guidance

Read this guide before changing product behavior, patient workflows, proposal
compliance, or any CRUD feature that crosses the backend and frontend. It
replaces the project-overview, scope, proposal-mapping, core-feature, patient
status, lookup-display, and working-principle sections of the former
`CLAUDE.md`.

## Project identity

The project is the USTH internship topic **Development of a medical examination
and treatment management software system** for academic year 2023–2026. It is a
web application for hospital patient-record management. The operational stack
is Java 21 with Spring Boot 3.5, Angular 21, and Oracle Database.

The core record includes:

- stable patient identity, demographic, contact, insurance, emergency-contact,
  allergy, medical-history, status, and note data;
- visits or treatment episodes represented by admissions;
- diagnoses and prescriptions associated with a patient and, where applicable,
  an admission;
- doctors, departments, medicines, ICD-10 codes, rooms, and lookup values used
  to support those records;
- field-level change history for patient updates;
- action-level business activity for patient, admission, and staff-account
  mutations;
- a patient self-service account linked one-to-one to the patient record, with
  access limited to that patient's own profile and unified examination/treatment records;
- filtered statistics and patient medical-record views.

## Binding specification and precedence

The binding functional specification is:

`docs/ref/1.3. Annex 3 - Internship proposal 2026.pdf`

Apply these rules:

1. Every behavior required by the proposal must remain represented. Do not
   replace a required behavior with a different design.
2. Additional functionality is allowed when it supports the patient-record
   scope and does not contradict the proposal.
3. Permanent patient deletion is explicit in the proposal. Soft deletion,
   archival, or a status flag may supplement it but may not replace it.
4. When the proposal and implementation differ, identify the gap. Do not edit
   this guide to make the gap disappear and do not claim the requirement is
   complete without verified evidence.
5. Code, Flyway migrations, tests, and actual report sources define current
   implementation details. Historical plans are not implementation evidence.

## Proposal traceability

This table is a routing and verification aid, not a completion certificate.
Recheck the cited implementation and tests whenever the behavior changes.

| Proposal requirement | Current verification anchor | Current interpretation or known gap |
|---|---|---|
| CRUD-based patient management following MVC | `PatientController`, `PatientServiceImpl`, `PatientRepository`, and the Angular patient route | Controller-service-repository and component-service-model separation are present. |
| Patient list containing all records | `GET /api/v1/patients` and `PatientList` | The API is paginated rather than returning an unbounded list. |
| Search by patient name or keyword | `PatientRepository.search`, `PatientController.list`, and `PatientList.filterForm` | The UI supports name, phone, and identity-card search fields. |
| Filter by multiple criteria | Patient list query parameters and form controls | Current patient-list filters are status and gender, combined with search. Do not claim department filtering unless it is actually added. |
| Detail popup contains more information than the list | `PatientDetailDialog` and `PatientDetailResponse` | The list uses a summary DTO; the dialog loads the full detail DTO and medical record. |
| Create a patient form with name, age, gender, medical condition, and related data | `PatientFormDialog`, `CreatePatientRequest`, and `PatientEntity` | The implementation stores birth date rather than a mutable age. Medical context is represented by allergies, medical history, note, and related clinical tables. |
| Edit stored patient information | `PUT /api/v1/patients/{id}` and the edit dialog | The edit flow fetches the full record and records changed fields. |
| Validate on both frontend and backend | Reactive-form validators and Jakarta Validation request records | Field constraints exist on both sides. Keep them aligned. |
| Catch duplicate usernames or emails | `PatientServiceImpl.requireUniqueIdentity`, `PatientAccountLifecycleService`, and `V13__patient_identity_unique.sql` | A patient account uses the 12-digit identity-card number as its generated username and one-time password. Duplicate identity numbers/usernames are rejected; duplicate patient email is not enforced. |
| Permanently delete a patient with protection against mistakes | Delete endpoints and confirmation dialogs | Single and bulk delete are permanent. The UI confirms the operation. The backend checks existence for single delete; do not describe additional safeguards that are not implemented. |
| Record every field-level change with field, old value, new value, actor, and time | `PatientServiceImpl.recordHistory` and `PATIENT_HISTORY` | The service compares snapshots and stores changed fields. The actor comes from the authenticated server security context. |
| Bulk action for selected records | `POST /api/v1/patients/bulk-delete` and grid selection | The implemented bulk action is permanent bulk deletion. |
| Searching, filtering, status control, and change-history tracking | Patient list, workflow endpoints, history endpoints, and history UI | Status values are stored, filterable, and advanced only by the delivered workflow actions. Each successful transition records field history and a business activity event. |
| Database for patient information, department structure, hospital profiles, and reference data | Flyway schema and seed migrations | Patient, department, and reference data exist. There is no separate hospital-profile table; verify whether the report/proposal expects one before claiming it. |
| Spring Boot, Spring MVC, Spring Data JPA, Hibernate, Jakarta Validation | `hospital/pom.xml` and backend source | These are active backend technologies. |
| Angular Router, Reactive Forms, RxJS, and HttpClient | frontend package and source | These are active frontend technologies in an Angular 21 standalone application. |
| RESTful JSON API | controllers and DTO response envelope | Active traffic is under `/api/v1`. |
| Oracle sequences, triggers, constraints, and indexes | `V0__schema.sql`, `V12__patient_audit_trigger.sql`, and later migrations | All are present, but exact objects must be read from the migrations. |

## Scope boundary and model differences

Patient-record management is the grading-critical core. `APPOINTMENTS` and
`ROOMS` are outside the current conceptual ERD, but they remain deliberately
present in the physical schema and seed data. The `admission_room_id` relation
also remains. Do not remove these objects simply to make the physical schema
look identical to the conceptual model.

The generic `LOOKUP_VALUE` table also remains in the physical schema while the
conceptual ERD may show lookup codes as attributes rather than as a separate
entity. `ICD10_CODES` remains conceptually visible because it represents an
external medical classification.

Before removing any extension:

1. verify the proposal still remains fully satisfied;
2. trace every foreign key, query, seed row, UI model, report statement, and
   diagram that depends on it;
3. verify patient medical-record behavior end to end;
4. add a forward-only Flyway migration rather than rewriting applied history;
5. update the conceptual and physical documentation deliberately.

## Core patient behavior

The required patient workflow comprises:

1. a paginated list with search and combined filters;
2. a compact list row and a richer detail dialog;
3. create and edit dialogs using reactive forms;
4. equivalent validation at the UI boundary and API boundary;
5. permanent single deletion with confirmation;
6. permanent bulk deletion of selected rows with confirmation;
7. field-level update history;
8. a patient medical-record view grouped by treatment episode;
9. statistics filtered by date range, ICD code, and medicine where supported;
10. clear API error and validation feedback;
11. Administrator review of successful patient, admission, and staff-account
    mutations through a separate business activity log;
12. a generated Patient account whose first login requires password replacement
    and whose only clinical read is the server-resolved record linked to it.

## Patient self-service security boundary

Creating a patient requires a unique 12-digit identity-card number. In the same
transaction, `PatientAccountLifecycleService` creates an active `PATIENT`
account whose username and initial one-time password are that number. The
password is stored only as a BCrypt hash. The patient must replace it before
the account receives `VIEW_OWN_RECORD`; while the replacement flag is set, the
effective permission set contains only `CHANGE_OWN_PASSWORD`.

`GET /api/v1/patient-portal/me` never accepts a patient identifier from the
browser. It resolves the active authenticated account, follows its unique
patient link, and returns only that profile and unified examination/treatment records. A Patient
role has no general patient-list, medical-record, history, statistics, account
management, or mutation permission. Frontend routing is a usability layer;
Spring Security controller authorization and server-side link resolution are
the confidentiality boundary.

Changing the identity number synchronizes the generated account username and
revokes existing tokens. Permanent single or bulk patient deletion deletes the
linked account in the same transaction; the database cascade is a final
integrity safeguard. Staff account-management endpoints deliberately exclude
Patient accounts. Existing seeded patients are not bulk backfilled by V19. V21
adds one explicit fictional Patient demo account for `P26ML011`, whose seeded
record has three admissions; another legacy patient receives an account when a
valid identity number is supplied by a later update.

Do not turn implementation extensions into proposal requirements. Conversely,
do not use an extension to avoid implementing a proposal requirement.

## Patient status

The persisted and displayed patient status codes are:

| Code | Meaning |
|---:|---|
| 0 | Waiting |
| 1 | Examining |
| 2 | Admitted |
| 3 | Discharged |

Preserve this mapping across persistence, frontend options, history formatting,
diagrams, and report prose. Patient create and generic edit requests do not
accept status. New patients start in Waiting. Receptionist and Administrator
can check in a Discharged patient, producing `3 → 0`. Doctor and Administrator
can start an examination, producing `0 → 1`, or complete one without an active
treatment episode, producing `1 → 3`. Creating or editing an active episode
with a room produces `1 → 2`; removing its room produces `2 → 1`. Completing
an active episode produces `1 → 3` or `2 → 3`. Every successful transition is
written to patient field history and the business activity log. These direct
transitions apply only without an open care visit. In the delivered care
workspace, ward reception establishes inpatient status even without a bed, and
later bed removal does not reverse it. Service trips preserve patient status.
Doctor handoff requires acceptance; inter-ward transfer is not implemented.
See `docs/care-workflow.md` and the root policy for the complete care workflow.

Admission status is a separate code system. Do not interchange patient status
with admission status.

## Stable patient data and episodic clinical data

`PATIENTS` stores stable patient information. A patient is not permanently
assigned to one department, doctor, or room because those assignments vary by
treatment episode.

- `ADMISSIONS` records the department, doctor, optional room, dates, reason,
  type, status, and notes for an episode.
- `APPOINTMENTS` records a scheduled doctor and department.
- `DIAGNOSES` records the diagnosing doctor and optional admission.
- `PRESCRIPTIONS` records the prescriber, diagnosis, medicine, and optional
  admission.

When current assignment is needed, derive it from the active admission rather
than adding permanent department, doctor, or room columns to `PATIENTS`.

## Patient identifier

Patient IDs are human-readable and application-generated:

`P + YY + LL + NNN`, for example `P26MB001`.

- `YY` is the last two digits of the creation year.
- `NNN` is a three-digit sequence that restarts by year.
- `LL` is a two-letter check value derived from `(YYNNN) mod 676`.
- The database enforces the shape `^P[0-9]{2}[A-Z]{2}[0-9]{3}$`.

Preserve `PatientIdGenerator`; do not replace patient IDs with UUIDs or numeric
database-generated IDs. UUIDs are currently used for history-row identifiers,
which is a separate concern.

## Patient identity in other screens

When another screen, selector, table, diagnosis, prescription, appointment, or
report references a patient, show the readable patient ID together with the full
name. Use the full detail view for the remaining personal and medical fields.
Do not expose more sensitive data than the interaction needs.

## Patient field history and business activity audit

Patient update history is field-level rather than a full-row snapshot. Each row
stores:

- patient ID;
- field name;
- previous value;
- new value;
- changed-by value;
- changed date.

The current implementation compares before/after maps in
`PatientServiceImpl.recordHistory` inside the update transaction. It does not
use JPA `@EntityListeners`. `V12__patient_audit_trigger.sql` only stamps patient
date columns; it does not create `PATIENT_HISTORY` rows.

Do not record unchanged fields. Keep the patient update and its history rows in
one transaction. The actor comes from the authenticated username resolved by the
server. Do not accept it from a request body and do not fabricate historical
actors.

`AUDIT_LOGS` is a separate business-event header. It records successful create,
update, delete, bulk-delete, status-update, and password-reset actions for
patients, admissions, and staff accounts. Version 2 events snapshot actor,
resource, optional subject, and a concise change summary. The summary
does not repeat the event name or target label. A sample provenance flag keeps
demonstration rows distinct from operational activity. Ordered changed fields are
stored in `AUDIT_LOG_CHANGES`, while ordered bulk-operation targets are stored in
`AUDIT_LOG_TARGETS`. This mechanism does not replace `PATIENT_HISTORY`.

Only Administrator receives `VIEW_AUDIT_LOG`. Event headers retain actor,
resource, and subject snapshots without foreign keys to staff accounts or
domain rows. Child rows reference only the event header. The complete audit
write participates in the same transaction as the mutation that produced it.
Classified personal and medical change values are redacted. Secret change values
are not recorded. Resource, subject, and operational reference labels remain
readable snapshots for the Administrator.

Activity history supports server-side filtering before pagination. Optional
criteria are an exact case-insensitive actor username or display name, a keyword
across actor snapshots, resource and subject identities, details, and summary,
an exact action, an exact entity type, and inclusive starting and ending dates.
Applying or clearing criteria returns to page one. A starting date after the
ending date is invalid. An Administrator can open the details of one event.
Version 1 rows remain readable and state when structured changes or affected
targets were not captured. Synthetic V16 and V18 rows are visibly identified as
sample data and must not be treated as evidence of an operational action.

## Status-flag semantics

Status flags are table-specific and must not be generalized blindly:

- doctor, medicine, ICD-10, and lookup status flags represent whether reference
  data remains available for new use while preserving historical foreign keys;
- inactive reference items should be excluded from new-record choices but still
  resolved in historical records;
- shipped room seed data uses `ROOMS.status_flag` together with bed count and
  occupancy as room availability; rows with full occupancy are flagged `0`;
- none of these flags creates a generic user-facing Activate/Deactivate feature;
- none of them may substitute for permanent deletion of patient records.

Before changing a flag, inspect its table definition, seed migration, all query
uses, and the report. If the desired meaning differs from the shipped meaning,
make that an explicit schema and behavior change.

## Data ethics and privacy

- Generate fake Vietnamese names, addresses, phone numbers, identity numbers,
  and other personal details for patients and doctors.
- Never introduce a real person's medical or identifying information.
- Use local reference datasets and official medical sources for ICD-10,
  medicines, departments, and terminology.
- Sample-patient datasets are structure references only. Do not copy their
  records into project seeds.
- Do not put credentials, private network addresses, or local machine paths into
  committed guidance or configuration.

## Working principles for Codex

### Inspect before deciding

- Read the applicable `AGENTS.md`, detailed guide, implementation, tests, and
  proposal section before editing.
- State an assumption when it materially affects behavior.
- Ask only when a missing choice would materially change scope or behavior.
  Otherwise inspect the repository and choose the smallest compatible approach.
- Surface a proposal/code conflict rather than silently choosing one side.

### Keep the solution proportional

- Implement only requested or proposal-required behavior.
- Avoid enterprise abstractions, speculative configuration, microservices,
  cloud infrastructure, or security systems that are outside this internship
  scope.
- Prefer the smallest coherent implementation that follows the existing
  architecture.
- Do not introduce an abstraction for a single use unless it solves a verified
  recurring problem.

### Make surgical changes

- Preserve unrelated user modifications.
- Match existing conventions and do not reformat unrelated code.
- Do not remove pre-existing dead code or rewrite old migrations unless the task
  explicitly includes that cleanup.
- Remove only the imports, variables, files, or comments made obsolete by the
  current change.
- Every changed line should trace to the request, a required compatibility fix,
  or the verification needed to support it.

### Work toward verifiable outcomes

- Convert a feature request into observable success criteria.
- Reproduce a bug with a focused test when practical.
- Run focused checks during iteration and the appropriate completion checks at
  the end.
- Do not report a test, coverage value, screenshot, API result, or report build
  that was not actually produced.
- Update the report and progress log when behavior, schema, design, or measured
  evidence changes, unless the user explicitly excludes report work.

## Completion criteria

A behavior-changing task is complete only when:

- the proposal requirement remains satisfied;
- backend and frontend contracts agree;
- schema changes are forward-only Flyway migrations;
- relevant unit or integration checks pass;
- permanent deletion, validation, history, and pagination conventions remain
  intact where applicable;
- report and diagrams are updated when the implementation or evidence changed;
- any unrun check is reported with the exact reason.
