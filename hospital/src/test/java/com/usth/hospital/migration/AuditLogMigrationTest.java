package com.usth.hospital.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class AuditLogMigrationTest {

    @Test
    void v17_defaultsGenuineLegacyRowsToRealAndMarksOnlySyntheticLegacyRowsAsSample() throws IOException {
        String migration = resource("db/migration/V17__structured_audit_events.sql");

        assertThat(migration)
                .contains("audit_log_sample_flag         NUMBER(1,0)    DEFAULT 0 NOT NULL")
                .contains("CONSTRAINT ck_audit_sample_flag CHECK (audit_log_sample_flag IN (0, 1))")
                .contains("SET audit_log_sample_flag = 1")
                .contains("WHERE audit_log_details LIKE 'Synthetic demo event%'");
    }

    @Test
    void v18_marksEveryHeaderAsSampleAndUsesMasterReferenceLabels() throws IOException {
        String migration = resource("db/migration/V18__structured_audit_sample_events.sql");
        Pattern headerPattern = Pattern.compile("(?s)INSERT INTO AUDIT_LOGS\\s*\\(.*?;");
        var headers = headerPattern.matcher(migration).results().map(MatchResult::group).toList();

        assertThat(headers).hasSize(10).allSatisfy(header -> {
            assertThat(header).contains("audit_log_sample_flag");
            assertThat(header).matches("(?s).*,\\s*1\\s*\\)\\s*;");
        });
        assertThat(migration)
                .contains("NULL, '1', NULL, 'Nội khoa'")
                .contains("'2', NULL, 'Ngoại khoa', NULL")
                .contains("'12', '18', 'P-601', 'P-1001'")
                .doesNotContain("Internal Medicine", "Cardiology", "Room A12", "Room A18");
    }

    private static String resource(String path) throws IOException {
        try (InputStream input = AuditLogMigrationTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(input).as("classpath resource %s", path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
