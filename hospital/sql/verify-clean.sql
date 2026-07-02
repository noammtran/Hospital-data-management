SET LINESIZE 200
SET PAGESIZE 100
SET SQLBLANKLINES ON
PROMPT === HOSPITAL schema: tables ===
SELECT table_name FROM user_tables ORDER BY table_name;
PROMPT === flyway_schema_history summary ===
SELECT installed_rank, version, description, success FROM "flyway_schema_history" ORDER BY installed_rank;
PROMPT === row counts (key tables) ===
SELECT 'LOOKUP_VALUE'    AS tbl, COUNT(*) AS n FROM LOOKUP_VALUE   UNION ALL
SELECT 'DEPARTMENTS'             , COUNT(*)        FROM DEPARTMENTS    UNION ALL
SELECT 'DOCTORS'                 , COUNT(*)        FROM DOCTORS        UNION ALL
SELECT 'PATIENTS'                , COUNT(*)        FROM PATIENTS       UNION ALL
SELECT 'ADMISSIONS'              , COUNT(*)        FROM ADMISSIONS     UNION ALL
SELECT 'DIAGNOSES'               , COUNT(*)        FROM DIAGNOSES      UNION ALL
SELECT 'PRESCRIPTIONS'           , COUNT(*)        FROM PRESCRIPTIONS  UNION ALL
SELECT 'APPOINTMENTS'            , COUNT(*)        FROM APPOINTMENTS;
EXIT;
