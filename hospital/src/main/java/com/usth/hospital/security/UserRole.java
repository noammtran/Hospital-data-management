package com.usth.hospital.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum UserRole {
    RECEPTIONIST(EnumSet.of(
            AppPermission.CARE_VIEW,
            AppPermission.CARE_REGISTER,
            AppPermission.VIEW_PATIENT,
            AppPermission.CREATE_PATIENT,
            AppPermission.UPDATE_PATIENT,
            AppPermission.PATIENT_CHECK_IN,
            AppPermission.CHANGE_OWN_PASSWORD
    )),
    DOCTOR(EnumSet.of(
            AppPermission.CARE_VIEW,
            AppPermission.CARE_EXAMINE,
            AppPermission.CARE_PERFORM,
            AppPermission.CARE_VERIFY,
            AppPermission.CARE_COORDINATE,
            AppPermission.VIEW_PATIENT,
            AppPermission.VIEW_MEDICAL_RECORD,
            AppPermission.VIEW_STATISTICS,
            AppPermission.PATIENT_START_EXAMINATION,
            AppPermission.PATIENT_COMPLETE_EXAMINATION,
            AppPermission.ADMISSION_CREATE,
            AppPermission.ADMISSION_UPDATE,
            AppPermission.ADMISSION_DISCHARGE,
            AppPermission.CHANGE_OWN_PASSWORD
    )),
    NURSE(EnumSet.of(
            AppPermission.CARE_VIEW,
            AppPermission.CARE_COORDINATE,
            AppPermission.CARE_PERFORM,
            AppPermission.CARE_ADMIT,
            AppPermission.VIEW_PATIENT,
            AppPermission.VIEW_MEDICAL_RECORD,
            AppPermission.CHANGE_OWN_PASSWORD
    )),
    TECHNICIAN(EnumSet.of(
            AppPermission.CARE_VIEW,
            AppPermission.CARE_COORDINATE,
            AppPermission.CARE_PERFORM,
            AppPermission.CARE_VERIFY,
            AppPermission.CHANGE_OWN_PASSWORD
    )),
    ADMIN(EnumSet.complementOf(EnumSet.of(AppPermission.VIEW_OWN_RECORD))),
    PATIENT(EnumSet.of(
            AppPermission.VIEW_OWN_RECORD,
            AppPermission.CHANGE_OWN_PASSWORD
    ));

    private final Set<AppPermission> permissions;

    UserRole(Set<AppPermission> permissions) {
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public Set<AppPermission> permissions() {
        return permissions;
    }
}
