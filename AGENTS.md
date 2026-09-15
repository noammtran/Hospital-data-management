# Hospital Patient Management System

## Mandatory startup reading for every new Codex task

Before planning, editing, testing, or reporting completion, read the following
sources in this order. Do not rely only on a previous chat summary.

1. Read this repository policy at `AGENTS.md`.
2. Read the guidance index at `.agents/guidance/README.md`.
3. Read every task-specific guide selected by that index. The guides are stored
   under `.agents/guidance/` and cover requirements, backend, frontend,
   development data, report and diagrams, and the legacy migration map.
4. Read the nearest nested instructions for the area being changed. Backend work
   uses `hospital/AGENTS.md`, frontend work uses
   `hospital-frontend/AGENTS.md`, and report work uses
   `docs/report/AGENTS.md`.
5. Read `README.md` before using setup, build, run, or verification commands.
6. Read `docs/ref/1.3. Annex 3 - Internship proposal 2026.pdf` when the task can
   affect product requirements, scope, or user-visible behavior.
7. Inspect the current code, Flyway migrations, tests, and LaTeX sources that
   implement the requested area. They take precedence over old planning notes
   for implementation details.
8. When continuing report or verification work, read the newest entries in
   `docs/report/progress-log.md` and `docs/report/test-results.md` before making
   changes or repeating checks.

For a cross-stack task, combine all applicable guidance instead of selecting
only one area. If a required file is missing or contradicts a more authoritative
source, stop and report the conflict before changing behavior.

## Sources of truth

- Treat `docs/ref/1.3. Annex 3 - Internship proposal 2026.pdf` as the binding
  functional specification. Do not replace a required behavior with a different
  design, such as replacing permanent deletion with soft deletion only.
- Treat the running code, Flyway migrations, and tests as the source of truth for
  implementation details. Planning documents can be stale.
- Use `README.md` for supported setup and run commands.
- Follow the nearest nested `AGENTS.md` for backend, frontend, or report work.
  Nested instructions extend this file; they do not replace repository-wide
  requirements.

## Required detailed guidance

The former Claude project notes were migrated without discarding their useful
detail. `AGENTS.md` files hold the small, always-loaded policy layer, while
`.agents/guidance/` contains the complete Codex-oriented operational guidance.

Before making an implementation decision, editing files, or reporting
completion, read `.agents/guidance/README.md` and every guide whose scope
matches the task:

- `01-project-and-requirements.md` for proposal compliance, product behavior,
  patient workflows, or cross-stack CRUD;
- `02-backend-database-api.md` for Java, Spring Boot, JPA, Oracle, Flyway,
  schema, API, validation, history, medical records, or statistics;
- `03-frontend.md` for Angular, UI, forms, routing, HTTP, styling, or frontend
  tests;
- `04-development-and-data.md` for setup, commands, troubleshooting, seed or
  reference data, provenance, privacy, or external datasets;
- `05-report-and-diagrams.md` for LaTeX, report content, citations, evidence,
  Mermaid, UML, ERD, figures, or report verification.

A cross-stack task must load all matching guides. The exact original
`CLAUDE.md` remains under `.legacy-ai/` for provenance only; it contains stale
plans and machine-specific values and is not an active instruction source.
Use `.agents/guidance/06-claude-migration-map.md` to audit the disposition of
every former section.

## Product scope

This is the USTH internship project **Development of a medical examination and
treatment management software system**. The core product is patient-record
management: patient identity and contact data, visits or admissions, diagnoses,
prescriptions, and the supporting doctor, department, medicine, ICD-10, and
lookup data.

Appointments and rooms are outside the current conceptual ERD, but they remain
intentionally present in the database migrations and seed data. Do not remove
`APPOINTMENTS`, `ROOMS`, or `admission_room_id` unless the user explicitly requests
that change and the patient-record workflow has been verified end to end. The
conceptual model and physical schema therefore intentionally differ.

Required behavior includes patient CRUD, paginated search and multi-criteria
filtering, detail and create/edit dialogs, frontend and backend validation,
permanent single and bulk deletion with confirmation, and field-level change
history.

## Delivered authentication, Patient self-service, admission, and audit extension

The running system now has database-backed staff authentication and permission
checks for Receptionist, Doctor, Nurse, Technician, and Administrator. Receptionist and
Administrator can check a discharged patient in for a new visit. Doctor and
Administrator can start and complete examinations, create and edit an active
admission, and complete an active treatment episode. Only Administrator can
permanently delete an admission. Active means that the discharge date is empty.
Accepted doctor handoff and consultation are delivered through the care workspace. Inter-ward inpatient transfer remains future work.

The self-service role is `PATIENT`. The additional staff role `TECHNICIAN` uses assigned care tasks and has no general patient-list or medical-record permission. Creating a new patient requires a
12-digit identity-card number and creates one linked `APP_USERS` row in the same
transaction. The identity number is the username and initial password input;
only its BCrypt hash is stored. A temporary-password Patient has only
`CHANGE_OWN_PASSWORD`. After replacement and re-authentication, Patient has
`VIEW_OWN_RECORD` and can call only `GET /api/v1/patient-portal/me` for record
data. That endpoint accepts no patient identifier: the backend resolves
`app_user_patient_id` from the current account and returns only that patient's
personal information and unified examination/treatment records. Patient never receives general
patient-list, patient-detail, medical-record, history, statistics, admission
write, user-management, or audit permissions. Patient accounts are hidden from
staff-account management, synchronized with identity/name changes, explicitly
removed by single or bulk patient deletion, and also protected by an
`ON DELETE CASCADE` foreign key. Existing records are not bulk assigned a
reversible migration credential. V21 deliberately adds one fictional Patient
demo account for `P26ML011`, whose seeded record has three admissions; every
other identity-bearing legacy record receives an account only when subsequently
updated.

Staff accounts use a 12-digit identity-card number as both username and initial
temporary password input; only its BCrypt hash is stored. V20 backfills the four
original demonstration accounts and adds individual fictional accounts for the
seeded doctors, nurses, and receptionists. Receptionist, Doctor, Nurse, and
Administrator can open Change password from the profile menu. The separate
staff-account table and reset action remain Administrator-only. Reset never
accepts a replacement secret: it restores the account password to the stored
identity number, requires another change at next sign-in, and revokes existing
tokens. Patient accounts remain excluded from staff management.

Staff creation opens a work-details review and links to care configuration with
the selected account. The review lists role functions, doctor linkage, all point
assignments, and missing setup separately from login restrictions. Administrator
can create and link a new doctor profile for a clinical account before assigning
work. The profile and account link commit together and record a USER update
event. Existing links cannot be replaced through this operation. This uses the
existing doctor table and does not introduce general reference-data CRUD.

Keep the two delivered audit mechanisms distinct:

- `PATIENT_HISTORY` is per-patient field history. It stores old and new values
  for fields changed by a patient update and remains available in the
  per-patient popup.
- `AUDIT_LOGS` is the business-event header. It records successful patient
  create, update, delete, bulk-delete, and status operations, admission create,
  update, status, and delete operations, and staff-account create, update, status, and
  password-reset operations. Version 2 headers snapshot the actor display name
  and role, the primary resource, an optional business subject, a concise
  change summary, child-row counts, and sample provenance. The summary must add meaning
  such as `2 fields changed`, `Permanently deleted`, or `Active to Locked`
  instead of repeating the event name or target label.
- `AUDIT_LOG_CHANGES` stores ordered typed before-and-after values for one event.
  `AUDIT_LOG_TARGETS` stores ordered affected targets for multi-record actions.
  Version 1 rows from V15 and V16 remain readable through the legacy fallback.
  V17 marks the synthetic V16 rows as sample data, and V18 inserts structured
  sample rows with the same flag. Runtime events are never marked as samples.

Business audit actions are `CREATE`, `UPDATE`, `DELETE`, `BULK_DELETE`,
`STATUS_UPDATE`, and `PASSWORD_RESET`. Entity types are `PATIENT`, `ADMISSION`, `USER`, `CARE_VISIT`,
`CARE_TASK`, `CARE_RESULT`, and `CARE_ASSIGNMENT`. Reads, searches, filters, pagination, page navigation, login, logout,
and other non-writing interactions do not create business audit events.

`GET /api/v1/audit-logs` and `GET /api/v1/audit-logs/{id}` are protected by
`VIEW_AUDIT_LOG`, which belongs only to Administrator. The Angular route is
`/audit-log`; `/patient-history` redirects there for compatibility. The old
patient-history endpoints remain available for field-history data and do not
back the Activity history page. The list uses Time, Event, Target, Performed by,
Change summary, and a text-only View details action. The details dialog must
preserve action-specific layouts and truthful legacy messages. Sample rows use
a visible Sample prefix in the list and an explicit demonstration warning plus
data source in the dialog. They must not be presented as operational evidence.

The Activity history list endpoint accepts optional `actor`, `search`, `action`,
`entityType`, `from`, and `to` filters. Actor is an exact case-insensitive match
against username or actor display name. Search covers actor identity and display
name, resource and subject identifiers and labels, details, and summary. Date
boundaries are inclusive calendar dates. Filtering happens on the server before
pagination, and a starting date after the ending date is a validation error.

`AuditLogService.record` participates in the existing write transaction. A
failed write leaves no success event. `AUDIT_LOGS` deliberately has no foreign
key to an account or domain row so actor, resource, and subject snapshots remain
readable after permanent deletion. Its child tables reference only the event
header with cascading deletion. Classified personal and medical change values
are redacted, and secret changes are omitted. Never put passwords, hashes, access tokens, or
other secrets in an audit event.

A successful patient update that submits the same tracked values still records
one successful `UPDATE` event with a zero change count. It creates no
`PATIENT_HISTORY` row because no field changed. Do not describe this case as an
attempted or failed event.

The current keyword query uses case-normalized leading-wildcard matching. Do not
claim enterprise-scale performance without representative Oracle plans and load
evidence. No public audit mutation endpoint exists, but the database rows are not
append-only or tamper-evident. Production work still requires restricted grants,
retention policy, and live Oracle migration and rollback verification.

Patient status codes currently have the stable meanings `0 = Waiting`,
`1 = Examining`, `2 = Admitted`, and `3 = Discharged`. Preserve that mapping
across the API, UI, migrations, and report. New patients start in Waiting.
Care registration changes Discharged to Waiting. A linked and assigned doctor receives an arrived patient to change Waiting to Examining. Ward reception changes Examining to Admitted even without a bed. Services and bed removal preserve confirmed inpatient status. Completion changes Examining or Admitted to Discharged, subject to result-review and follow-up checks. Direct status/admission operations remain for records without an open care visit and retain their existing room-dependent behavior. They cannot bypass an active care workflow.

The delivered care workspace is `/care`, backed by `/api/v1/care`. It stores care points, services, staff assignments, visits, examination/service tasks, result revisions, notes, and handoffs. A patient has at most one open visit. Keep responsible doctor, last confirmed location, patient status, task status, and arrival state separate. Published results require acknowledgement of the current revision; corrections retain older versions. Doctor transfer requires acceptance, while consultation only returns an opinion. A closed visit can retain pending services only with a future follow-up plan. Existing active inpatient admissions can enter the workspace without fabricated history. See `docs/care-workflow.md` for the delivered workflow.

Care writes serialize through the patient lock and record successful business events using `CARE_VISIT`, `CARE_TASK`, `CARE_RESULT`, and `CARE_ASSIGNMENT` resource types. Care assignment also identifies configuration events. Clinical signing requires a linked doctor account and active point assignment even for Administrator. Reception receives coordination metadata, and Technician receives only assigned work. Patient portal reads remain account-derived and read-only, with completed clinical content released through the unified record described below.

Treat the status flags on doctors, medicines, ICD-10 codes, and lookup values as
reference-data availability flags that preserve historical foreign-key
integrity. New-record selectors should exclude inactive reference items, while
historical records must still resolve and display them. `ROOMS.status_flag` is
different: the shipped seed data uses it as room-availability state alongside
`bed_count` and `current_occupancy`, not as a soft-delete flag. Verify the
specific table and migration before interpreting any status flag. None of these
flags is a generic Activate/Deactivate UI feature, and none may replace the
proposal-required permanent deletion of patient records.

## Repository map

- `hospital/`: Java 21, Spring Boot 3.5, Spring MVC, Spring Data JPA, Flyway,
  and Oracle.
- `hospital-frontend/`: Angular 21 standalone application using Angular Material.
- `docs/report/`: English LaTeX internship report.
- `docs/uml/` and `docs/erd/`: Mermaid sources and rendering inputs for report
  diagrams.
- `data/reference/`: external medical reference datasets.
- `hospital/src/main/resources/db/migration/`: the migrations actually shipped by
  the backend.
- `.agents/guidance/`: complete task-scoped Codex guidance and the
  Claude-to-Codex migration record.

## Working agreement

- Inspect the relevant implementation before editing; do not rely on remembered
  paths or old design notes.
- Make the smallest coherent change and preserve unrelated local modifications.
- Keep the application suitable for an internship project. Do not add enterprise
  abstractions or speculative features.
- Keep API traffic under `/api/v1` and preserve the existing response-envelope and
  pagination conventions unless the task explicitly changes the contract.
- Use fake Vietnamese personal data for patients and doctors. Never introduce real
  personal or medical records. Prefer `data/reference/` for medical terminology,
  medicines, departments, and ICD-10 data.
- Use a new Flyway migration for an intentional schema change. Keep Oracle syntax
  and existing naming conventions.
- Treat `hospital/src/main/resources/db/migration/` as the shipped seed and schema
  source. Files under `data/seed/` are staging or historical inputs and may
  differ from the application migrations.
- When behavior, design, or measured tests change, update the corresponding report
  section and `docs/report/progress-log.md` in the same task unless the user scopes
  report work out.
- Before continuing report or verification work in a new chat, read the newest
  entries in `docs/report/progress-log.md` and `docs/report/test-results.md`.
  Keep current measured counts in those files instead of copying transient
  results into this root guidance.
- Do not claim completion from code inspection alone. Run the checks relevant to
  the changed area.
- When the user asks Codex to create a commit, use a Conventional Commit prefix
  appropriate to the change, such as `feat:`, `fix:`, `refactor:`, `docs:`, or
  `test:`.

## Report narrative: completed system

- Present the final implemented design, workflows, behavior, and verification
  results. Explain how the completed system works and why its design was chosen.
- Do not recount migration numbers such as V19 or V22, previous implementations,
  discarded proposals, intermediate designs, or successive development and test
  runs. This applies to prose, tables, captions, and diagrams.
- Describe accounts, audit storage, and database initialization by their final
  behavior. Migration references elsewhere in this guidance are implementation
  anchors for verification, not material to copy into the report narrative.
- Use the latest relevant measured result for each check, with its date and
  evidence boundary. Keep historical development and verification records in
  `docs/report/progress-log.md` and `docs/report/test-results.md`, outside the
  compiled report.
- Preserve actual limitations, future work, and the distinction between sample
  data and operational evidence. API paths, technology versions, and security
  token-version details may remain when needed to explain the final system.
- Follow the same rule in `docs/report/AGENTS.md` for subsequent report updates.

## Definition of done

- Backend changes: run focused tests while iterating, then `.\mvnw.cmd verify` for
  a cross-cutting or completed backend change.
- Frontend changes: run affected tests and `npm run build`.
- Report changes: run `docs/report/build.ps1` when the local LaTeX toolchain is
  available and inspect the generated log for errors.
- State any check that could not run and the concrete reason.

## Repository skills

- Use `$hospital-crud` for end-to-end CRUD, validation, search/filter, history, or
  patient medical-record changes.
- Use `$angular-developer` for Angular implementation details.
- Use `$spring-boot`, `$jpa-patterns`, `$database-designer`, or `$code-quality`
  for focused backend, database, and Java review work.
- Use `$academic-research-suite` for deep research, academic-paper planning or
  drafting, revision, citation or integrity checks, reviewer-style assessment,
  and the full research-to-paper pipeline. It is the official single Codex
  router for those workflows; do not expose its nested Claude workflows as
  separate project skills.
- When an academic research or writing capability overlaps between
  `$academic-research-suite` and another skill, follow the project owner's
  preference and use `$academic-research-suite` first.
- Use focused skills only for distinct specialist work that the selected ARS
  workflow does not cover. Examples include `$latex-paper-en` for direct LaTeX
  source editing and compilation, `$latex-thesis-zh` for Chinese theses, and
  `$bib-search-citation` for local BibTeX-library search. Use `$paper-audit`
  instead of the overlapping ARS reviewer only when the user explicitly asks
  for that skill or the required narrow read-only gate is not covered by ARS.
- Third-party skill sources, commits, licenses, and Codex-only overlays are pinned
  in `.agents/skills.sources.json`. Keep project policy in `AGENTS.md` or
  `$hospital-crud`; do not silently rewrite vendored upstream skill content.

## Unified examination and treatment record

Patient details and My Record display one entry per care visit, including its
linked admission, with independent historical admissions retained. The patient
details screen has no Add admission or Add treatment episode action. New care
and inpatient reception use the care workspace. The responsible doctor can
update an active inpatient stay's details or bed there without bypassing the
handoff workflow. Unlinked existing episodes retain their permitted update and
completion tools. No schema migration or fabricated visit history is required.

Staff clinical record visibility uses the care assignment rules and excludes
unpublished results. My Record still calls only the account-derived endpoint,
which returns profile plus a single records list. It releases diagnoses,
prescriptions and care conclusions only after episode completion, and includes
a service result only when the latest published revision has been reviewed.
Drafts, internal clinical notes, handoff discussions and admission notes are
excluded from self service. The endpoint grants no staff care access or writes.
