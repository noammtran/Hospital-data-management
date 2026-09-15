package com.usth.hospital.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class StaffIdentityAccountMigrationTest {

    @Test
    void v20AddsIdentityBackedTemporaryCredentialsForFictionalStaff() throws IOException {
        String migration = resource("db/migration/V20__staff_identity_accounts.sql");

        assertThat(migration)
                .contains("app_user_identity_no VARCHAR2(12)")
                .contains("REGEXP_LIKE(app_user_identity_no, '^[0-9]{12}$')")
                .contains("app_user_username = app_user_identity_no")
                .contains("app_user_password_change_required = 1")
                .contains("FROM DOCTORS")
                .contains("'DOCTOR'")
                .contains("'NURSE'")
                .contains("'RECEPTIONIST'")
                .doesNotContain("app_user_password_hash = '001");

        assertThat(count(migration, "UPDATE APP_USERS")).isEqualTo(4);
        assertThat(count(migration, "INSERT INTO APP_USERS")).isEqualTo(26);
    }

    private static int count(String text, String value) {
        return (text.length() - text.replace(value, "").length()) / value.length();
    }

    private static String resource(String path) throws IOException {
        try (InputStream input = StaffIdentityAccountMigrationTest.class
                .getClassLoader().getResourceAsStream(path)) {
            assertThat(input).as("classpath resource %s", path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
