package com.usth.hospital.service.impl;

import com.usth.hospital.dto.request.CreateStaffDoctorRequest;
import com.usth.hospital.dto.response.StaffWorkProfileResponse;
import com.usth.hospital.exception.DuplicateFieldException;
import com.usth.hospital.exception.InvalidUserOperationException;
import com.usth.hospital.exception.UserNotFoundException;
import com.usth.hospital.model.*;
import com.usth.hospital.repository.*;
import com.usth.hospital.security.*;
import com.usth.hospital.service.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffWorkProfileService {
    private final AppUserRepository users;
    private final DoctorRepository doctors;
    private final CareAssignmentRepository assignments;
    private final CarePointRepository points;
    private final AdmissionRepository references;
    private final UserManagementService userManagement;
    private final CurrentUserProvider currentUser;
    private final AuditLogService audit;

    public StaffWorkProfileResponse get(Long id) {
        AppUserEntity user = staff(users.findById(id), id);
        var departments = references.findDepartmentOptions().stream().collect(Collectors.toMap(
                AdmissionRepository.DepartmentRow::getId, AdmissionRepository.DepartmentRow::getName));
        var rows = assignments.findByUserIdOrderByIdAsc(id);
        var pointMap = points.findAllById(rows.stream().map(CareAssignmentEntity::getPointId).toList())
                .stream().collect(Collectors.toMap(CarePointEntity::getId, Function.identity()));
        var work = rows.stream().map(a -> {
            CarePointEntity point = pointMap.get(a.getPointId());
            return new StaffWorkProfileResponse.Assignment(a.getId(), a.getPointId(),
                    point == null ? "Unavailable point" : point.getName(),
                    point == null ? null : point.getKind(),
                    point == null ? null : departments.get(point.getDepartmentId()),
                    point != null && Integer.valueOf(1).equals(point.getActive()),
                    Integer.valueOf(1).equals(a.getActive()), Integer.valueOf(1).equals(a.getCanPerform()),
                    Integer.valueOf(1).equals(a.getCanVerify()));
        }).toList();
        DoctorEntity doctor = user.getDoctorId() == null ? null : doctors.findById(user.getDoctorId()).orElse(null);
        var doctorResponse = doctor == null ? null : new StaffWorkProfileResponse.Doctor(
                doctor.getId(), doctor.getCode(), doctor.getName(), doctor.getSpecialization(),
                doctor.getDepartmentId(), departments.get(doctor.getDepartmentId()),
                Integer.valueOf(1).equals(doctor.getStatus()));
        List<String> issues = new ArrayList<>();
        if (user.getRole() == UserRole.DOCTOR) {
            if (doctor == null) issues.add("DOCTOR_LINK_MISSING");
            else if (!doctorResponse.active()) issues.add("DOCTOR_UNAVAILABLE");
        }
        if (Set.of(UserRole.DOCTOR, UserRole.NURSE, UserRole.TECHNICIAN).contains(user.getRole())) {
            var activeWork = work.stream().filter(a -> a.active() && a.pointActive()).toList();
            if (activeWork.isEmpty()) issues.add("POINT_ASSIGNMENT_MISSING");
            else if (user.getRole() != UserRole.NURSE && activeWork.stream().noneMatch(a ->
                    a.perform() || a.verify() && "SERVICE".equals(a.pointKind()))) {
                issues.add("CLINICAL_CAPABILITY_MISSING");
            }
        }
        return new StaffWorkProfileResponse(userManagement.get(id),
                user.getRole().permissions().stream().map(Enum::name).sorted().toList(),
                doctorResponse, work, List.copyOf(issues));
    }

    @Transactional
    public StaffWorkProfileResponse.Doctor createDoctor(Long id, CreateStaffDoctorRequest request) {
        AppUserEntity user = staff(users.findByIdForUpdate(id), id);
        if (!user.getRole().permissions().contains(AppPermission.CARE_EXAMINE)) {
            throw new InvalidUserOperationException("role", "Only a clinical doctor account can have a doctor profile");
        }
        if (user.getDoctorId() != null) {
            throw CareAccessService.conflict("This account already has a linked doctor profile");
        }
        var department = references.findDepartment(request.departmentId()).orElseThrow(() ->
                new InvalidUserOperationException("departmentId", "Choose an existing department"));
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (doctors.existsByCodeIgnoreCase(code)) throw new DuplicateFieldException("code", "Doctor code is already used");
        DoctorEntity doctor = DoctorEntity.builder().code(code).name(request.name().trim())
                .departmentId(request.departmentId()).specialization(clean(request.specialization()))
                .phoneNo(clean(request.phoneNo())).email(clean(request.email())).status(1).build();
        try {
            doctors.saveAndFlush(doctor);
        } catch (DataIntegrityViolationException ex) {
            throw CareAccessService.conflict("Doctor profile could not be created. Check its code and department, then retry");
        }
        user.setDoctorId(doctor.getId());
        user.setLastUpdatedBy(currentUser.username());
        user.setLastUpdatedDate(LocalDateTime.now());
        users.saveAndFlush(user);
        audit.record(AuditEventCommand.builder().action(AuditLogAction.UPDATE).entityType(AuditLogEntityType.USER)
                .entityId(String.valueOf(id)).targetLabel(user.getFullName())
                .summary("Doctor profile created and linked")
                .changes(List.of(AuditLogChange.generalReference("doctorId", AuditLogValueType.REFERENCE,
                        null, doctor.getId(), null, doctor.getCode() + " · " + doctor.getName(), 0)))
                .build());
        return new StaffWorkProfileResponse.Doctor(doctor.getId(), doctor.getCode(), doctor.getName(),
                doctor.getSpecialization(), department.getId(), department.getName(), true);
    }

    private static AppUserEntity staff(Optional<AppUserEntity> result, Long id) {
        return result.filter(u -> u.getPatientId() == null && u.getRole() != UserRole.PATIENT)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
