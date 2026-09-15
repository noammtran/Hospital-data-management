package com.usth.hospital.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PatientDemoAccountMigrationTest {

    private static final String PATIENT_ID = "P26ML011";
    private static final String IDENTITY_NO = "001068011234";

    @Test
    void v21CreatesAForcedChangePatientLoginForARecordWithThreeAdmissions() throws IOException {
        String migration = resource("db/migration/V21__patient_demo_account.sql");
        String patientSeed = resource("db/migration/V7__patients.sql");
        String admissionSeed = resource("db/migration/V8__admissions.sql");

        assertThat(migration)
                .contains("MERGE INTO APP_USERS")
                .contains("app_user_patient_id = source.patient_id")
                .contains("target.app_user_role = 'PATIENT'")
                .contains("target.app_user_password_change_required = 1")
                .contains(") > 2")
                .doesNotContain("app_user_password_hash = '001");
        assertThat(patientSeed).contains(PATIENT_ID, IDENTITY_NO);
        assertThat(count(admissionSeed, "'" + PATIENT_ID + "'")).isEqualTo(3);

        var hashMatcher = Pattern.compile("'(\\$2[aby]\\$12\\$[./A-Za-z0-9]{53})'").matcher(migration);
        assertThat(hashMatcher.find()).isTrue();
        assertThat(new BCryptPasswordEncoder().matches(IDENTITY_NO, hashMatcher.group(1))).isTrue();
    }

    private static int count(String text, String value) {
        return (text.length() - text.replace(value, "").length()) / value.length();
    }

    private static String resource(String path) throws IOException {
        try (InputStream input = PatientDemoAccountMigrationTest.class
                .getClassLoader().getResourceAsStream(path)) {
            assertThat(input).as("classpath resource %s", path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
