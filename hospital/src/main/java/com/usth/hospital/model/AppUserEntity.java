package com.usth.hospital.model;

import com.usth.hospital.security.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "APP_USERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_user_generator")
    @SequenceGenerator(name = "app_user_generator", sequenceName = "app_user_seq", allocationSize = 1)
    @Column(name = "app_user_id", nullable = false)
    private Long id;

    @Column(name = "app_user_username", length = 30, nullable = false)
    private String username;

    @Column(name = "app_user_email", length = 100)
    private String email;

    @Column(name = "app_user_password_hash", length = 100, nullable = false)
    private String passwordHash;

    @Column(name = "app_user_identity_no", length = 12)
    private String identityNo;

    @Builder.Default
    @Column(name = "app_user_token_version", nullable = false)
    private Long tokenVersion = 0L;

    @Column(name = "app_user_patient_id", length = 50)
    private String patientId;

    @Column(name = "app_user_doctor_id")
    private Long doctorId;

    @Builder.Default
    @Column(name = "app_user_password_change_required", nullable = false)
    private Integer passwordChangeRequired = 0;

    @Column(name = "app_user_full_name", length = 100, nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_user_role", length = 20, nullable = false)
    private UserRole role;

    @Column(name = "app_user_active_flag", nullable = false)
    private Integer activeFlag;

    @Column(name = "app_user_created_by", length = 30, nullable = false)
    private String createdBy;

    @Column(name = "app_user_created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "app_user_last_updated_by", length = 30, nullable = false)
    private String lastUpdatedBy;

    @Column(name = "app_user_last_updated_date", nullable = false)
    private LocalDateTime lastUpdatedDate;

    public boolean isActive() {
        return Integer.valueOf(1).equals(activeFlag);
    }

    public void setActive(boolean active) {
        activeFlag = active ? 1 : 0;
    }

    public boolean isPasswordChangeRequired() {
        return Integer.valueOf(1).equals(passwordChangeRequired);
    }

    public void setPasswordChangeRequired(boolean required) {
        passwordChangeRequired = required ? 1 : 0;
    }
}
