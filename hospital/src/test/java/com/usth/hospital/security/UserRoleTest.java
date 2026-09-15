package com.usth.hospital.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserRoleTest {

    @Test
    void everyStaffRoleCanChangeItsOwnPassword() {
        assertThat(UserRole.RECEPTIONIST.permissions()).contains(AppPermission.CHANGE_OWN_PASSWORD);
        assertThat(UserRole.DOCTOR.permissions()).contains(AppPermission.CHANGE_OWN_PASSWORD);
        assertThat(UserRole.NURSE.permissions()).contains(AppPermission.CHANGE_OWN_PASSWORD);
        assertThat(UserRole.ADMIN.permissions()).contains(AppPermission.CHANGE_OWN_PASSWORD);
    }

    @Test
    void onlyAdminCanViewAuditLog() {
        assertThat(UserRole.ADMIN.permissions()).contains(AppPermission.VIEW_AUDIT_LOG);
        assertThat(UserRole.DOCTOR.permissions()).doesNotContain(AppPermission.VIEW_AUDIT_LOG);
        assertThat(UserRole.NURSE.permissions()).doesNotContain(AppPermission.VIEW_AUDIT_LOG);
        assertThat(UserRole.RECEPTIONIST.permissions()).doesNotContain(AppPermission.VIEW_AUDIT_LOG);
    }

    @Test
    void doctorCanAdvanceClinicalWorkflowButCannotDeleteAdmissions() {
        assertThat(UserRole.DOCTOR.permissions())
                .contains(
                        AppPermission.PATIENT_START_EXAMINATION,
                        AppPermission.PATIENT_COMPLETE_EXAMINATION,
                        AppPermission.ADMISSION_CREATE,
                        AppPermission.ADMISSION_UPDATE,
                        AppPermission.ADMISSION_DISCHARGE)
                .doesNotContain(AppPermission.ADMISSION_DELETE);
    }

    @Test
    void adminCanCreateUpdateAndDeleteAdmissions() {
        assertThat(UserRole.ADMIN.permissions())
                .contains(
                        AppPermission.ADMISSION_CREATE,
                        AppPermission.ADMISSION_UPDATE,
                        AppPermission.ADMISSION_DISCHARGE,
                        AppPermission.ADMISSION_DELETE);
    }

    @Test
    void nurseCannotWriteAdmissions() {
        assertThat(UserRole.NURSE.permissions())
                .doesNotContain(
                        AppPermission.ADMISSION_CREATE,
                        AppPermission.ADMISSION_UPDATE,
                        AppPermission.ADMISSION_DISCHARGE,
                        AppPermission.ADMISSION_DELETE);
    }

    @Test
    void receptionistCanCheckInButCannotWriteAdmissions() {
        assertThat(UserRole.RECEPTIONIST.permissions())
                .contains(AppPermission.PATIENT_CHECK_IN)
                .doesNotContain(
                        AppPermission.PATIENT_START_EXAMINATION,
                        AppPermission.PATIENT_COMPLETE_EXAMINATION,
                        AppPermission.ADMISSION_CREATE,
                        AppPermission.ADMISSION_UPDATE,
                        AppPermission.ADMISSION_DISCHARGE,
                        AppPermission.ADMISSION_DELETE);
    }

    @Test
    void patientCanOnlyChangeOwnPasswordAndViewOwnRecord() {
        assertThat(UserRole.PATIENT.permissions())
                .containsExactlyInAnyOrder(
                        AppPermission.CHANGE_OWN_PASSWORD,
                        AppPermission.VIEW_OWN_RECORD)
                .doesNotContain(
                        AppPermission.VIEW_PATIENT,
                        AppPermission.VIEW_MEDICAL_RECORD,
                        AppPermission.VIEW_PATIENT_HISTORY,
                        AppPermission.VIEW_STATISTICS);
        assertThat(UserRole.ADMIN.permissions()).doesNotContain(AppPermission.VIEW_OWN_RECORD);
    }
}
