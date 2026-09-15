# Claude-to-Codex migration map

This document accounts for every heading in the former
`.claude/CLAUDE.md`. It proves that the migration did not merely shorten the
project notes. Each legacy section was retained, corrected, split by task
scope, or preserved only as historical evidence when it was unsafe to use as
current instruction.

The original remains unchanged at
`.legacy-ai/claude-source/CLAUDE.md`. Its SHA-256 at the time of this
reconciliation is
`BB891FD8EBAA382C1AD2D2A9D0D63E3F05B9773754F461D3A6A9195D97222853`.
The archive
`.legacy-ai/backups/claude-original-before-codex-20260727.zip` preserves the
wider pre-migration Claude configuration.

The legacy document title, `Hospital Patient Management System — CLAUDE.md`,
maps to the repository-root `AGENTS.md` title and this Codex guidance index.

## Disposition labels

- **Retained**: the rule remains valid and is present in current Codex guidance.
- **Updated**: the intent remains, but versions, paths, commands, or
  implementation facts were corrected from the repository.
- **Split**: the content is complete across more than one scoped guide.
- **Historical only**: the text describes a plan, machine-specific setup, or
  behavior that is not implemented. It remains in the backup but is explicitly
  excluded from current operational guidance.

## Project, requirements, and working method

| Legacy heading | Disposition | Codex destination and adaptation |
|---|---|---|
| `Project Overview` | Retained | `01-project-and-requirements.md`, Project identity. Keeps the USTH internship context, hospital domain, and patient-record purpose. |
| `Scope Focus — Patient Records Management (note 2026-06-03)` | Updated | Root `AGENTS.md` plus guide 01, Scope boundary. Keeps patient records, admissions, diagnoses, prescriptions, and supporting data as the core. Keeps appointments and rooms in the physical schema, but removes the contradictory hard-coded conceptual-entity counts. |
| `Source of Truth: Proposal Compliance` | Updated and expanded | Root `AGENTS.md` plus guide 01, Binding specification and Proposal traceability. The proposal remains binding, while the table now distinguishes verified implementation from unresolved gaps. |
| `Working Principles (apply these to EVERY task)` | Split | Root `AGENTS.md` and guide 01, Working principles for Codex. The four principles remain, adapted to Codex inspection, dirty-worktree preservation, scoped autonomy, planning, testing, and evidence rules. |
| `1. Think Before Coding` | Updated | Guide 01, Inspect before deciding. Codex inspects first and asks only when a missing choice materially changes behavior or scope. |
| `2. Simplicity First` | Retained | Guide 01, Keep the solution proportional. Internship-scale solutions and the smallest coherent design remain mandatory. |
| `3. Surgical Changes` | Retained | Root `AGENTS.md` and guide 01, Make surgical changes. Unrelated user work and pre-existing dead code are preserved. |
| `4. Goal-Driven Execution` | Updated | Guide 01, Work toward verifiable outcomes, plus the definition of done in root and nested `AGENTS.md` files. Tests and observable evidence replace vague completion claims. |
| `Tech Stack` | Updated | Repository map, guides 02 and 03. Java 21 and Spring Boot 3.5 remain; Angular is corrected from 19+ to the installed Angular 21 stack. |
| `Data Strategy` | Split | Root `AGENTS.md`, guides 01 and 04. Fake Vietnamese personal data and real medical reference terminology remain, with privacy and provenance requirements. |
| `Reference Data Files` | Updated | Guide 04, Current local data inventory. The planned CSV tree was replaced by the paths that actually exist, and backend Flyway migrations are identified as the shipped source. |
| `Core Features` | Updated and expanded | Guide 01, Core patient behavior and Proposal traceability. All eight legacy features remain; medical-record, statistics, exact filters, and known compliance gaps are also recorded. |
| `Patient Status Flow` | Updated | Root `AGENTS.md` and guide 01, Patient status. Codes 0–3 remain; the legacy arrows are no longer treated as an enforced transition state machine because code and schema only enforce the range. |
| `Patient Lookup Display` | Retained | Root and frontend `AGENTS.md`, guides 01 and 03. Patient references must show readable ID plus full name; the history screen is documented as a current gap rather than falsely marked compliant. |
| `Status flag vs Activate/Deactivate UI — Important Distinction` | Updated | Root and backend `AGENTS.md`, guides 01 and 02. Reference availability remains distinct from UI activation. Room status is corrected to availability/fullness semantics rather than generic soft deletion. |

## Backend structure, schema, and API

| Legacy heading | Disposition | Codex destination and adaptation |
|---|---|---|
| `Project Structure — Backend` | Updated | Guide 02, Current package structure, plus `hospital/AGENTS.md`. Removed planned `config`, `audit`, and `validation` directories that do not exist; records the actual controller/service/repository/DTO/entity layout. |
| `Database Schema (Oracle)` | Updated and expanded | Guide 02, Flyway and schema rules plus Physical schema overview. Every shipped table, column, constraint meaning, cascade, sequence, index, and important absence is mapped from migrations. |
| `PATIENTS` | Updated | Guide 02, `PATIENTS`. Keeps stable personal fields and readable ID; adds V13 identity uniqueness and corrects the V12 trigger/history distinction. |
| `DEPARTMENTS — Khoa bệnh viện` | Retained | Guide 02, `DEPARTMENTS`. |
| `DOCTORS — Bác sĩ` | Updated | Guide 02, `DOCTORS`. Keeps department ownership and availability while avoiding an unimplemented public activation UI. |
| `ROOMS — Phòng/giường bệnh` | Updated | Guide 02, `ROOMS`. Keeps capacity constraints and corrects the status flag: shipped data treats it as availability, and no trigger automatically recalculates it. |
| `MEDICINES — Danh mục thuốc` | Retained and clarified | Guide 02, `MEDICINES`. Keeps all columns and reference-availability semantics. |
| `ICD10_CODES — Danh mục mã chẩn đoán ICD-10` | Updated | Guide 02, `ICD10_CODES`. Keeps code metadata and documents that diagnosis codes are text without a hard foreign key. |
| `ADMISSIONS — Đợt điều trị (khoa/bác sĩ/phòng theo thời gian)` | Updated | Guide 02, `ADMISSIONS`. Corrects ID length, distinguishes enforced one-active-admission uniqueness from unenforced status/room planning rules, and keeps the room relation. |
| `DIAGNOSES — Chẩn đoán` | Updated | Guide 02, `DIAGNOSES`. Adds the existing optional admission foreign key and corrects ID lengths. |
| `PRESCRIPTIONS — Đơn thuốc` | Updated | Guide 02, `PRESCRIPTIONS`. Adds the admission foreign key and corrects diagnosis to required. |
| `APPOINTMENTS — Lịch khám/hẹn` | Updated | Guide 02, `APPOINTMENTS`. Keeps the table and seed data while stating that no appointment controller/service/frontend feature currently exists. |
| `PATIENT_HISTORY — Audit log` | Updated | Guides 01 and 02, Change history. Corrects ID length and replaces the false `@EntityListeners` description with the transactional service snapshot/diff implementation. |
| `LOOKUP_VALUE — Master data dùng chung` | Updated | Guide 02, `LOOKUP_VALUE`. Keeps groups/codes and clarifies that business code columns currently have no physical foreign keys to the lookup table. |
| `API Design` | Updated and expanded | Guide 02, Patient API, Statistics API, Response envelope and errors. The table is generated from active controllers and tests rather than planned endpoints. |
| **Base: `/api/v1`** | Retained | Root `AGENTS.md` and guide 02. |
| `Patient APIs (core)` | Updated | Guide 02, Patient API. Adds global history and medical-record endpoints, exact list parameters, one-based pagination, and the current HTTP-200/envelope-204 delete nuance. |
| `Supporting APIs` | Historical plan separated from current API | Guide 02, Statistics API and Known implementation boundaries. Statistics endpoints are recorded. Planned department, doctor, room, medicine, diagnosis, prescription, appointment, and master-data APIs are explicitly marked absent. |
| `Standard Response Format` | Updated | Guide 02, Response envelope and errors. Keeps the envelope and pagination shapes, but records actual English messages, omitted null fields, and delete behavior. |
| `Coding Conventions` | Split | Root and nested `AGENTS.md`, guides 02 and 03. Conventions are scoped to the area where Codex applies them. |
| `Backend (Java / Spring Boot)` | Updated | Guide 02, Backend coding model. Constructor injection, DTO records, validation, transactions, pagination, and centralized errors remain. Native Oracle read queries are allowed where the current implementation uses justified projections. History is service-based. |
| `Frontend (Angular 19+)` | Updated | Guide 03. Corrected to Angular 21, actual standalone/zoneless/signals patterns, centralized error interception, and route-local architecture. |
| `Database (Oracle)` | Updated | Guide 02, Flyway and schema rules. Keeps naming and constraints, corrects actual column lengths/indexes, and makes applied migrations immutable. |

## Frontend structure and behavior

| Legacy heading | Disposition | Codex destination and adaptation |
|---|---|---|
| `Project Structure — Frontend` | Updated | Guide 03, Current application structure, plus `hospital-frontend/AGENTS.md`. The nonexistent `features/` design is replaced by the actual `core/`, `routes/`, `shared/`, and `theme/` tree. |
| Patient list, form, detail, and history entries in the old tree | Updated | Guide 03, Patient workflows. The guide points to the current route-local components, models, and service rather than inventing feature modules. |
| Appointment, diagnosis, prescription, and room feature folders in the old tree | Historical only | Guide 03, Known frontend boundaries. Their tables or read data may exist, but the listed Angular feature folders do not. |

## Local development, commands, and completion rules

| Legacy heading | Disposition | Codex destination and adaptation |
|---|---|---|
| `Local Development Setup` | Updated | `README.md` remains command truth; guide 04 explains safe usage and troubleshooting without machine-private settings. |
| `Database — Oracle 23ai Free` | Updated | Guide 04, Database preparation. Oracle Free/PDB concepts remain; the personal ZeroTier address, local Oracle home, and service names are not project policy. |
| `Application database user` | Updated | Guide 04. Keeps the dedicated `HOSPITAL` schema approach and warns that `setup-hospital-user.sql` drops the user with `CASCADE`, so it is a destructive reset operation. |
| `Connection env vars (per PowerShell session)` | Retained and sanitized | Guide 04. Keeps `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` with portable placeholders and documents the current fallback. |
| `Common gotchas` | Updated and expanded | Guide 04, Troubleshooting. Keeps listener, PDB/CDB, quoted Flyway history, and console-encoding issues; adds credentials, ports, proxy, backend availability, and fallback details. |
| `Common Commands` | Updated | Guide 04 and nested `AGENTS.md` files. Uses Windows wrapper commands, `npm start`, non-watch tests, `npm run build`, read-only lint, report build, and current component paths. Removes obsolete `--prod`. |
| `Backend` command comments | Updated | Guide 04, Backend commands. |
| `Frontend` command comments | Updated | Guide 04, Frontend commands. |
| `Rules` | Split and updated | Root/nested definitions of done plus guides 01, 04, and 05. Verification, Conventional Commits, progressive reporting, English LaTeX, and simplicity remain. The old unconfigured 70% coverage target is not treated as a build gate. |

## Report writing

| Legacy heading | Disposition | Codex destination and adaptation |
|---|---|---|
| `Report Writing Guidelines` | Updated and expanded | Guide 05 and `docs/report/AGENTS.md`. |
| `Report Structure` | Retained and aligned | Guide 05, Chapter ownership. It follows the six actual chapter files and distinguishes context, design, implementation, measured evaluation, and conclusion. |
| **Style Rules (STRICT — apply to every `docs/report/*` edit)** | Updated | Guide 05, Evidence and writing rules. Whole or long listings remain prohibited; a short verified excerpt of roughly 10–20 lines is allowed only when it materially supports analysis, resolving the legacy internal contradiction. |
| `How to Write Each Content Type` | Retained and clarified | Guide 05. External claims stay brief and cited; original project decisions receive detailed, evidence-backed treatment. |
| `Content Mapping: What to Cite vs What to Detail` | Updated | Guide 05. Official/primary sources are preferred. External claims need citations wherever they occur; project facts need code, test, screenshot, or measurement evidence rather than invented citations. |
| `Chapter 2 — Cite` | Retained | Guide 05, Chapter 2 and citation rules. |
| `Chapter 3 — Detail` | Updated | Guide 05. Removes claims that dynamic JPA Specifications, entity listeners, or enforced transition rules exist unless implementation evidence is added. |
| `Chapter 4 — Detail` | Updated | Guide 05. Describes verified backend/frontend/integration reality and the service-based audit flow. |
| `Chapter 5 — Detail` | Updated | Guide 05. Test tools and numerical results must come from fresh execution; legacy duplicate-email and transition-edge-case examples cannot be claimed without tests. |
| `Diagram Conventions` | Updated and expanded | Guide 05. Applies to current Mermaid sources in `docs/uml/` and `docs/erd/`, preserves reader-focused simplification, and removes reliance on the stale per-diagram audit draft. |
| `Citation Style` | Updated | Guide 05. IEEE numbered presentation remains; implementation is corrected to `biblatex` with Biber. |
| `Language & Formatting` | Retained and clarified | Guide 05. English, professional tables, cross-references, annotated screenshots, and short excerpts remain. |
| `No interrupting parentheses or quoted asides in prose` | Retained | Guide 05, Prose mechanics, including the exceptions for references, acronym definitions, citations, and mathematical notation. |
| `Narrative Voice — first person in chapters about author's work` | Retained | Guide 05. Chapters 1–3 are impersonal; Chapters 4–6 use `I` for author actions and software subjects for runtime behavior; never use `we`. |
| `Incremental Writing Workflow — IMPORTANT` | Retained | Root `AGENTS.md`, guide 05, and report `AGENTS.md`. Behavior, design, and measured evidence changes require the matching chapter and progress-log update unless explicitly excluded. |
| `Report Location & Format` | Updated | Guide 05. Keeps `docs/report/` and LaTeX, aligns the tree to actual files, and removes the personal absolute path. |
| `LaTeX conventions` | Updated | Guide 05. Correct pipeline is XeLaTeX, Biber, XeLaTeX, XeLaTeX. The report uses `biblatex`, not `\bibliographystyle{IEEEtran}` plus BibTeX. |
| `Mapping: Work done → Report section to update` | Retained and updated | Guide 05, Work-to-report map. “Feature module” becomes route/feature implementation; code examples become optional short verified evidence. |
| Incremental prompt template | Converted from Claude prompt to Codex checklist | Guide 05, Incremental update workflow. Tool-specific wording was removed while all required outputs remain. |

## Reference-data sources

| Legacy heading | Disposition | Codex destination and adaptation |
|---|---|---|
| `Data Sources (for seed/reference data)` | Updated | Guide 04, Reference-source catalogue and Import-to-migration workflow. Sources are candidates, not automatically trusted dependencies. |
| `How to use` | Updated | Guide 04. Inspect local assets, record provenance/license/version, normalize safely, generate a new Flyway migration, and verify it. `data/seed/` is not the shipped source. |
| `ICD-10 Diagnosis Codes` | Updated | Guide 04. WHO and CMS remain preferred official sources; ICD10Data is labeled third-party; local year-specific CSV paths replace the nonexistent planned path. |
| `Medications` | Updated | Guide 04. WHO Essential Medicines and authoritative Vietnamese references are preferred; Kaggle is third-party/structure-only and requires license review. |
| `Hospital Departments` | Retained as synthetic-design guidance | Guide 04. Large Vietnamese hospitals may inform categories, but seed names/codes are synthetic and no unverified claim about a hospital is copied. |
| `Room Types` | Retained | Guide 04 and shipped lookup migration. Existing migrations define the values used by the application. |
| `Patient Sample Data (structure reference only)` | Retained with stronger privacy boundary | Guides 01 and 04. Third-party patient datasets are structure-only; project records must be newly generated fake Vietnamese data. |
| `Hospital Management DB References (schema design only)` | Retained as historical candidates | Guide 04. GitHub examples may inspire comparison, never replace the proposal/migrations, and require license/provenance review before reuse. |

## Important corrections made during migration

The following legacy statements remain available in the backup but are
deliberately not active instructions:

| Legacy statement | Verified Codex guidance |
|---|---|
| Angular 19+ and a `features/` tree | Angular 21 and the actual `core/routes/shared/theme` structure |
| Patient history through `@EntityListeners` | Manual before/after comparison in `PatientServiceImpl` inside the update transaction |
| V12 trigger creates field history | V12 only stamps created/updated dates |
| All status flags are soft-delete flags | Doctor/medicine/ICD/lookup flags are reference availability; room flag represents room availability in shipped data |
| The arrow diagram proves allowed patient transitions | Only status values 0–3 are enforced today |
| Department filtering exists on patient list | Current filters are search field, status, and gender |
| Supporting CRUD APIs are implemented | Only patient, medical-record, and statistics endpoints are active |
| Duplicate username/email handling is complete | There is no patient username; current uniqueness is for nonblank identity number |
| Native SQL should always be avoided | Native Oracle projections are currently used for medical-record and statistics reads and are allowed when justified |
| Personal ZeroTier IP, Oracle home, and password location are project setup | Portable placeholders and `README.md` are the shared setup contract |
| `setup-hospital-user.sql` is harmless one-time setup | It performs `DROP USER HOSPITAL CASCADE` and is a destructive schema reset |
| `ng build --prod` | `npm run build` or `ng build --configuration production` |
| JaCoCo has a mandatory 70% threshold | Reports are generated, but no minimum gate is configured; report only fresh measured coverage |
| BibTeX with `IEEEtran` | `biblatex` with Biber and IEEE-style numbered output |
| Exactly 12 Mermaid diagrams, including archive deletion | Inspect current `docs/uml/` and `docs/erd/`; permanent-delete diagrams replace archive assumptions |
| A prior PDF/log proves the report is clean | Run the current build and inspect unresolved references, citations, figures, and errors |

## Completeness rule for future changes

When an upstream requirement or implementation change makes one of these
entries stale, update:

1. the relevant current guide;
2. the nearest applicable `AGENTS.md` if routing or an always-loaded invariant
   changes;
3. this migration map if the disposition of a legacy section changes; and
4. the report and progress log when project behavior, design, or evidence
   changes.

Do not edit the backed-up original to make the comparison appear cleaner.
