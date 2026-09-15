package com.usth.hospital.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PatientAccountMigrationTest {

    @Test
    void v19LinksExactlyOnePatientAccountAndCascadesItsDeletion() throws IOException {
        String migration = resource("db/migration/V19__patient_self_service_accounts.sql");

        assertThat(migration)
                .contains("'PATIENT'")
                .contains("app_user_patient_id")
                .contains("REFERENCES PATIENTS (patient_id)")
                .contains("ON DELETE CASCADE")
                .contains("UNIQUE (app_user_patient_id)")
                .contains("app_user_password_change_required IN (0, 1)")
                .doesNotContain("app_user_password_hash VARCHAR2");
    }

    private static String resource(String path) throws IOException {
        try (InputStream input = PatientAccountMigrationTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(input).as("classpath resource %s", path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
