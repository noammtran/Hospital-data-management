---
name: hospital-crud
description: Implement and verify end-to-end CRUD vertical slices in the Hospital Patient Management System. Use when adding or changing patient create, read, update, delete, bulk-delete, validation, search/filter, change-history, statistics, or medical-record behavior across the Spring Boot backend, Angular frontend, Oracle migrations, tests, and LaTeX report.
---

# Hospital CRUD

Implement the smallest complete vertical slice that preserves the repository's
existing contracts and proposal requirements.

## Workflow

1. Read the applicable `AGENTS.md` files and inspect the current flow before
   proposing changes.
2. Define the user-visible behavior and acceptance checks. Confirm that it does
   not contradict the internship proposal.
3. Trace the existing slice through migration, entity, repository, service,
   controller, DTOs, frontend model, HTTP service, component, template, and tests.
   Include only layers affected by the requested behavior.
4. Implement backend rules and API contracts first when the contract changes,
   then align the frontend.
5. Add focused regression tests, run the narrow checks while iterating, and finish
   with the verification commands required by the scoped `AGENTS.md`.
6. Update the relevant LaTeX chapter and `docs/report/progress-log.md` when the
   completed behavior, design, schema, or test evidence changed.

## Backend rules

- Keep controllers thin and map through request and response DTOs.
- Put invariants, duplicate checks, and orchestration in services.
- Preserve `ApiResponse`, `PageResponse`, exception mapping, `/api/v1`, and the
  readable patient identifier unless the task explicitly changes a contract.
- Keep `spring.jpa.open-in-view=false` in mind when fetching and mapping data.
- Add a new Flyway migration for schema changes and use Oracle-compatible SQL.
- Preserve field-level patient history for edits to audited data.

## Frontend rules

- Follow the existing standalone route/component structure.
- Keep HTTP access in route services and use relative `/api` URLs.
- Align reactive-form validation with backend request validation.
- Preserve backend field errors and confirmation behavior for destructive actions.
- Match the existing Angular Material and Matero UI instead of introducing a new
  component system.

## Data and scope safeguards

- Use fake Vietnamese personal data. Use `data/reference/` for medical reference
  values when available.
- Do not add appointment or room management to the conceptual core by default.
- Do not remove appointments, rooms, or admission-room data from the physical
  schema without an explicit request and end-to-end impact verification.
- Do not introduce authentication, soft-delete-only behavior, or speculative
  enterprise infrastructure as part of an unrelated CRUD task.

## Skill routing

- Use `$angular-developer` for Angular-version-specific implementation details.
- Use `$spring-boot` and `$jpa-patterns` for focused Spring or persistence issues.
- Use `$database-designer` for non-trivial schema or query design.
- Use `$latex-paper-en` only when direct report editing or compilation is part of
  the task.

## Completion report

Summarize the completed behavior, files or layers changed, checks run and their
results, and any remaining limitation. Never report a check as passed when it was
not run.
