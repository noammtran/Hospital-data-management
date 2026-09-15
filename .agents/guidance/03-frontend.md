# Frontend guidance

Read this guide before changing Angular code, routes, components, forms,
Material/Matero UI, frontend HTTP behavior, or frontend tests. It replaces the
frontend stack, frontend project structure, Angular coding conventions, UI
behavior, and frontend command notes in the former `CLAUDE.md`.

Also read:

- the root `AGENTS.md` and `hospital-frontend/AGENTS.md` for always-applicable
  policy;
- `01-project-and-requirements.md` when product behavior changes;
- `02-backend-database-api.md` when an API contract or validation rule changes;
- `04-development-and-data.md` for full-machine setup and troubleshooting;
- `05-report-and-diagrams.md` when behavior or measured results must be reflected
  in the internship report.

The executable frontend sources of truth are:

- `hospital-frontend/package.json` and `package-lock.json`;
- `hospital-frontend/angular.json`;
- `hospital-frontend/tsconfig*.json`;
- `hospital-frontend/src/app/`;
- `hospital-frontend/src/environments/`;
- `hospital-frontend/proxy.config.js`;
- frontend tests under `hospital-frontend/src/**/*.spec.ts`.

This guide is a current map for Codex. Reinspect those files before editing.

## Active frontend stack

The lockfile currently resolves:

| Technology | Resolved version or setting |
|---|---|
| Angular core, common, forms, and router | 21.2.16 |
| Angular CLI and build tooling | 21.2.14 |
| Angular Material and CDK | 21.2.14 |
| `@ng-matero/extensions` | 21.3.1 |
| `ng-matero` | 21.1.0 |
| RxJS | 7.8.2 |
| TypeScript | 5.9.3 |
| Vitest | 4.1.8 |
| Formly | 7.1.0 |
| ngx-translate | 17.0.0 |
| Hot Toast | 6.2.0 |
| date-fns | 4.4.0 |
| Package manager declared by the project | npm 11.12.1 |

The Angular packages accept Node `^20.19.0`, `^22.12.0`, or `>=24.0.0`.
Use the lockfile rather than this snapshot when exact installed versions matter.
Do not upgrade Angular or a UI library as an incidental part of feature work.

The workspace uses the modern `@angular/build:application` builder, SCSS,
production as the default build configuration, and the Angular unit-test
builder with Vitest. It is a browser-rendered application; no SSR, service
worker, or E2E runner is configured.

## Current application structure

```text
hospital-frontend/src/app/
├── app.config.ts               application-wide providers
├── app.routes.ts               top-level routes
├── app.ts                      root component
├── fake-login.service.ts       retained unused legacy mock
├── core/
│   ├── authentication/         API login, JWT session, profile, and permission guards
│   ├── bootstrap/              startup, menu, settings, translation
│   └── interceptors/           API, base URL, error, token, logging, settings
├── routes/
│   ├── audit-log/              administrative Activity history
│   ├── dashboard/              retained template page, not a current route
│   ├── patient/                patient screens, models, and HTTP service
│   ├── sessions/               login and error pages
│   ├── statistics/             filters, result grid, and report dialog
│   └── user/                   administrative staff-account management
├── shared/
│   ├── components/
│   ├── directives/
│   ├── interfaces/
│   ├── pipes/
│   ├── services/
│   ├── utils/
│   └── Formly configuration
└── theme/
    ├── admin-layout/
    ├── auth-layout/
    ├── header/
    ├── sidebar/
    ├── sidemenu/
    ├── topmenu/
    └── widgets/
```

Do not recreate the historical `src/app/features/` hierarchy. Put
route-specific components, models, and services in the matching directory under
`routes/`. Put genuinely application-wide behavior in `core/`, reusable
presentation or utilities in `shared/`, and shell/layout work in `theme/`.

## Routing and application shell

`app.routes.ts` currently defines:

- an authenticated `AdminLayout` at the root;
- a default redirect from `/` to `/patients`;
- lazy standalone routes for `/patients`, `/audit-log`, `/statistics`, and
  `/users`;
- a compatibility redirect from `/patient-history` to `/audit-log`;
- error routes for 403, 404, and 500;
- an `AuthLayout` containing `/auth/login`;
- a wildcard redirect to `/patients`.

The root and child routes use `authGuard`. Feature routes additionally use
`permissionGuard` and a permission value in route data. The menu uses the same
server-issued permissions through `ngx-permissions`, and action controls check
permissions through `AuthService`. These client guards improve navigation and
presentation but do not replace backend authorization. Preserve lazy
`loadComponent()` loading for main feature pages. Add a route to the existing
`Routes` array instead of creating a feature routing NgModule.

The application enables router scroll restoration and component input binding.
Keep a route's public URL stable unless the task explicitly changes navigation
or a report screenshot that depends on it.

## Angular implementation conventions

- Components are standalone by default in Angular 21. Do not introduce
  NgModules or add redundant `standalone: true` solely for style.
- Use `inject()` for dependency injection. Existing classes may still have a
  constructor for initialization; do not use constructor parameters for DI.
- Use signals for local render state and built-in `@if`, `@for`, and `@switch`
  template control flow.
- Supply a stable `track` expression for `@for`; use `$index` only when rows
  have no stable identity and their order is not independently mutable.
- The application uses `provideZonelessChangeDetection()`. Do not add Zone.js
  assumptions or impose the obsolete blanket rule that every component must
  declare `ChangeDetectionStrategy.OnPush`.
- Existing patient and statistics forms use Reactive Forms. Keep that strategy
  for coherent edits; do not migrate an established form to Signal Forms
  incidentally.
- Keep HTTP calls in injectable services. Components orchestrate view state and
  dialogs but should not call `HttpClient` directly.
- Keep route-specific models beside the route service. Avoid a second, parallel
  global model hierarchy for the same API shapes.
- Use `takeUntilDestroyed()` or an equivalent lifecycle-safe pattern for
  long-lived subscriptions. Finite `HttpClient` requests do not need artificial
  unsubscribe boilerplate.
- Match existing class naming. The lint configuration deliberately does not
  require `Component` or `Directive` suffixes.
- Prefer Angular CLI scaffolding for a new Angular artifact, then adapt the
  generated result to the local route layout.

## HTTP, proxy, and response contracts

Development requests use relative URLs:

- authentication service: `/api/v1/auth/login` and `/api/v1/auth/me`;
- patient service: `/api/v1/patients`;
- audit-log service: `/api/v1/audit-logs`;
- statistics service: `/api/v1/statistics`.

`proxy.config.js` forwards `/api/**` to `http://localhost:8080`. Do not hardcode
the backend host in a component or route service. `environment.ts` and
`environment.prod.ts` currently use an empty `baseUrl`; application interceptors
and relative URLs preserve same-origin production behavior.

Backend success responses use:

```ts
interface ApiResponse<T> {
  success: boolean;
  statusCode: number;
  message: string;
  data: T;
  errors?: Record<string, string>;
  timestamp: string;
}
```

Paginated data uses `items` plus a `pagination` object containing one-based
`page`, `pageSize`, totals, and next/previous flags. Route services unwrap
`res.data`; do not bypass or redefine the envelope for a single screen.

The functional error interceptor handles global HTTP failures, toast messages,
401 navigation to login, and 403/404/500 error pages. A form may additionally
map backend `errors: { field: message }` to controls. Do not swallow the error
before a form can apply those field errors, and do not duplicate a second
global error system in each service.

## Patient list behavior

The patient route is a server-paginated Matero grid:

- external pages are one-based; the grid's zero-based page index is converted at
  the component boundary;
- the default page size is 20 and the offered sizes are 10, 20, and 50;
- search is debounced by 300 ms;
- the user chooses name, phone, or identity-card search;
- status and gender can be combined with the search;
- changing a filter resets the page to 1;
- each row displays patient ID, full name, gender, birth date, phone, and
  patient status;
- row actions open details, edit, permanent delete, field-level history, or an
  authorized next patient-status action;
- the history action is disabled when the row reports `hasHistory = false`;
- row selection enables permanent bulk deletion.

Single and bulk deletion require confirmation and are irreversible. Do not
replace these operations with soft deletion, `status_flag`, or a hidden archive
action. After a mutation, reload the server page so totals and the visible rows
come from the backend.

Editing first fetches the full patient because list rows are intentionally
summary DTOs. Do not populate the edit form from a partial list row.

## Patient create and edit form

Create and edit share `PatientFormDialog`. The form groups:

- personal information;
- medical information;
- health insurance;
- emergency contact;
- notes.

The frontend currently enforces required full name, date of birth not in the
future, email format, an exactly 12-digit identity number for create and linked
records, and field-length limits aligned with the request DTOs. It trims
strings, sends empty optional text as `null`, and sends dates as `yyyy-MM-dd`.
Status is not part of the generic create or edit form.

Identity-card uniqueness and any backend-only validation must still be handled
from the API error envelope. `applyServerErrors()` maps each backend field error
to its matching control. Keep frontend and backend validation aligned. Phone
still has only a length limit, and insurance expiry has no business-date
constraint.

The backend defaults a new patient's status to `0`. Status changes use dedicated
permission-controlled workflow actions and are never submitted by this form.

Successful creation tells staff that the Patient account uses the identity-card
number as its one-time credential and must change that password on first login.

## Patient self-service route

`/my-record` requires `VIEW_OWN_RECORD` and uses a service with only
`GET /api/v1/patient-portal/me`; its client API has no patient-ID argument. The
page is read-only and displays the linked patient's profile and one examination
and treatment record list. Completed records include diagnoses, prescriptions,
conclusions and the latest published service revision after doctor review.
Drafts, internal notes and handoffs stay excluded. It exposes no patient list,
arbitrary patient-ID read, history, statistics or write controls.

`/auth/change-password` is available to every authenticated account from the
profile menu. While `mustChangePassword` is true, it is the only allowed
destination: the application-level guard prevents entry to the main layout
until the password is changed. After a successful change, the stored access
token is removed and the user signs in again. These guards improve navigation
only; backend permissions remain the security boundary.

The profile avatar is a real menu trigger and must remain usable when the
sidebar is collapsed. It exposes personal Change password and Log out actions,
plus an Administrator-only Staff accounts shortcut. The same Staff accounts
destination remains a permission-protected sidebar item and has a visible page
heading. The sidebar expand/collapse button must also remain inside the
collapsed width, and icon-only navigation entries use readable tooltips.
Administrator password reset stays in that separate account table and is not a
personal profile action.

## Patient detail and medical record

`PatientDetailDialog` loads patient details and the medical record as separate
requests. The detail view displays:

- readable patient ID and full name;
- birth date, gender, identity number, contact information, and address;
- blood type, status, allergies, and medical history;
- insurance information;
- emergency contact;
- note, creator/date, and last updater/date.

The medical-record section groups data by admission or visit and shows
admission and discharge dates, department, doctor, room, admission type and
status, reason, diagnoses, and prescriptions. Diagnoses show date, ICD-10 code,
description, severity, and doctor. Prescriptions show medicine, dosage,
frequency, duration, and instructions.

Patient details show one entry per visible care visit with its linked admission,
plus independent historical episodes. The shared read-only record component
includes clinical summary, diagnoses, prescriptions, published result revisions,
and permitted internal notes or consultations. New examination and inpatient
reception are performed in the care workspace; no Add admission or standalone
patient-status action appears in this dialog. Active inpatient details and bed
updates are accessible to the responsible doctor in the workspace, with doctor
and department fixed to preserve the handoff boundary. Unlinked existing active
episodes retain update and completion tools when no care visit is open. Only
Administrator can permanently delete an admission, and an open linked care visit
blocks deletion. The frontend does not provide master-data or appointment CRUD.

## Activity history and patient field history

Keep the two history presentations distinct:

- the per-patient field-history dialog remains available from the patient list,
  uses a default page size of 10, and displays changed field, old value, new
  value, changed by, and date;
- the administrative Activity history page is at `/audit-log`, uses a default
  page size of 20, and displays Time, Event, Target, Performed by, Change
  summary, and Details. The last column contains a text-only View details button.

Both use server pagination. The Activity history page offers page sizes 10, 20,
and 50 and calls `GET /api/v1/audit-logs`. Its route and menu entry require
`VIEW_AUDIT_LOG`, which the delivered role mapping grants only to
Administrator. `/patient-history` is a compatibility redirect to `/audit-log`
and is not a second global patient field-history screen.

The Activity history list prefers snapshot names to technical identifiers.
Admission rows show the admission resource as the primary target and the patient
snapshot as secondary subject context. The actor display name is primary, while
username and role remain secondary context. The mobile layout presents the same
information as cards and keeps View details as a full-width text action.
Sample rows prefix the event label with `Sample ·` in both layouts.

The audit-log service loads a selected event from
`GET /api/v1/audit-logs/{id}`. The detail dialog contains Event summary,
Performed by, Target information, Changes, and Event information. Create events
show new values. Update and status events show before and after values. Delete
events show previous values. Bulk-delete events show ordered affected targets.
Password-reset events state that passwords are never stored. A protected marker
or protected null is displayed as `Protected value`. A sample event also shows
an explicit demonstration warning and Sample data under Data source.

Version 1 rows remain visible. The dialog shows a legacy banner and truthful
messages when changed fields or affected targets were not captured. Do not infer
names or reconstruct missing values from current domain records.

The Patient, Statistics, Activity history, and Staff accounts list filters
refresh automatically after a 300-millisecond pause. A valid change resets the
list to page one, cancels any superseded request, and keeps the latest valid
criteria for later page and page-size changes. These filter bars expose only a
Reset or Clear text action and no Apply action.

The Activity history filter form contains Performed by actor, keyword, action,
entity type, from date, and to date controls. The actor control uses the
placeholder `Username or full name`. Actor input has a 100-character maximum
and keyword input has a 200-character maximum to match the backend. Clear is a
text button without an icon and reloads page one through the same reactive
flow. The page rejects an ending date earlier than the starting date before it
sends a request. A failed request clears stale rows and the total before
showing its error state.

The per-patient dialog still translates numeric gender, blood-type, and
patient-status values to labels and presents stored ISO date values as
`dd/MM/yyyy`. Patient references should display readable patient ID together
with full name.

## Statistics behavior

The statistics route lets the user add only the criteria they need:

- examination date range;
- diagnosis by ICD-10 option;
- medicine option.

The frontend fetches diagnosis and medicine options from the backend, submits
active criteria to `GET /api/v1/statistics/patients`, and shows the total plus
the matching patients. Results display patient ID and full name and can open the
full patient detail/medical-record dialog.

The date filter is meaningful on the backend only when both ends are supplied.
Do not imply that a one-sided date range is currently enforced. The “View as
report” action renders the current result in an A4-style dialog; it does not
generate the LaTeX internship report or a persisted server report.

## Status and reference-data rules

Patient status codes are:

| Code | Label |
|---|---|
| 0 | Waiting |
| 1 | Examining |
| 2 | Admitted |
| 3 | Discharged |

The frontend mirrors these codes as constants because there is no general
master-data endpoint yet. Keep them aligned with backend validation, Flyway
seed data, and the report. The delivered controls expose Discharged to Waiting,
Waiting to Examining, direct Examining to Discharged when no episode is active,
and treatment-episode completion from Examining or Admitted to Discharged.
Direct episode controls remain available only without an open care visit.
The `/care` workspace additionally coordinates assigned room queues, results,
doctor handoff, and ward reception. Confirmed ward reception establishes Admitted
even without a bed; later room removal and service trips preserve that status.
See `docs/care-workflow.md` for current screens and role-by-role operation.

The `*_status_flag` fields are table-specific. Doctor, medicine, ICD-10, and
lookup flags control whether a reference item is available for new use while
historical records must still display it. `ROOMS.status_flag` is different:
shipped seed data uses it as room availability/fullness alongside bed count and
current occupancy. None of these fields is patient status or a generic
user-facing Activate/Deactivate feature. Do not add status-flag buttons to the
current UI without an explicit product requirement.

Compact patient references in selectors, grids, history, statistics, diagnosis,
prescription, or appointment contexts should use patient ID plus full name.
Additional personal and medical data belongs in the detail dialog.

## Material, Matero, styling, and accessibility

- Preserve the Angular Material and Matero admin-shell visual language.
- Use existing Material dialogs, form fields, date pickers, cards, icons, menus,
  and Matero grid/dialog components before adding a competing UI framework.
- Reuse responsive row/column helpers and existing layout patterns.
- Component styles use SCSS. Global style helpers live under `src/styles/`;
  route-specific rules stay beside the route component.
- `angular.json` sets component styles to SCSS and production budgets to 3 MB
  for the initial bundle and 8 kB per component style at error level.
- TypeScript is strict, with strict templates and strict injection parameters.
- Path aliases are `@core`, `@shared`, `@theme`, and `@env`.
- Formatting uses two spaces, UTF-8, single quotes, semicolons, and a 100-column
  Prettier width.
- Keep labels, dialog titles, form errors, keyboard behavior, focus handling,
  and Material semantics accessible. The HTML lint configuration includes the
  Angular template accessibility rules.

Do not redesign an unrelated screen while implementing a functional change.
When a visual redesign is explicitly requested, preserve behavior, responsive
layout, and accessibility while using the repository's design skill and current
theme tokens.

## Authentication and permission boundary

Authentication is database-backed. `LoginService` posts the submitted username
and password to `/api/v1/auth/login`, stores the returned JWT through
`TokenService`, and loads the staff profile, role, and permissions from
`/api/v1/auth/me`. The token interceptor sends the bearer token on protected API
requests. Startup restores a locally stored unexpired session by reloading the
profile; a failed restore clears the session.

`authGuard` protects the application shell. `permissionGuard` reads the required
permission from route data and redirects a user without that permission to
403. Startup loads the server-issued role and permission list into
`ngx-permissions` so the menu is filtered consistently. Components use
`AuthService.hasPermission()` for permission-sensitive actions such as
admission operations.

Client-side guards and hidden controls are presentation safeguards only. Every
protected backend endpoint must enforce the same permission. Do not hardcode
credentials, roles, or permission grants in frontend source, and do not use the
retained `FakeLoginService` as the application provider. Logout clears the
local session; login and logout are not business audit events.

## Testing conventions

The Angular test target uses Vitest in jsdom by default. Existing tests cover
API authentication and profile restoration, tokens, auth and permission
guards, bootstrap permission loading, interceptors, route and menu protection,
patient HTTP behavior, admission dialogs and permission-sensitive controls,
staff-account management, and the Activity history service, helpers, page,
route, and menu. Statistics still has no route spec. Reinspect the current spec
files before making a precise coverage claim.

For new behavior:

- test route services with `provideHttpClientTesting()` and assert URL, method,
  query parameters, request body, and envelope mapping;
- test form validation and backend field-error mapping when form behavior
  changes;
- test permanent-delete confirmation and selection behavior when those flows
  change;
- test pagination conversion and filters when list behavior changes;
- test automatic Activity history refresh and Clear behavior, active-filter
  persistence, request cancellation, query parameters, reversed-date validation,
  list presentation, detail loading,
  action-specific detail layouts, protected values, and legacy fallback when its
  contract changes;
- test status/date/history formatting when mappings change;
- use the zoneless act → `await fixture.whenStable()` → assert pattern for DOM
  updates instead of depending on manual change detection;
- keep tests focused on user-visible behavior and API contracts.

No E2E framework is configured. The generated frontend README mentions
`ng e2e`, but invoking it may prompt to install a framework; it is not a current
verification command.

## Safe commands

Run frontend commands from `hospital-frontend/`.

Install dependencies using the committed lockfile:

```powershell
npm install
```

Start the development server:

```powershell
npm start
```

It serves `http://localhost:4200` and requires the backend on port 8080 for
proxied API calls.

Run non-watch tests, the production build, and read-only lint:

```powershell
npm test -- --watch=false
npm run build
npx ng lint
```

`npm run build` already uses the production configuration because
`angular.json` makes production the default. For an explicit configuration,
use:

```powershell
npx ng build --configuration production
```

Generate an artifact in the actual route layout, for example:

```powershell
npx ng generate component routes/patient/<name>
npx ng generate service routes/patient/<name>
```

Do not use the following as standard verification:

- `npm run build:prod`: it still calls the obsolete `ng build --prod`;
- `npm run lint`, `npm run lint:ts`, or `npm run lint:scss`: the scripts include
  `--fix` and therefore modify files;
- the old `ng generate component features/patient/components/...` path;
- `ng e2e`: no E2E runner is configured.

After a frontend behavior change, run affected tests and `npm run build`. Run
`npx ng lint` when TypeScript, template, or accessibility behavior changed.
Report any command that could not run and the concrete reason.

## Known boundaries and stale legacy corrections

- Angular is 21, not the former “Angular 19+” snapshot.
- The actual architecture is `core/routes/shared/theme`, not
  `core/shared/features`.
- Standalone components are the Angular default; there are no feature NgModules.
- Zoneless change detection replaces the old blanket OnPush instruction.
- Reactive Forms are the established form strategy even though Angular 21 also
  offers Signal Forms.
- HTTP failures are centralized through interceptors; route services do not each
  need a duplicate `catchError`.
- `/patients` currently filters by search field, status, and gender. It does not
  currently offer a department filter.
- Lookup constants are mirrored locally because no general lookup endpoint
  exists.
- Patient-status transitions are enforced by dedicated APIs and permissions;
  generic patient create and edit forms cannot submit status.
- The patient-detail diagnosis and prescription sections remain read-only;
  the care workspace supports visit-linked clinical entry. Active admission
  create, edit, and completion actions are permission-controlled, and admission
  deletion is restricted to Administrator.
- Appointment and bed-room management screens are not implemented. Care points,
  service routing, and staff assignments are configured under
  `/care/configuration`, which requires `CARE_MANAGE`. These points are separate
  from bed rooms. Staff with `CARE_VIEW` land on `/care`; Patient still lands on
  `/my-record` and has no access to care routes.
- Comments in the current statistics patient-reference model that say identity
  card is the displayed label are stale; the actual UI displays patient ID and
  full name.
- The retained `dashboard/` directory is not evidence of an active dashboard
  route.
- The retained `fake-login.service.ts` file is not the active authentication
  provider. The application uses the backend login, JWT, and current-profile
  APIs.
- Activity history is a global business-action audit page. It does not replace
  the per-patient field-history dialog.
- Statistics route tests remain a coverage gap.

Treat these as boundaries to preserve or gaps to report. Do not silently invent
missing endpoints, screens, validations, filters, transitions, or security
features while documenting or making an unrelated change.
