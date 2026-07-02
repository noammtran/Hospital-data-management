-- Stamps the create/update dates on PATIENTS at the database layer, so the
-- last-modified date is guaranteed correct regardless of which path wrote the
-- row. The application also sets these columns; the trigger is a safety net.
CREATE OR REPLACE TRIGGER trg_patients_audit
BEFORE INSERT OR UPDATE ON PATIENTS
FOR EACH ROW
BEGIN
    IF INSERTING AND :NEW.patient_created_date IS NULL THEN
        :NEW.patient_created_date := TRUNC(SYSDATE);
    END IF;
    :NEW.patient_last_updated_date := TRUNC(SYSDATE);
END;
/
