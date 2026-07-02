# Development of a medical examination and treatment management software system

Web application for managing hospital patient records — registration, medical records, diagnoses, prescriptions and change history.

Internship project at USTH (University of Science and Technology of Hanoi).

**Tech stack:** Spring Boot 3.5 (Java 21) · Angular 21 · Oracle Database · Maven · npm

## Repository layout

| Path | Content |
|------|---------|
| `hospital/` | Backend — Spring Boot REST API (Maven project) |
| `hospital-frontend/` | Frontend — Angular web application |
| `start.ps1` / `stop.ps1` | Convenience scripts for the author's machine (see notes below) |

## Prerequisites

- **JDK 21** (LTS)
- **Node.js 22 LTS** with npm (minimum 20.19 — required by Angular 21)
- **Oracle Database** — Oracle Database 23ai Free or Oracle XE 21c, listener on port 1521
- Windows PowerShell (only for the helper scripts; all commands below also work in any shell)

## 1. Database setup

1. Install Oracle Database Free and note the **PDB service name**:
   `FREEPDB1` for 23ai Free, `XEPDB1` for XE 21c.

2. Create the application user by running the setup script **as SYSTEM against the PDB**:

   ```powershell
   cd hospital
   sqlplus system/<system_password>@localhost:1521/freepdb1 "@sql\setup-hospital-user.sql"
   ```

   This creates the user `HOSPITAL` with password `Hospital123` and the required privileges.

3. **No manual schema creation is needed.** On first backend start, Flyway automatically applies all migrations in `hospital/src/main/resources/db/migration/` — tables, constraints, triggers and seed data (departments, doctors, medicines, ICD-10 codes, sample patients).

## 2. Run the backend

From the `hospital/` directory, set the connection environment variables, then start:

```powershell
$env:DB_URL      = 'jdbc:oracle:thin:@localhost:1521/freepdb1'   # or xepdb1 on XE
$env:DB_USERNAME = 'hospital'
$env:DB_PASSWORD = 'Hospital123'
.\mvnw spring-boot:run
```

- Backend runs at **http://localhost:8080**, API base path `/api/v1`
  (quick check: `GET http://localhost:8080/api/v1/patients`)
- Run tests: `.\mvnw test`
- Build a jar: `.\mvnw clean package` → `target/hospital-0.0.1-SNAPSHOT.jar`

## 3. Run the frontend

From the `hospital-frontend/` directory:

```powershell
npm install
npm start
```

- App runs at **http://localhost:4200**
- The dev server proxies all `/api` requests to the backend on port 8080 (`proxy.config.js`), so **start the backend first**.
- The login screen is mocked for demo purposes — **any username/password logs in**.
- Production build: `npm run build` → `dist/`

## 4. Verify the installation

After both servers are running:

1. Open **http://localhost:4200** — the login page appears.
2. Log in with **any username and password** (authentication is mocked).
3. Open the **Patients** page — a paginated list of seeded sample patients appears. You can search, filter, view details, create, edit and delete records right away; the database is pre-populated by the migrations.
4. Optional API check without the UI: `GET http://localhost:8080/api/v1/patients` returns a JSON list of patients.

## Helper scripts (optional)

`start.ps1` starts the Oracle Windows services and opens backend + frontend in separate windows; `stop.ps1` shuts them down. They contain absolute paths and the author's database address — **adjust the variables at the top** before using them on another machine.

## Troubleshooting

| Symptom | Cause / fix |
|---------|-------------|
| `ORA-12541: no listener` | Oracle listener service not running, or wrong host/port in `DB_URL` |
| `ORA-65096: invalid common user` | Connected to the CDB — use the PDB service (`freepdb1` / `xepdb1`) in the URL |
| `ORA-01017: invalid username/password` | `DB_PASSWORD` not set or does not match the setup script |
| Port 8080 or 4200 already in use | Stop the conflicting process, or change `server.port` in `application.properties` / use `ng serve --port` |
| Frontend shows no data | Backend not running — start it first, then reload |
