package com.usth.hospital.service.impl;

import com.usth.hospital.dto.request.CreateUserRequest;
import com.usth.hospital.dto.request.UpdateUserRequest;
import com.usth.hospital.dto.response.UserResponse;
import com.usth.hospital.exception.DuplicateFieldException;
import com.usth.hospital.exception.InvalidUserOperationException;
import com.usth.hospital.exception.UserNotFoundException;
import com.usth.hospital.model.AppUserEntity;
import com.usth.hospital.model.AuditLogAction;
import com.usth.hospital.model.AuditLogEntityType;
import com.usth.hospital.model.AuditLogValueType;
import com.usth.hospital.repository.AppUserRepository;
import com.usth.hospital.security.CurrentUserProvider;
import com.usth.hospital.security.UserRole;
import com.usth.hospital.service.AuditLogService;
import com.usth.hospital.service.AuditEventCommand;
import com.usth.hospital.service.AuditLogChange;
import com.usth.hospital.service.UserManagementService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserManagementServiceImpl implements UserManagementService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> list(String search, UserRole role, Boolean active, Pageable pageable) {
        String keyword = search == null || search.isBlank() ? null : search.trim();
        Integer activeFlag = active == null ? null : (active ? 1 : 0);
        return appUserRepository.search(keyword, role, activeFlag, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return toResponse(requireUser(id));
    }

    @Override
    public UserResponse create(CreateUserRequest request) {
        requireStaffRole(request.role());
        String identityNo = normalizeIdentityNo(request.identityNo());
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String displayName = normalizeDisplayName(request.displayName());
        requireUnique(identityNo, email, null);

        String actor = currentUserProvider.username();
        LocalDateTime now = LocalDateTime.now();
        AppUserEntity user = AppUserEntity.builder()
                .username(identityNo)
                .email(email)
                .passwordHash(passwordEncoder.encode(identityNo))
                .identityNo(identityNo)
                .tokenVersion(0L)
                .passwordChangeRequired(1)
                .fullName(displayName)
                .role(request.role())
                .activeFlag(1)
                .createdBy(actor)
                .createdDate(now)
                .lastUpdatedBy(actor)
                .lastUpdatedDate(now)
                .build();
        AppUserEntity saved = appUserRepository.save(user);
        List<AuditLogChange> changes = userChanges(null, snapshot(saved));
        auditLogService.record(userCommand(
                AuditLogAction.CREATE,
                saved,
                "Record created",
                changes
        ));
        return toResponse(saved);
    }

    @Override
    public UserResponse update(Long id, UpdateUserRequest request) {
        requireStaffRole(request.role());
        AppUserEntity user = requireUser(id);
        UserSnapshot before = snapshot(user);
        String identityNo = normalizeIdentityNo(request.identityNo());
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String displayName = normalizeDisplayName(request.displayName());
        requireUnique(identityNo, email, id);
        preventLastAdminChange(user, request.role(), user.isActive());

        if (!Objects.equals(user.getIdentityNo(), identityNo)) {
            boolean temporaryPassword = user.isPasswordChangeRequired();
            user.setIdentityNo(identityNo);
            user.setUsername(identityNo);
            user.setTokenVersion(user.getTokenVersion() + 1);
            if (temporaryPassword) {
                user.setPasswordHash(passwordEncoder.encode(identityNo));
            }
        }
        user.setEmail(email);
        user.setFullName(displayName);
        user.setRole(request.role());
        stampUpdate(user);
        AppUserEntity saved = appUserRepository.save(user);
        List<AuditLogChange> changes = userChanges(before, snapshot(saved));
        auditLogService.record(userCommand(
                AuditLogAction.UPDATE,
                saved,
                changeSummary(changes.size()),
                changes
        ));
        return toResponse(saved);
    }

    @Override
    public UserResponse setActive(Long id, boolean active) {
        AppUserEntity user = requireUser(id);
        boolean previousActive = user.isActive();
        if (!active && user.getUsername().equalsIgnoreCase(currentUserProvider.username())) {
            throw new InvalidUserOperationException("active", "You cannot lock your own account");
        }
        preventLastAdminChange(user, user.getRole(), active);

        user.setActive(active);
        stampUpdate(user);
        AppUserEntity saved = appUserRepository.save(user);
        List<AuditLogChange> statusChanges = previousActive == active
                ? List.of()
                : List.of(AuditLogChange.generalReference(
                        "active",
                        AuditLogValueType.BOOLEAN,
                        previousActive,
                        active,
                        activeLabel(previousActive),
                        activeLabel(active),
                        0
                ));
        auditLogService.record(userCommand(
                AuditLogAction.STATUS_UPDATE,
                saved,
                statusChanges.isEmpty()
                        ? "No field changes"
                        : activeLabel(previousActive) + " to " + activeLabel(active),
                statusChanges
        ));
        return toResponse(saved);
    }

    @Override
    public void resetPassword(Long id) {
        AppUserEntity user = requireUser(id);
        String identityNo = user.getIdentityNo();
        if (identityNo == null || !identityNo.matches("^[0-9]{12}$")) {
            throw new InvalidUserOperationException(
                    "identityNo", "A valid 12-digit identity number is required before resetting the password"
            );
        }
        user.setPasswordHash(passwordEncoder.encode(identityNo));
        user.setPasswordChangeRequired(true);
        user.setTokenVersion(user.getTokenVersion() + 1);
        stampUpdate(user);
        appUserRepository.save(user);
        auditLogService.record(userCommand(
                AuditLogAction.PASSWORD_RESET,
                user,
                "Credentials reset and existing sessions revoked",
                List.of()
        ));
    }

    private AppUserEntity requireUser(Long id) {
        return appUserRepository.findById(id)
                .filter(user -> user.getRole() != UserRole.PATIENT)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private static void requireStaffRole(UserRole role) {
        if (role == UserRole.PATIENT) {
            throw new InvalidUserOperationException(
                    "role", "Patient accounts are managed from patient records");
        }
    }

    private void requireUnique(String identityNo, String email, Long currentId) {
        boolean usernameTaken = currentId == null
                ? appUserRepository.existsByUsernameIgnoreCase(identityNo)
                : appUserRepository.existsByUsernameIgnoreCaseAndIdNot(identityNo, currentId);
        if (usernameTaken) {
            throw new DuplicateFieldException("identityNo", "Identity number already exists");
        }

        boolean emailTaken = currentId == null
                ? appUserRepository.existsByEmailIgnoreCase(email)
                : appUserRepository.existsByEmailIgnoreCaseAndIdNot(email, currentId);
        if (emailTaken) {
            throw new DuplicateFieldException("email", "Email already exists");
        }
    }

    private String normalizeDisplayName(String displayName) {
        String normalized = displayName.trim();
        if (normalized.length() < 2) {
            throw new InvalidUserOperationException(
                    "displayName", "Display name must contain at least 2 characters"
            );
        }
        return normalized;
    }

    private String normalizeIdentityNo(String identityNo) {
        String normalized = identityNo == null ? "" : identityNo.trim();
        if (!normalized.matches("^[0-9]{12}$")) {
            throw new InvalidUserOperationException(
                    "identityNo", "Identity number must contain exactly 12 digits"
            );
        }
        return normalized;
    }

    private void preventLastAdminChange(AppUserEntity user, UserRole targetRole, boolean targetActive) {
        boolean removesActiveAdmin = user.isActive()
                && user.getRole() == UserRole.ADMIN
                && (!targetActive || targetRole != UserRole.ADMIN);
        if (removesActiveAdmin
                && appUserRepository.lockActiveUsersByRole(UserRole.ADMIN).size() <= 1) {
            throw new InvalidUserOperationException("role", "At least one active administrator is required");
        }
    }

    private void stampUpdate(AppUserEntity user) {
        user.setLastUpdatedBy(currentUserProvider.username());
        user.setLastUpdatedDate(LocalDateTime.now());
    }

    private UserResponse toResponse(AppUserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getIdentityNo(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.isPasswordChangeRequired(),
                user.getCreatedBy(),
                user.getCreatedDate(),
                user.getLastUpdatedBy(),
                user.getLastUpdatedDate()
        );
    }

    private static AuditEventCommand userCommand(
            AuditLogAction action,
            AppUserEntity user,
            String summary,
            List<AuditLogChange> changes
    ) {
        return AuditEventCommand.builder()
                .action(action)
                .entityType(AuditLogEntityType.USER)
                .entityId(String.valueOf(user.getId()))
                .targetLabel(userLabel(user))
                .summary(summary)
                .changes(changes)
                .build();
    }

    private static List<AuditLogChange> userChanges(UserSnapshot before, UserSnapshot after) {
        List<AuditLogChange> changes = new ArrayList<>();
        addPersonalChange(changes, "identityNo",
                value(before, UserSnapshot::identityNo), value(after, UserSnapshot::identityNo), 0);
        addPersonalChange(changes, "email",
                value(before, UserSnapshot::email), value(after, UserSnapshot::email), 1);
        addGeneralChange(changes, "displayName", AuditLogValueType.TEXT,
                value(before, UserSnapshot::displayName), value(after, UserSnapshot::displayName), 2);
        addRoleChange(changes,
                value(before, UserSnapshot::role), value(after, UserSnapshot::role), 3);
        addActiveChange(changes,
                value(before, UserSnapshot::active), value(after, UserSnapshot::active), 4);
        return changes;
    }

    private static void addGeneralChange(
            List<AuditLogChange> changes,
            String fieldKey,
            AuditLogValueType valueType,
            Object oldValue,
            Object newValue,
            int displayOrder
    ) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(AuditLogChange.general(
                    fieldKey, valueType, oldValue, newValue, displayOrder));
        }
    }

    private static void addPersonalChange(
            List<AuditLogChange> changes,
            String fieldKey,
            Object oldValue,
            Object newValue,
            int displayOrder
    ) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(AuditLogChange.personal(
                    fieldKey, AuditLogValueType.TEXT, oldValue, newValue, displayOrder));
        }
    }

    private static void addRoleChange(
            List<AuditLogChange> changes,
            UserRole oldValue,
            UserRole newValue,
            int displayOrder
    ) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(AuditLogChange.generalReference(
                    "role",
                    AuditLogValueType.REFERENCE,
                    oldValue,
                    newValue,
                    roleLabel(oldValue),
                    roleLabel(newValue),
                    displayOrder
            ));
        }
    }

    private static void addActiveChange(
            List<AuditLogChange> changes,
            Boolean oldValue,
            Boolean newValue,
            int displayOrder
    ) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(AuditLogChange.generalReference(
                    "active",
                    AuditLogValueType.BOOLEAN,
                    oldValue,
                    newValue,
                    activeLabel(oldValue),
                    activeLabel(newValue),
                    displayOrder
            ));
        }
    }

    private static UserSnapshot snapshot(AppUserEntity user) {
        return new UserSnapshot(
                user.getIdentityNo(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive()
        );
    }

    private static String userLabel(AppUserEntity user) {
        return user.getFullName() + " · " + roleLabel(user.getRole());
    }

    private static String changeSummary(int count) {
        if (count == 0) {
            return "No field changes";
        }
        return count + (count == 1 ? " field changed" : " fields changed");
    }

    private static String roleLabel(UserRole role) {
        if (role == null) {
            return null;
        }
        String value = role.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String activeLabel(Boolean active) {
        return active == null ? null : active ? "Active" : "Locked";
    }

    private static <T> T value(UserSnapshot snapshot, java.util.function.Function<UserSnapshot, T> field) {
        return snapshot == null ? null : field.apply(snapshot);
    }

    private record UserSnapshot(
            String identityNo,
            String email,
            String displayName,
            UserRole role,
            boolean active
    ) {
    }
}
