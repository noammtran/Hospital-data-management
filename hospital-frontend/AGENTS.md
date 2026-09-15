# Frontend guidance

These instructions apply under `hospital-frontend/`.

Before making a frontend implementation decision or edit, read the repository-root
`.agents/guidance/README.md` and `.agents/guidance/03-frontend.md`. Also read
guide 01 for patient behavior or proposal compliance, guide 02 for an API or
validation contract, guide 04 for setup or commands, and guide 05 when changed
behavior or evidence must be reflected in the report.

## Angular conventions

- Target the installed Angular 21 version and preserve the standalone-component
  setup.
- Follow the actual layout under `src/app/routes`, `src/app/core`,
  `src/app/shared`, and `src/app/theme`; do not create a parallel feature-module
  architecture.
- Keep route-specific models and HTTP services next to their route. Components
  should not call `HttpClient` directly.
- Use relative `/api` URLs so `proxy.config.js` continues to route development
  traffic to the backend.
- Use reactive forms and keep client validation aligned with backend request
  constraints. Surface backend field errors without discarding the existing
  response-envelope handling.
- Preserve the Angular Material and Matero visual language and existing responsive
  patterns.
- Authentication uses the backend login, signed bearer token, current profile,
  and permission guards. Do not reintroduce fake login or client-selected roles.
- Keep Patient self service on `/my-record` and call only
  `GET /api/v1/patient-portal/me`; the client must not accept, store in the URL,
  or submit a patient ID for this read. Redirect a Patient whose profile says
  `mustChangePassword` to `/auth/change-password` before activating the main
  layout. The self-service page stays read-only and shows one unified record list.
  Completed episodes may show diagnoses, prescriptions, conclusions, and the
  latest published service revision after doctor review. Ongoing clinical data,
  drafts, internal notes, handoffs, other-patient navigation, history, statistics,
  and write controls remain excluded.
- Keep Patient, Statistics, Activity history, and Staff accounts filters
  reactive. Every valid change waits 300 milliseconds, returns to page one,
  cancels any superseded request, and reloads with the latest criteria. Keep
  only Reset or Clear and do not add an Apply action.
- Keep Activity history filters server-side with optional Performed by actor,
  keyword, action, entity type, From date, and To date criteria. Clear resets
  the list to page one through the same reactive flow, later page changes
  preserve the latest valid criteria, and the browser rejects a reversed date
  range. Label the actor control
  Performed by with placeholder `Username or full name`. Enforce the backend
  limits of 100 characters for actor and 200 for keyword. Use text buttons
  without icons for these actions.
- Keep the Activity history list columns as Time, Event, Target, Performed by,
  Change summary, and Details. Details opens a text-only View details dialog.
  Show snapshot names before technical identifiers, show the admission patient
  as secondary subject context, and keep the equivalent mobile card layout.
- Load event details from `GET /api/v1/audit-logs/{id}`. Render create, update,
  delete, bulk-delete, status-change, and password-reset details according to
  their data shape. Show protected values without revealing their stored marker,
  and show explicit compatibility text when a legacy event lacks child details.
- Prefix a sample event label with `Sample ·`. Its dialog must state that the
  row is a demonstration record and show Sample data as its data source. Do not
  present a sample row as operational activity.
- If an Activity history request fails, clear stale rows and the total before
  presenting the error state so old data is not mistaken for a new filtered
  result.
- When a patient is referenced in another screen, selector, table, diagnosis,
  prescription, appointment, or report, show the readable patient ID together
  with the full name. Keep the remaining personal and medical information in the
  patient detail view rather than overloading compact references.
- Do not derive user-facing Activate/Deactivate controls from backend
  `*_status_flag` fields unless the user explicitly adds such a feature.

## Verification

Run commands from `hospital-frontend/`:

```powershell
npm test -- --watch=false
npm run build
npx ng lint
```

The repository lint scripts include `--fix`; use `npx ng lint` for a read-only
check unless the task explicitly includes formatting fixes.
