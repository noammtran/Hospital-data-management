# Development environment and data guidance

Read this guide before changing local setup, run commands, database connection
instructions, seed data, reference datasets, or external-data provenance. It
replaces the local-development, common-command, data-strategy, reference-file,
rules, and data-source sections of the former `CLAUDE.md`.

The supported setup and run instructions are in `README.md`. Treat that file,
the current build manifests, scripts, and application configuration as the
source of truth. Historical host addresses, Oracle installation paths, service
names, passwords, and framework versions in the former `CLAUDE.md` are not
portable instructions.

## Supported local toolchain

The currently supported baseline is:

- JDK 21;
- Node.js 22 LTS with npm, with Node.js 20.19 as the minimum accepted by the
  Angular 21 toolchain;
- Oracle Database 23ai Free, normally using the `FREEPDB1` pluggable database;
- the Maven Wrapper shipped in `hospital/`;
- npm dependencies pinned by the frontend lockfile;
- PowerShell for the repository's Windows helper scripts.

Optional documentation tools are:

- XeLaTeX and Biber for the report;
- Node.js plus Mermaid CLI for diagrams. The repository has a dedicated
  `docs/uml/package-lock.json`, and `render-all.ps1` can also fall back to a
  global `mmdc` or `npx`.

Do not claim a report or diagram build succeeded when the corresponding
toolchain is absent.

## Database initialization

Create the application schema by connecting an administrative Oracle account
to the intended **PDB**, then running:

```powershell
Set-Location <repository-root>\hospital
sqlplus <admin-user>/<admin-password>@<db-host>:<listener-port>/<pdb-service> "@sql\setup-hospital-user.sql"
```

Use placeholders in documentation and examples. Do not restore the old
machine-specific ZeroTier address, Oracle home, or Windows service names as
repository-wide defaults.

### Destructive-script warning

`hospital/sql/setup-hospital-user.sql` begins with:

```sql
DROP USER hospital CASCADE
```

It therefore destroys the existing `HOSPITAL` schema, including its tables and
data, before recreating the user. Treat the script as a destructive reset, not
an ordinary startup step.

- Never run it automatically.
- Run it only when the user explicitly wants a clean local schema.
- Before running it, verify the resolved host, port, PDB service, administrative
  account, and target username.
- Do not run it against a shared, production-like, or otherwise valuable
  database.
- Warn that recovery requires a backup or reseeding through Flyway.
- The script currently contains a development password. Do not repeat that
  password in new documentation, logs, source files, or messages.

On the first backend start after a clean reset, Flyway creates the physical
schema and loads the shipped seed data.

## Portable database configuration

Set connection values in the PowerShell session that starts the backend:

```powershell
$env:DB_URL = 'jdbc:oracle:thin:@<db-host>:<listener-port>/<pdb-service>'
$env:DB_USERNAME = '<application-user>'
$env:DB_PASSWORD = '<application-password>'
```

Then start the backend from `hospital/`:

```powershell
.\mvnw.cmd spring-boot:run
```

`hospital/src/main/resources/application.properties` reads those three
variables. When they are absent, it currently falls back to:

- `jdbc:oracle:thin:@localhost:1521/XEPDB1`;
- username `hospital`;
- a blank password.

Those values are compatibility fallbacks, not proof that a particular machine
uses Oracle XE, localhost, or a blank password. Prefer explicit environment
variables. Never commit a personal host, password, token, or administrative
credential to `application.properties`.

The backend normally serves `http://localhost:8080` with APIs under `/api/v1`.
The frontend normally serves `http://localhost:4200` and proxies `/api` traffic
to the backend through `hospital-frontend/proxy.config.js`.

## Supported commands

Run commands from the directory shown. Prefer repository wrappers and package
scripts so commands remain reproducible.

### Backend

From `hospital/`:

```powershell
# Start the API
.\mvnw.cmd spring-boot:run

# Focused test while iterating
.\mvnw.cmd -Dtest=PatientServiceImplTest test

# All tests
.\mvnw.cmd test

# Completed or cross-cutting backend verification
.\mvnw.cmd verify

# Package the executable jar
.\mvnw.cmd clean package
```

Use a real test class in place of `PatientServiceImplTest`. `verify` is the
completion check for a backend change that crosses layers or changes a contract.

### Frontend

From `hospital-frontend/`:

```powershell
# Install dependencies as documented by the repository
npm install

# Start the Angular development server
npm start

# Run tests once
npm test -- --watch=false

# Production build
npm run build

# Read-only Angular lint check
npx ng lint
```

For a clean reproducible installation in CI or a disposable working tree,
`npm ci` may be used with the committed lockfile. It removes the existing
`node_modules` directory, so do not substitute it casually when preserving a
developer's local dependency tree matters.

The repository's `npm run lint`, `lint:ts`, and `lint:scss` scripts include
`--fix` and therefore modify files. Do not use them as read-only checks unless
formatting changes are in scope. Do not use the obsolete
`ng generate ... features/...` example from the former `CLAUDE.md`; the current
application is organized under `src/app/routes`, not `src/app/features`.

### Report

From `docs/report/`:

```powershell
.\build.ps1
```

The script runs XeLaTeX, Biber, XeLaTeX, and XeLaTeX and writes
`docs/report/build/main.pdf`. Treat LaTeX errors, missing figures, unresolved
citations, and unresolved references as failures.

### Mermaid diagrams

Install the pinned local renderer when needed:

```powershell
npm ci --prefix .\docs\uml
```

Then, from the repository root:

```powershell
.\docs\uml\render-all.ps1
```

The script renders all `docs/uml/*.mermaid` sources into
`docs/report/figures/`. Inspect the output rather than treating process exit
alone as visual verification.

### Do not use the root `start.ps1`

The root `start.ps1` is not a supported or portable launcher. It contains a
machine-specific path, host, Oracle Windows service names, and a plaintext
development credential, and it opens additional terminal windows. Do not run,
copy, recommend, or use it as configuration truth. Start the database through
the developer's own Oracle tooling, then start backend and frontend separately
using the `README.md` commands.

## Troubleshooting

Use evidence from the local machine rather than restoring assumptions from the
former `CLAUDE.md`.

### Oracle listener and service

`ORA-12541: no listener` means the JDBC client cannot reach a listener at the
selected host and port. Verify:

- the Oracle listener and database services are running;
- the host and port are correct and reachable;
- the listener is actually bound to the selected interface;
- a firewall, VPN, or mesh-network configuration is not blocking the route.

Do not automatically replace the host with either `localhost` or the old
ZeroTier address. The correct host is environment-specific.

### CDB versus PDB

`ORA-65096: invalid common user or role name` commonly means the administrative
session is connected to the container database rather than the intended
pluggable database. Run the application-user setup against the PDB service,
normally `FREEPDB1`, not the CDB service.

### Credentials

`ORA-01017: invalid username/password` means the configured credentials do not
match the target database or the session is reaching a different service than
expected. Recheck `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the same shell
that launches Maven. Do not print the password while diagnosing the problem.

### Ports and frontend proxy

- If port 8080 is occupied, either stop the conflicting process or intentionally
  change `server.port`.
- If port 4200 is occupied, use an intentional Angular development-server port.
- If the backend port changes, update `hospital-frontend/proxy.config.js` or the
  equivalent local proxy configuration so `/api/**` still reaches the backend.
- A frontend that loads but shows no data often indicates that the backend is
  stopped or the proxy target is wrong.

### Flyway

Flyway creates a quoted lowercase metadata table on this Oracle setup. Query it
as:

```sql
SELECT * FROM "flyway_schema_history";
```

An unquoted `flyway_schema_history` is uppercased by Oracle and can produce
`ORA-00942`.

If Flyway reports a checksum or validation mismatch, do not rewrite a migration
that may already have been applied. Determine which database and schema are in
use, inspect `"flyway_schema_history"`, and add a new ordered migration for an
intentional change. A clean local reset is an explicit destructive alternative,
not an automatic fix.

### SQL*Plus character display

Mojibake in a Windows SQL*Plus console may be a terminal code-page or client
locale problem rather than corrupted database data. Check the source file's
UTF-8 encoding and verify the stored value through JDBC/JSON or an
encoding-aware database client before changing seed text. Do not "repair" valid
UTF-8 SQL by replacing Vietnamese characters with ASCII merely because one
console renders them incorrectly.

### Configuration fallback

If the backend silently attempts `localhost:1521/XEPDB1`, the expected
environment variables were not visible to the Maven process. Set them in the
same PowerShell session, or intentionally provide equivalent runtime
configuration. Do not solve this by committing personal values to
`application.properties`.

## Data ownership and current inventory

The repository currently has two distinct data areas:

```text
data/
├── reference/
│   ├── db_schema_sample/
│   ├── DIAGNOSES/
│   ├── ICD-10-CSV/
│   ├── medications/
│   └── sample_patients/
└── seed/
    ├── V1__lookup_values.sql
    ├── ...
    └── V11__appointments.sql
```

The reference inventory includes:

- third-party hospital database examples under `db_schema_sample/`;
- a vendored pediatric complex chronic conditions research package and ICD
  resources under `DIAGNOSES/`;
- 2019 and 2020 diagnosis CSV files under `ICD-10-CSV/`;
- a WHO Essential Medicines List workbook under `medications/`;
- several external sample-patient datasets and their source notes under
  `sample_patients/`.

The normalized paths proposed by the former `CLAUDE.md`, such as
`data/reference/icd10/icd10_codes.csv` and
`data/reference/departments/departments.csv`, do not currently exist. Do not
write code that assumes those paths without first adding and documenting the
corresponding data.

### Flyway is the shipped data truth

`hospital/src/main/resources/db/migration/` is the only migration location
loaded by the backend:

- `V0__schema.sql` creates the baseline physical schema;
- `V1` through `V11` load lookup and sample data;
- `V12` adds the patient audit-date trigger;
- `V13` adds the unique patient identity constraint;
- `V14` adds database-backed test staff accounts;
- `V15` adds the business activity audit store;
- `V16` adds sixteen legacy synthetic Activity history events for local
  demonstration;
- `V17` adds version 2 event snapshots and the ordered audit change and target
  tables. It also adds sample provenance and marks the synthetic V16 rows;
- `V18` adds ten structured synthetic events for the Activity history list and
  detail dialog. Every V18 header is marked as sample data;
- `V19` adds the Patient role, a unique cascading account-to-patient link, and
  the forced-password-change flag. It does not backfill accounts for the
  existing synthetic patients; runtime patient create/update manages them.
- `V20` adds the staff identity number, migrates the original fictional staff
  accounts to identity-backed temporary credentials, and seeds individual fake
  doctor, nurse, and receptionist accounts.
- `V21` merges one forced-change Patient demo account linked to fictional
  patient `P26ML011`. Its identity-number credential is reproducible for local
  testing, and the linked seed record has exactly three admissions.

`data/seed/` is a staging/history area. Its files are not byte-identical to the
currently shipped migrations and can be stale. Never assume editing
`data/seed/Vn__*.sql` changes a running or freshly installed application.
Promote a reviewed change into a **new** file under the backend migration
directory, using the next unused Flyway version.

## Privacy and synthetic data

- Never add real patient, doctor, employee, emergency-contact, insurance,
  identity-card, phone, email, address, or medical-history records.
- Generate clearly synthetic Vietnamese names and contact details for demo
  patients and doctors.
- Keep synthetic Activity history rows visibly marked through stored provenance.
  They are demonstration data and never evidence of real operational actions.
- Avoid copying a row from an external patient dataset and merely translating
  the name. External patient datasets are structure references only.
- Medical terminology, ICD codes, medicine names, and department names may come
  from authoritative references, but every person-to-condition association in
  seed data must remain fictional.
- Do not paste sensitive data into tests, screenshots, logs, prompts, issue
  descriptions, commits, or the report.
- Keep original external archives separate from normalized output so provenance
  and transformation can be audited.

For normalized CSV files, use UTF-8, comma delimiters, a header row, consistent
newline handling, and proper CSV quoting. Preserve Vietnamese diacritics. Source
artifacts may remain in their native formats, such as XLSX, ZIP, PDF, R data, or
upstream repository files.

## Candidate external sources

These are candidates carried forward from the former `CLAUDE.md`, not
pre-approved downloads. Before using any of them, reverify the current URL,
publisher, edition, license, redistribution terms, and fitness for the project's
jurisdiction and scope.

### Official or primary candidates

- WHO ICD browser:
  `https://icd.who.int/browse10/2019/en`
- US Centers for Medicare & Medicaid Services ICD-10 files:
  `https://www.cms.gov/medicare/coding-billing/icd-10-codes`
- WHO Essential Medicines List:
  `https://www.who.int/publications/i/item/WHO-MHP-HPS-EML-2023.02`
- The current official edition of *Dược thư quốc gia Việt Nam*, when a lawful,
  verifiable source is available.
- Official department/service directories published by Vietnamese hospitals
  such as Bạch Mai, Chợ Rẫy, and Central Military Hospital 108.

WHO ICD and US ICD-10-CM are related but not interchangeable. Record the exact
classification, country modification, release year, and intended use.

### Third-party candidates

- ICD10Data.com: `https://www.icd10data.com/ICD10CM/Codes`
- Kaggle drug or medicine datasets.
- Kaggle Healthcare Dataset:
  `https://www.kaggle.com/datasets/prasad22/healthcare-dataset`
- Kaggle Hospital Patient Records:
  `https://www.kaggle.com/datasets/blueblushed/hospital-dataset-for-practice`
- `GziXnine/Hospital_Management_System` on GitHub.
- `rawanfouzan/Hospital-Management-System-Database` on GitHub.

Third-party sites and repositories are design or structure references unless
their license explicitly permits the intended reuse. Do not copy schema,
documentation, data, or code without recording and complying with the license.
Kaggle access terms and the individual dataset license must both be checked.

### Locally defined data

Room types currently consist of normal, VIP, ICU, and emergency categories in
`LOOKUP_VALUE`; no external room-type dataset is required. Department codes,
Vietnamese translations, descriptions, and demo records may be locally curated
after comparison with authoritative references.

## Provenance and data-to-Flyway workflow

Use this workflow for any new or refreshed dataset:

1. Define the exact application need, target fields, expected row count, and
   whether the data is reference data or synthetic demo data.
2. Select a source and reverify its publisher, version/date, license, download
   URL, and redistribution constraints.
3. Preserve the original artifact under an appropriate `data/reference/`
   subdirectory. Record source URL, retrieval date, version, license, checksum,
   and any attribution requirement in a nearby provenance note.
4. Use a reproducible cleaning or extraction process. Document column mapping,
   filtering, translations, normalization, and exclusions.
5. Validate UTF-8 text, required fields, duplicate business keys, Oracle column
   lengths, numeric lookup codes, date formats, foreign-key targets, and quote
   escaping.
6. For people, discard source rows and generate synthetic Vietnamese records.
   For medical/reference items, select only the subset justified by project
   scope.
7. Generate reviewable Oracle SQL in a staging location. Do not overwrite an
   applied Flyway version or assume `data/seed/` is automatically executed.
8. Add a new ordered migration under
   `hospital/src/main/resources/db/migration/`, preserving current naming,
   constraints, and Oracle syntax.
9. Test the migration against a verified disposable schema, then run backend
   tests or `.\mvnw.cmd verify` as appropriate. Check actual row counts and
   representative Vietnamese text.
10. Update the relevant report section and `docs/report/progress-log.md` when
    the shipped schema, seed data, provenance, or measured result changes.

Do not download large datasets speculatively. Do not add binaries, archives, or
vendored repositories without a concrete use, an understood license, and a
review of repository-size impact.

## Secrets and operational safety

- Keep database passwords and administrative credentials out of committed
  files.
- Use session environment variables or an approved local secret mechanism.
- Redact credentials from command history, logs, screenshots, and support
  output.
- Do not expose a database listener to a new network interface as part of an
  application task unless the user explicitly requests that operational change.
- Never run user/schema deletion, database reset, or bulk data replacement
  against an unresolved target.
- Before any destructive database action, identify the exact host, service,
  schema, expected data loss, and recovery path.
- Do not infer that a local development password, host, or Windows service name
  is safe to reuse elsewhere.
