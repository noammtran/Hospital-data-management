# Development of a medical examination and treatment management software system

An internship project at USTH, the University of Science and Technology of Hanoi.

The application manages hospital patient records, examination and treatment workflows, staff accounts, patient access, and business activity history.

## Technology

* Java 21 and Spring Boot 3.5
* Angular 21 with Angular Material
* Oracle Database and Flyway
* Maven Wrapper and npm

## Main features

* Patient creation, editing, paginated search, filtering, and permanent single or bulk deletion with confirmation
* Patient details with field history and unified examination and treatment records
* Care registration, arrival confirmation, examinations, service requests, result review, accepted doctor handoff, and inpatient reception
* Diagnoses, prescriptions, treatment completion, and management of active inpatient details and beds
* Staff authentication, role permissions, account management, and work assignments
* My Record for patients to view their own released records
* Activity history for successful business changes, with filters and event details

Patient status values are `0 = Waiting`, `1 = Examining`, `2 = Admitted`, and `3 = Discharged`.

Each patient can have at most one open care visit. Clinical signing requires a linked doctor account and an active assignment. Published results require review of the current revision before completion. Pending services require a future follow up plan. Transfer between inpatient wards remains future work.

## Repository layout

* `hospital/` contains the backend, Maven Wrapper, database migrations, and backend tests.
* The root folder matching `hospital*frontend` contains the Angular application, npm manifests, and frontend tests.
* `hospital/src/main/resources/db/migration/` contains the shipped schema and demonstration data.
* `.agents/guidance/` contains project guidance for contributors.

The current Git rules exclude local documentation, reference datasets, editor state, temporary files, and generated output. Application migrations remain included so a fresh checkout can initialize its database.

## Prerequisites

1. Install JDK 21.
2. Install Node.js 22 LTS with npm.
3. Install Oracle Database 23ai Free or prepare a compatible Oracle development database.
4. Use PowerShell for the commands below.

Run each section from the directory indicated. The frontend wildcard selects its existing folder. The Maven start command constructs its plugin name using character code 45.

## Database preparation

Connect an administrative account to the intended pluggable database, normally `FREEPDB1`, and prepare a dedicated application schema with the permissions needed by Flyway.

The SQL setup file under `hospital/sql/` is a destructive reset utility. It drops the existing `HOSPITAL` user and all of that user's data before recreating the schema. Use it only for an intentionally disposable database after checking the host, service, and target user. Review its local credential before use and do not publish personal credentials.

On backend startup, Flyway applies the migrations from `hospital/src/main/resources/db/migration/`. A fresh schema receives the tables, constraints, reference data, fictional patients, demonstration accounts, and sample activity events. Existing schemas receive only pending migrations.

## Run the backend

Open PowerShell at the repository root. Replace the placeholders with your local connection values and a private signing secret containing at least 32 bytes in UTF8 encoding.

```powershell
cd hospital
$env:DB_URL = 'jdbc:oracle:thin:@<host>:<port>/<service>'
$env:DB_USERNAME = '<username>'
$env:DB_PASSWORD = '<password>'
$env:JWT_SECRET = '<private signing secret containing at least 32 bytes>'
.\mvnw.cmd ('spring' + [char]45 + 'boot:run')
```

The backend serves [localhost:8080](http://localhost:8080), with API routes under `/api/v1`. Keep the signing secret stable between restarts. Without an explicit secret, a temporary key is generated and previously issued tokens become invalid after restart.

Use a separate terminal for verification or packaging, from `hospital/`:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd clean package
```

The packaged application is written to `hospital/target/`. Verification and packaging require the dependencies to be available through Maven.

## Run the frontend

Open another PowerShell terminal at the repository root:

```powershell
cd hospital*frontend
npm install
npm start
```

Open [localhost:4200](http://localhost:4200). Start the backend first. The frontend proxy forwards `/api` requests to port 8080 through `proxy.config.js`. Update that proxy if you intentionally change the backend port.

For a fresh checkout, `npm ci` installs the exact dependency versions in the committed lockfile. It replaces an existing dependency installation.

From the frontend folder, run:

```powershell
npm test
npm run build
```

The test command can remain in watch mode. For a single run, use:

```powershell
npm test ([string][char]45 * 2) (([string][char]45 * 2) + 'watch=false')
```

The production build is written to the frontend `dist/` folder.

## Demonstration accounts

All people in the shipped demonstration data are fictional. The usernames below are synthetic identity numbers. On a fresh database, each is also that account's temporary password. Replace it when prompted before using the application. Only password hashes are stored.

* Receptionist: `001201300001`
* Doctor with a linked profile: `001201100002`
* Nurse: `001201200001`
* Technician: `001201400001`
* Administrator: `001201900001`
* Patient: `001068011234`

The general Doctor account `001201100001` has no linked doctor profile. Use an individually linked account for clinical work. Imaging performer `001201400002` has no validation authority. Imaging validator `001201100022` can validate according to the configured assignment.

Administrator manages staff accounts and care configuration. Staff details show role functions, doctor linkage, point assignments, and missing work setup. A new doctor profile can be created and linked before assigning work. Password replacement and clinical assignment are separate requirements.

Creating a patient requires an identity number containing 12 digits and creates a linked Patient account in the same transaction. Patient accounts are excluded from staff account management and are removed when the patient is permanently deleted.

My Record resolves the patient from the authenticated account. It releases completed diagnoses, prescriptions, and care conclusions. Service results appear only when the latest published revision has been reviewed. Drafts, internal notes, and handoff discussions are excluded.

## Check the installation

1. Open the frontend and sign in with a demonstration account. Replace its temporary password when prompted.
2. Use an authorized staff account to open Patients, search the list, and inspect a fictional record.
3. Open Care workspace to register a visit, confirm arrival, and receive the patient with a linked and assigned doctor.
4. Use Administrator to inspect Staff accounts, work assignments, and Activity history.
5. Check that synthetic activity events show a Sample label. They are demonstration content and do not prove real operational activity.
6. Use the Patient demonstration account to confirm that My Record shows only its linked profile and three historical treatment records.

Patient field history and Activity history are separate. Field history records changed patient values. Activity history records successful business writes for patients, admissions, staff accounts, and care operations. Ordinary reads, searches, login, and navigation do not create business events.

## Troubleshooting

* If Oracle cannot be reached, check the listener, host, port, and service in `DB_URL`.
* If Oracle rejects the application user, confirm that the connection targets the intended pluggable database and that the credentials match.
* If the backend uses an unexpected database, set the environment variables in the same terminal that starts Maven.
* If a port is already occupied, stop the conflicting local process or intentionally update the server and proxy configuration.
* If the frontend shows no data, confirm that the backend is running and inspect the proxy target.
* An HTTP `401` response can indicate a missing, expired, revoked, or invalid token. Sign in again after a password reset or signing key change.
* An HTTP `403` response indicates insufficient permission or missing clinical access for the requested operation.
* If Flyway reports a validation mismatch, inspect the database history before changing anything. Do not rewrite an applied migration or reset a valuable database to bypass the error.

## Preparing a Git update

Review the changed files before staging. Keep database passwords, signing secrets, personal data, generated output, and local backups out of commits. Commit application sources, tests, build manifests, lockfiles, and new Flyway migrations together when they belong to the same change.

Git ignore rules apply to untracked files. They do not remove files that Git already tracks or erase content from previous commits.
