# Report and diagram guidance

Read this guide before editing the LaTeX report, bibliography, screenshots,
Mermaid or ERD sources, rendered figures, report progress log, or report build
workflow. It is the Codex-oriented replacement for the report-writing,
diagram, citation, incremental-writing, and report-format sections of the
former `CLAUDE.md`.

This guide records durable policy and a verified map of the current repository.
Reinspect the actual source before editing because report prose, screenshots,
rendered figures, progress entries, and historical design notes can become
stale independently.

## Authority and evidence order

Use this precedence for report claims:

1. `docs/ref/1.3. Annex 3 - Internship proposal 2026.pdf` is the binding
   functional specification.
2. Running code, Flyway migrations, tests, and fresh command output establish
   implementation facts.
3. The actual LaTeX sources establish what the report currently says.
4. The nearest applicable `AGENTS.md` and this guide establish writing and
   workflow policy.
5. Historical plans, progress-log entries, old rendered output, and backed-up
   Claude instructions are context only.

Never make the report look internally consistent by hiding a proposal-versus-
implementation gap. State the verified limitation or fix the implementation
when the user has requested that work.

Do not fabricate:

- functionality, constraints, transitions, roles, or security behavior;
- measurements, test counts, coverage, performance, or build results;
- screenshots, diagrams, data provenance, or implementation challenges;
- citations, bibliographic metadata, quotations, or source conclusions.

Every material factual claim must be traceable to the proposal, current source,
a fresh test or build result, a captured screenshot, or a cited external
source.

## Current report layout

The report is an English LaTeX project under `docs/report/`:

```text
docs/report/
|-- AGENTS.md
|-- main.tex
|-- build.ps1
|-- references.bib
|-- progress-log.md
|-- test-results.md
|-- README.md
|-- chapters/
|   |-- 01-introduction.tex
|   |-- 02-literature-review.tex
|   |-- 03-design.tex
|   |-- 04-implementation.tex
|   |-- 05-testing.tex
|   |-- 06-conclusion.tex
|   `-- business-process-draft.md
|-- figures/
`-- build/
```

`business-process-draft.md` is historical planning material, not an
implementation source of truth. `build/` contains generated output and cannot
prove that the current source still compiles.

`main.tex` currently:

- uses the `report` class with `12pt`, A4, one-sided layout;
- includes the six chapter files listed above;
- compiles with XeLaTeX and uses `fontspec`;
- searches for figures only under `figures/`;
- declares graphics extensions in this order:
  `.png`, `.pdf`, `.jpg`, `.jpeg`;
- provides `booktabs`, `tabularx`, `longtable`, `array`, and `caption` for
  tables and captions;
- loads `listings`, but this does not make code listings mandatory;
- uses `hyperref` and `cleveref` for links and cross-references;
- uses `acronym` for the abbreviation list;
- uses `biblatex` with `backend=biber`, `style=ieee`, and `sorting=none`;
- adds `references.bib` with `\addbibresource`;
- prints the bibliography with `\printbibliography`.

Do not replace this with legacy BibTeX commands, `IEEEtran` bibliography style,
PDFLaTeX, or PlantUML conventions unless the user explicitly requests a
toolchain migration.

## Chapter ownership

Each chapter has a distinct job. Preserve this ownership and use
cross-references instead of repeating the same explanation.

| Chapter | Ownership | Expected emphasis |
|---|---|---|
| 1. Introduction | Problem context, motivation, objectives, scope, and a brief methodology overview | Define what problem the project addresses and what the internship set out to deliver. Summarize the delivered authentication, admission, and audit extension without implementation detail. Keep technology detail out of this chapter. |
| 2. Literature Review | Brief externally sourced technology and architectural background | Cover only technology actually used. Cite primary or official sources and emphasize why it was suitable for this project rather than teaching the technology. |
| 3. System Analysis and Design | Requirements, use cases, logical architecture, conceptual data model, API design, validation rationale, status representation, authentication and authorization design, admission write rules, both audit layers, and other design choices | Explain why the design was chosen. Define the role and permission boundary, the distinction between patient field history and global business activity, and the conceptual-versus-physical schema boundary. Keep this chapter at design level and do not turn it into a walkthrough of annotations, classes, or source files. |
| 4. Implementation | Verified implementation reality across backend, frontend, Oracle and Flyway, authentication, permission enforcement, patient status transitions, admission create, edit, completion, and delete flows, both audit mechanisms, permanent patient deletion, medical-record views, and implemented statistics | Explain how the design appears in the running system and support important points with verified screenshots, diagrams, tables, or short excerpts. Include transaction behavior and the route or endpoint that exposes a delivered capability when material. |
| 5. Testing and Evaluation | Test strategy, test layers, expected-versus-observed cases, permission boundaries, admission rules, audit success and rollback behavior, edge cases, measured results, coverage, gaps, and evaluation | Use fresh evidence. Distinguish automated, integration, and manual verification. Never carry old counts or coverage forward without rerunning the relevant checks. |
| 6. Conclusion | Verified summary, achievements, limitations, lessons, and realistic future work | Distinguish delivered status and admission-completion behavior from future transfer, audit export, and tamper-evident logging work. Do not claim that every proposal item is complete when code or tests show a gap. Future work must not be described as delivered behavior. |

### Two audit layers

Keep the delivered audit mechanisms separate in every chapter, figure, caption,
table, and test description.

- `PATIENT_HISTORY` is per-patient field history. A patient update compares the
  before and after snapshots and stores one row for each changed field with the
  old value, new value, actor, and time. The per-patient history popup continues
  to read this layer.
- `AUDIT_LOGS` is the global business-event header. It records one event for each
  successful patient create, update, delete, or bulk-delete operation, admission
  create, update, or delete operation, and staff-account create, update,
  status-update, or password-reset operation. `AUDIT_LOG_CHANGES` stores ordered
  typed field differences and `AUDIT_LOG_TARGETS` stores ordered targets for
  multi-record events. The Administrator-only Activity history page reads this
  layer through the list and detail audit endpoints under `VIEW_AUDIT_LOG`.

Reads, search, filtering, pagination, navigation, login, logout, and other
non-writing interactions do not create business activity events. The business
event participates in the domain write transaction, so a failed write leaves no
success event. Version 2 events snapshot actor, resource, optional subject, and
a concise change summary. The summary must not repeat the event name
or target label. Sample provenance must visibly distinguish synthetic V16 and
V18 rows from operational activity. Classified personal and medical change values are redacted. Secret
changes are omitted. Audit data must never contain a password, password hash,
token, or another secret. Version 1 rows remain readable as legacy events and
must not be presented as though missing details were reconstructed.

A successful patient update with no tracked value difference still records a
successful `UPDATE` event with a zero change count. The patient field-history
layer adds no row for that request. Do not call this an attempted event because
the mutation request completed successfully.

Sample rows must never be described as operational evidence.

Map these facts by chapter. Chapter 1 states the delivered scope briefly.
Chapter 3 owns the rationale, event boundary, permissions, action and entity
scope, retention design, query filters, and conceptual-versus-physical
distinction. Chapter 4 owns the verified services, transactions, migration,
filtered endpoint, route, and user interface. Chapter 5 owns permission,
event-content, ordering, exclusion, rollback, filter, API, and frontend
evidence. Chapter 6 owns the verified achievement, current limitations, and
future export and tamper-evident retention work.

### Verified design topics for Chapter 3

When relevant to the current task, Chapter 3 should explain:

- the proposal-derived patient CRUD, search, filtering, detail, validation,
  bulk-delete, and field-history requirements;
- why a small layered MVC-style application suits this internship scope;
- the readable check-letter patient identifier generated by
  `PatientIdGenerator`, not the earlier UUID design;
- the distinction between stable patient identity data and treatment episodes;
- the lookup-value pattern and reference-data availability flags;
- the conceptual ERD, relationships, constraints, and the deliberate
  conceptual-versus-physical schema boundary;
- the meanings `0 = Waiting`, `1 = Examining`, `2 = Admitted`, and
  `3 = Discharged`;
- the `/api/v1` resource shape and response conventions;
- frontend and backend validation as two complementary checks;
- the current multi-criteria search and filtering design;
- why per-patient field history is retained and why it remains separate from
  global action-level business activity;
- database-backed authentication, four staff roles plus the Patient role, the
  generated one-time credential, and operation-level permissions enforced
  independently by the server;
- why the Patient self-service endpoint accepts no patient ID and resolves the
  one-to-one account link on the server before returning only that profile and
  its unified examination/treatment records;
- why Doctor and Administrator can create and edit an active admission while
  only Administrator can permanently delete one;
- why successful patient, admission, and staff-account writes create business
  activity events while non-writing interactions do not;
- why Activity history filters are applied on the server before pagination and
  how optional actor, keyword, action, entity type, and date criteria combine;
- why snapshot names remain stable after resource deletion and why an admission
  resource can carry a separate patient subject;
- why ordered child rows represent field changes and bulk-operation targets;
- why sensitive values are redacted or excluded and why legacy rows use an
  explicit compatibility fallback;
- why stored sample provenance distinguishes demonstration rows from operational
  evidence in both the list and details dialog;
- why `AUDIT_LOGS` retains actor and target text without foreign keys to a staff
  account or domain row.
- why leading-wildcard search still needs representative Oracle plan and load
  evidence before enterprise-scale claims;
- why no public mutation endpoint does not make the tables append-only or
  tamper-evident and why production needs restricted grants and retention;
- why source and isolated tests do not prove V17 and V18 migration or rollback
  behavior on live Oracle.

Do not describe the four patient status values as an enforced transition state
machine. The current implementation validates the value range but does not
enforce a fixed transition order.

Do not prescribe `@EntityListeners` or `@PreUpdate` for field history. The
current implementation takes before-and-after snapshots in
`PatientServiceImpl`, compares fields, and persists changed entries through the
history repository. Describe the verified design at the appropriate chapter
level.

### Verified implementation topics for Chapter 4

When relevant to the current task, Chapter 4 may cover:

- backend and frontend bootstrap plus database wiring;
- entities, request and response DTOs, mapping, services, controllers, and
  centralized exception handling;
- the Angular standalone component structure, reactive forms, HTTP integration,
  dialogs, and list behavior;
- patient identifier generation and duplicate identity-card validation;
- search, combined filters, response pagination, and integration flow;
- service-level field comparison and history persistence;
- database-backed login, token validation, method-level permission checks, and
  Administrator account management;
- transactional Patient-account creation/synchronization/deletion, forced first
  password replacement, and the read-only `/my-record` route;
- admission create and active-admission edit popups, transactional room
  occupancy handling, and Administrator-only permanent deletion;
- action-level business activity recording inside the patient, admission, and
  staff-account write transactions;
- the Administrator-only `/audit-log` route and paginated, server-filtered
  `GET /api/v1/audit-logs` endpoint;
- the Activity history list, text-only View details action, and
  `GET /api/v1/audit-logs/{id}` detail response with ordered changes and targets;
- versioned event snapshots, sensitive-value handling, and legacy fallback;
- sample provenance, visible demonstration labels, and runtime events that are
  always operational;
- permanent single and bulk deletion with explicit UI confirmation;
- medical-record and statistics read views, limited to what is implemented and
  tested.

Name a framework class, annotation, or source artifact only when it helps
explain verified implementation. The report should explain the project's
behavior and decisions rather than serve as framework documentation.

## External content, original work, and evidence

Use two different levels of detail.

### External content

External technology, standards, and established architectural ideas should:

- normally take no more than one or two focused paragraphs per technology;
- cite primary, official, standards, or original research sources;
- explain why the project selected the technology;
- be paraphrased in the author's own words;
- avoid tutorials, generic feature inventories, and copied documentation.

Every externally described technology in Chapter 2 needs at least one suitable
citation. Determine the technology list from current dependencies and source,
not from an old fixed checklist. The current report includes topics such as
Spring Boot, Maven, DevTools, Flyway, JPA, Hibernate, Spring Data JPA, Jakarta
Validation, Oracle, MVC, and REST. Reverify that list when dependencies change.

### Original project work

The project's requirements interpretation, design, implementation, and testing
should be detailed enough to demonstrate understanding. Explain:

- the decision and the problem it addresses;
- the alternatives or trade-off when that is material;
- how the decision appears in the verified implementation;
- the supporting diagram, screenshot, table, test, or measured result;
- any genuine limitation or unresolved compliance gap.

Do not add a citation merely to make an original project decision appear more
academic. Cite any external factual premise used in the reasoning, wherever it
appears, but support implementation claims with repository evidence.

## Citation and bibliography rules

- Preserve IEEE numbered citation output through the existing `biblatex` and
  Biber configuration.
- Add or update entries in `docs/report/references.bib`.
- Cite with the existing LaTeX citation commands. Do not type reference numbers
  such as `[4]` manually.
- Prefer official documentation, specifications, standards, original papers,
  and authoritative institutional sources over blogs and tutorials.
- Verify author or organization, title, year, URL or DOI, and other metadata
  before adding an entry.
- Do not cite a source that has not been inspected.
- Do not cite a source for a claim it does not support.
- Do not copy source prose. Paraphrase it and keep any necessary quotation
  short.
- External claims in Chapters 3 through 6 still require citations. The chapter
  number does not turn an external claim into original work.
- Project-specific design and implementation facts do not require invented
  citations, but they do require verification against the repository.

## Writing style

### Language and narrative voice

- Write the report in English.
- Chapters 1 through 3 use an impersonal voice for the problem, background,
  requirements, and design.
- Chapters 4 through 6 use first-person singular when describing the author's
  decisions, implementation work, experiments, tests, or learning.
- Use `the system`, `the application`, `the client`, `the API`, or
  `the backend` when the subject is runtime software behavior.
- Never use first-person plural `we` or `our` for this single-author report.
- Keep the chosen voice consistent within a passage.

### Behavior before framework mechanism

Explain what the application guarantees, why the design exists, and what the
evidence shows. Avoid prose that merely translates annotations or library calls
into English.

For example, prefer a statement that duplicate identity-card submissions are
rejected over an explanation of what a uniqueness annotation means. In Chapter
4, it is acceptable to name the verified service, migration, annotation, or
query when the implementation mechanism is itself important.

### Code and technical excerpts

- Never paste a whole source file or a long Java, TypeScript, SQL, JPQL, JSON,
  configuration, console, or HTTP-response listing.
- Use a short, verified excerpt only when it materially supports the
  explanation and a diagram, table, or prose description would be less clear.
- Keep an excerpt within 10 to 20 lines. Prefer the shorter end of that range.
- Introduce the excerpt, explain the relevant lines, and identify the verified
  source. Do not leave an excerpt to explain itself.
- Use monospace formatting for short identifiers and endpoint paths.
- The presence of the LaTeX `listings` package is support for exceptional short
  excerpts, not a requirement to include code.

### Prose punctuation and asides

- In narrative paragraphs, do not use render-visible round parentheses, colons,
  or semicolons. Restructure the sentence instead.
- Do not emphasize paragraph prose with `\emph`, `\textit`, or `\textbf`.
  Keep paragraph text in normal roman style.
- This restriction applies to narrative paragraphs only. It does not prohibit
  LaTeX command syntax, labels, headings, captions, tables, bibliography
  entries, or other non-paragraph structures.
- Enforce this rule only in paragraphs touched by the current task. Do not run
  a document-wide cleanup or rewrite unrelated paragraphs unless the user asks
  for it explicitly.
- Avoid interrupting explanations in parentheses. Rewrite them as complete
  sentences when they carry substantive content.
- Avoid quotation marks around identifiers, example strings, error messages,
  or technical terms. Use `\texttt{...}` for identifiers where appropriate.
- Avoid em-dash asides. Prefer a separate sentence.
- Parentheses remain valid for cross-references, first-use acronym expansion,
  citations, and mathematical or numerical notation.
- Preserve meaningful LaTeX punctuation and escaping.
- Keep terminology, capitalization, tense, and acronym use consistent.

### Duplication and cross-references

- Do not repeat design rationale from Chapter 3 in Chapters 4 or 5.
- Do not repeat implementation walkthroughs in Chapter 5.
- Do not reproduce table contents in adjacent prose.
- Refer to the owning section, figure, or table with the existing
  `\Cref`, `\cref`, label, and acronym conventions.
- Preserve existing labels whenever possible. Update every reference if a label
  must change.

## Figures, screenshots, and tables

- Store report-consumed figures in `docs/report/figures/`.
- Give every figure and table a useful caption and a stable label.
- Introduce each figure or table in the prose and explain what the reader should
  learn from it.
- Use cross-references rather than positional phrases such as `the figure
  below`.
- A diagram should normally have one or two explanatory sentences in its
  owning section.
- UI screenshots must come from the current interface and use fake data.
- Use numbered callouts when a screenshot contains several controls or states
  that need separate explanation. Do not add decorative annotations that carry
  no information.
- Check that text remains legible at the final PDF size.
- Use tables for comparisons, field summaries, endpoint inventories,
  expected-versus-observed test cases, and measured results when a table is
  clearer than prose.
- Use the existing professional table packages and avoid tables that overflow
  the page.

## Work-to-chapter mapping

Update report content in the same task when behavior, design, schema, or
measured evidence changes, unless the user explicitly excludes report work.

| Completed work | Report destination |
|---|---|
| Project setup or a material stack change | Chapter 1 scope or methodology when relevant, and Chapter 4 project setup |
| Schema, constraint, index, sequence, trigger, or seed migration | Chapter 3 database design and Chapter 4 database implementation when material |
| Backend entity, DTO, mapper, repository, service, or controller vertical slice | Chapter 4 backend implementation |
| Angular standalone screen, component, form, dialog, route, or service | Chapter 4 frontend implementation |
| New or changed API endpoint | Chapter 3 API design table and Chapter 4 implementation |
| Frontend and backend validation behavior | Chapter 3 validation rationale and Chapter 4 verified implementation |
| Search, filtering, or pagination behavior | Chapter 3 design and Chapter 4 end-to-end implementation |
| Staff authentication, role permissions, or account management | Chapter 1 delivered scope when material, Chapter 3 security and authorization design, Chapter 4 verified implementation, Chapter 5 permission evidence, and Chapter 6 limitations when relevant |
| Patient status transitions and admission create, active edit, completion, or permanent delete | Chapter 1 delivered scope when material, Chapter 3 requirements and permission design, Chapter 4 workflow controls and transaction implementation, Chapter 5 behavioral and authorization tests, and Chapter 6 future transfer limits |
| Per-patient field history in `PATIENT_HISTORY` | Chapter 3 field-history rationale, Chapter 4 before-and-after comparison and persistence, and Chapter 5 field-change evidence |
| Global business activity in the audit event tables | Chapter 1 delivered scope when material, Chapter 3 event boundary, snapshots, retention, changes, targets, filtering, and legacy design, Chapter 4 transaction, migrations, API, list, and details dialog, Chapter 5 permission, content, ordering, filtering, redaction, compatibility, exclusion, and rollback evidence, and Chapter 6 current limits and future export |
| Permanent single or bulk deletion | Chapter 3 requirement/design and Chapter 4 implementation and confirmation flow |
| Medical-record or statistics behavior | Chapters 3 and 4, limited to delivered scope |
| Unit, controller, integration, or frontend tests | Chapter 5 strategy, cases, and fresh results |
| Coverage or other measured evaluation | Chapter 5 with the command, date, environment, source artifact, and current value |
| A material bug or refactor that reveals a design lesson | Chapter 4 when it explains implementation, or Chapter 6 when it establishes a genuine limitation or lesson |
| A verified unresolved scope or compliance gap | Chapter 6 limitations or future work, and the relevant earlier section when needed for accuracy |

Do not document every small refactor or debugging step. Include work that helps
the reader understand the design, implementation, evidence, or limitation.

## Incremental report workflow

After a material implementation or report change:

1. Inspect the current implementation, relevant tests, chapter, labels,
   figures, citations, and recent progress entries.
2. Update the chapter that owns the content.
3. Update or capture current figures and screenshots when the visual behavior
   changed.
4. Add verified bibliography entries only when new external claims require
   them.
5. Append a dated, factual entry to
   `docs/report/progress-log.md`.
6. Refresh `docs/report/test-results.md` when measured evidence or a verification
   limitation changes.
7. Build and inspect the report when the local toolchain is available.
8. State any verification that could not run and the concrete reason.

The progress log is historical. Append a new entry rather than rewriting old
entries to make earlier observations look current. An old `pending` note is not
proof that the work is still pending; inspect the current code and report.

## Current Mermaid and ERD source inventory

At the 2026-08-21 audit reconciliation, the repository contains 25 Mermaid
source files in two directories. Recount and inspect them before relying on
this inventory.

`docs/uml/` contains 21 sources:

```text
activity-examination.mermaid
architecture.mermaid
audit-log-scope.mermaid
class-domain.mermaid
db-wiring.mermaid
erd-clinical.mermaid
erd-grouped.mermaid
erd-scope.mermaid
erd-security.mermaid
lookup-dereference.mermaid
seq-create-admission.mermaid
seq-create-patient.mermaid
seq-delete.mermaid
seq-edit-admission.mermaid
seq-edit-audit.mermaid
seq-login.mermaid
seq-search-filter.mermaid
state-admission.mermaid
state-patient.mermaid
test-isolation.mermaid
usecase.mermaid
```

`docs/erd/` contains four Mermaid sources:

```text
erd.mermaid
erd-proposed-linked-render.mermaid
erd-proposed-render.mermaid
erd-render.mermaid
```

Do not recreate a concept before checking both directories and the figures
already referenced by the LaTeX chapters. Similar filenames can represent
different abstraction levels, historical proposals, or render intermediates.

## Diagram conventions

Diagrams serve report readers, not developers reading source code.

- A diagram may collapse layers, omit routine fields, and hide edge cases, but
  it must preserve the main actors, relationships, states, and flow.
- Keep the visual focused on one teaching purpose.
- Use the happy path by default in a sequence diagram.
- Use no more than five meaningful sequence participants unless the interaction
  between extra participants is the subject.
- Collapse controller, service, and repository into one `Backend` participant
  when separate lifelines do not teach the reader anything.
- Hide routine getters, setters, and repeated audit fields in a class diagram.
  Explain common audit fields once when needed.
- Show only three to five representative components or tables plus an ellipsis
  in a high-level architecture diagram.
- Do not place proposal IDs, counter increments, assignments, raw SQL,
  annotation names, or other implementation side effects in reader-facing
  labels unless that detail is the explicit subject.
- Keep diagram labels in the report's English terminology.
- Preserve consistency between source, caption, surrounding prose, and the
  implementation claim.
- Apply a ten-second readability check. If a reader cannot identify the purpose
  and main flow quickly, simplify the diagram.

A diagram is explanatory evidence, not implementation evidence by itself. In
particular:

- a state diagram does not prove that transition enforcement exists;
- a login diagram does not prove that backend authentication or authorization
  exists;
- an old archive branch does not override proposal-required and implemented
  permanent deletion;
- a logical class or ER diagram does not prove that every physical column or
  table exists.

Verify those claims against code, migrations, and tests.

## Conceptual and physical ERD boundary

The conceptual ERD explains the patient-record model at a reader-friendly
level. The physical schema is defined by the Flyway migrations and is
deliberately larger.

`APPOINTMENTS`, `ROOMS`, and `admission_room_id` remain in the physical
schema and seed data even though appointments and rooms are outside the current
conceptual clinical ERD. `APP_USERS`, `AUDIT_LOGS`, `AUDIT_LOG_CHANGES`, and
`AUDIT_LOG_TARGETS` are also delivered physical tables outside that clinical
model. Use `erd-security.mermaid` as the focused security and audit storage view,
and use `audit-log-scope.mermaid` to explain the two audit mechanisms. Do not add
staff-account or business-audit tables to the clinical conceptual ERD merely to
make it match the physical schema.

`AUDIT_LOGS` deliberately has no foreign key to `APP_USERS`, `PATIENTS`,
`ADMISSIONS`, or another domain table. This preserves the stored actor and
target text after a staff account or domain record is permanently deleted. Do
not draw a relationship that the migration does not create. Draw the two real
one-to-many relationships from `AUDIT_LOGS` to its ordered child tables.

Use the current migrations to verify physical names, keys, constraints,
sequences, indexes, and triggers. Use the appropriate conceptual source to
explain the report model. Do not infer either from a rendered image alone.

## Rendering workflow and selected image format

`docs/uml/render-all.ps1` renders `docs/uml/*.mermaid` to PDF files under
`docs/report/figures/`. It:

1. prefers the local Mermaid CLI under `docs/uml/node_modules/`;
2. falls back to a globally available `mmdc`;
3. otherwise uses `npx` with `@mermaid-js/mermaid-cli`.

The script does not render the four sources under `docs/erd/`. Render a changed
ERD source explicitly with the same Mermaid CLI or extend the script only when
that change is within the requested scope.

The report currently prefers PNG before PDF when both files share a basename.
This means regenerating only `diagram.pdf` may leave the report displaying an
older `diagram.png`. After changing a diagram:

1. identify the exact basename referenced by LaTeX;
2. identify which extension `main.tex` will select;
3. update the Mermaid source;
4. regenerate the selected output and any deliberately retained archival
   format;
5. build the report;
6. inspect the rendered page for clipping, multi-page output, tiny labels, and
   stale content.

Do not assume that a successful Mermaid command means the LaTeX report consumed
the new image.

## LaTeX build and verification

Run from `docs/report/`:

```powershell
.\build.ps1
```

The exact pipeline is:

```text
XeLaTeX -> Biber -> XeLaTeX -> XeLaTeX
```

The expected output is `docs/report/build/main.pdf`.

For a completed report or diagram change:

- require the build script to exit successfully;
- inspect `build/main.log` and `build/main.blg`;
- treat LaTeX errors, Biber errors, missing figures, unresolved citations, and
  unresolved cross-references as failures;
- investigate new missing-character, overfull, and severe underfull warnings;
- inspect the generated PDF visually, especially changed pages, figure
  legibility, page breaks, captions, numbering, references, and bibliography;
- verify that screenshots match the current UI and contain no real personal or
  medical data;
- record the factual report update in `progress-log.md`.

If XeLaTeX, Biber, Mermaid CLI, fonts, or another required tool is unavailable,
state exactly which check could not run. Do not claim success from source
inspection alone.

## Current stale and dirty-state boundaries

These boundaries were verified during the Claude-to-Codex reconciliation and
must be rechecked rather than assumed permanent:

- `docs/report/` is a separate Git working tree and currently contains existing
  modified and untracked files. Inspect `git -C docs/report status --short`
  before editing. Preserve unrelated user changes and do not clean, reset, or
  overwrite them.
- The current `build/main.pdf` and `build/main.log` were produced on 2026-08-28
  after the structured Activity history V2 reconciliation. The PDF has 77 A4
  pages.
- The current build log has no LaTeX error, Biber error, unresolved citation,
  unresolved reference, missing report figure, missing character, or fatal
  error. It retains 31 underfull and 2 overfull box warnings. The affected pages
  were visually checked without clipped content.
- The newest sections of `docs/report/test-results.md` and
  `docs/report/progress-log.md` are the current handoff record for structured
  Activity history V2. Refresh them whenever relevant behavior, diagrams, or
  measured evidence changes.
- `docs/uml/README.md` and
  `docs/report/chapters/business-process-draft.md` contain historical filenames,
  authentication, role, archive, listener, and transition assumptions. Do not
  use them as implementation evidence.
- `docs/report/README.md` is a setup aid, not a source of truth for active
  LaTeX commands or current figure paths. Verify its claims against `main.tex`
  and `build.ps1`.
- Old screenshots and rendered figures can remain present after their source or
  UI has changed. Verify the exact file consumed by LaTeX.

Do not fix unrelated report prose, generated artifacts, stale planning
documents, or dirty working-tree entries merely because they are discovered
during a focused task. Report material blockers and keep the requested change
coherent.

## Codex skill routing

- Prefer `$academic-research-suite` whenever its academic research, planning,
  drafting, revision, citation-integrity, or reviewer workflow overlaps with
  another available skill. This is an explicit project-owner preference and
  applies in future Codex sessions that load the repository guidance. If an
  older project note, historical workflow, or overlapping skill says otherwise,
  this project preference controls and `$academic-research-suite` is selected
  first.
- Use `$latex-paper-en` for distinct source-level work such as direct editing of
  the existing English LaTeX files, compilation diagnosis, and LaTeX-specific
  formatting when the selected ARS workflow does not provide that operation.
- Use `$paper-audit` instead of the overlapping ARS reviewer only when the user
  explicitly requests `$paper-audit` or needs a narrow read-only submission gate
  that ARS does not cover.
- Continue to use specialist skills for non-overlapping needs, including
  `$latex-thesis-zh` for Chinese theses and `$bib-search-citation` for local
  BibTeX-library search.

The applicable skill supplements this repository guidance. It does not override
the proposal, current implementation, local evidence requirements, or the
nearest `AGENTS.md`.
