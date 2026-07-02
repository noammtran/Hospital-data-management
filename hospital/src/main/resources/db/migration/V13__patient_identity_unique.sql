-- Enforce that no two patients share the same identity card number (CCCD).
-- The column stays nullable; Oracle permits multiple NULLs under a single-column
-- unique constraint, so patients without a recorded CCCD are unaffected. The
-- plain lookup index is replaced by the unique constraint's own index.
DROP INDEX ix_patients_identity_no;
ALTER TABLE PATIENTS ADD CONSTRAINT uq_patients_identity_no UNIQUE (patient_identity_no);
