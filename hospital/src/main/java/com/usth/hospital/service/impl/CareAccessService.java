package com.usth.hospital.service.impl;

import com.usth.hospital.exception.CareWorkflowException;
import com.usth.hospital.model.*;
import com.usth.hospital.repository.*;
import com.usth.hospital.security.*;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CareAccessService {
    private final CurrentUserProvider currentUser;
    private final AppUserRepository users;
    private final CareAssignmentRepository assignments;
    private final CareTaskRepository tasks;
    private final CareHandoffRepository handoffs;
    private final CareVisitRepository visits;
    private final PatientRepository patients;
    private final AdmissionRepository admissions;

    public AppUserEntity actor() {
        AppUserEntity user = users.findByUsernameIgnoreCase(currentUser.username()).orElseThrow(CareAccessService::denied);
        if (!user.isActive() || user.isPasswordChangeRequired() || user.getRole()==UserRole.PATIENT) throw denied();
        return user;
    }
    public void require(AppUserEntity user, AppPermission permission) {
        if (!user.getRole().permissions().contains(permission)) throw denied();
    }
    public boolean has(AppUserEntity user, AppPermission permission) {
        return user.getRole().permissions().contains(permission);
    }
    public boolean allStaff(AppUserEntity user) {
        return user.getRole()==UserRole.ADMIN || user.getRole()==UserRole.RECEPTIONIST;
    }
    public boolean assigned(AppUserEntity user, Long pointId) {
        return pointId!=null && assignments.findByUserIdAndPointId(user.getId(), pointId)
                .filter(a -> Integer.valueOf(1).equals(a.getActive())).isPresent();
    }
    public boolean perform(AppUserEntity user, Long pointId) {
        return has(user,AppPermission.CARE_PERFORM) && assignments.findByUserIdAndPointId(user.getId(),pointId)
                .filter(a -> Integer.valueOf(1).equals(a.getActive()) && Integer.valueOf(1).equals(a.getCanPerform())).isPresent();
    }
    public boolean verify(AppUserEntity user, Long pointId) {
        return has(user,AppPermission.CARE_VERIFY) && assignments.findByUserIdAndPointId(user.getId(),pointId)
                .filter(a -> Integer.valueOf(1).equals(a.getActive()) && Integer.valueOf(1).equals(a.getCanVerify())).isPresent();
    }
    public boolean coordinate(AppUserEntity user, Long pointId) {
        return has(user,AppPermission.CARE_COORDINATE) && (user.getRole()==UserRole.ADMIN || assigned(user,pointId));
    }
    public boolean examine(AppUserEntity user, Long pointId) {
        return has(user,AppPermission.CARE_EXAMINE) && user.getDoctorId()!=null && perform(user,pointId)
                && admissions.findDoctor(user.getDoctorId()).filter(d -> Integer.valueOf(1).equals(d.getStatus())).isPresent();
    }
    public boolean owner(AppUserEntity user, CareVisitEntity visit) {
        return Objects.equals(user.getId(),visit.getOwnerId()) && examine(user,visit.getPointId());
    }
    public boolean clinical(AppUserEntity user, CareVisitEntity visit) {
        if (!has(user,AppPermission.VIEW_MEDICAL_RECORD)) return false;
        return user.getRole()==UserRole.ADMIN || Objects.equals(user.getId(),visit.getOwnerId())
            || Objects.equals(user.getId(),visit.getFollowupOwnerId()) || assigned(user,visit.getPointId())
            || assigned(user,visit.getWardPointId()) || handoffs.findByVisitIdOrderByCreatedAtAsc(visit.getId()).stream()
                .anyMatch(h -> Objects.equals(h.getToUserId(),user.getId()) && assigned(user,h.getPointId()));
    }
    public void requireRead(AppUserEntity user, CareVisitEntity visit) {
        require(user,AppPermission.CARE_VIEW);
        if (!allStaff(user) && !clinical(user,visit) && tasks.findByVisitIdOrderByCreatedAtAscIdAsc(visit.getId()).stream()
                .noneMatch(t -> assigned(user,t.getPointId()))) throw denied();
    }
    public void requireOwner(AppUserEntity user, CareVisitEntity visit) {
        if (!owner(user,visit)) throw denied();
    }
    // All visit/task/result writes take the same patient lock as patient/admission mutations.
    // Scalar ID reads avoid retaining a stale entity before acquiring that lock.
    public CareVisitEntity lockVisit(String id) {
        String patientId=visits.patientId(id).orElseThrow(() -> missing("Visit"));
        patients.findByIdForUpdate(patientId).orElseThrow(() -> missing("Patient"));
        return visits.findById(id).orElseThrow(() -> missing("Visit"));
    }
    public CareVisitEntity lockTaskVisit(Long id) {
        return lockVisit(tasks.visitId(id).orElseThrow(() -> missing("Work item")));
    }
    public static void active(CareVisitEntity visit) {
        if (visit.getClosedAt()!=null) throw conflict("The visit is closed");
    }
    public static AccessDeniedException denied() { return new AccessDeniedException("Care assignment required"); }
    public static CareWorkflowException missing(String resource) { return new CareWorkflowException(404,resource+" was not found"); }
    public static CareWorkflowException conflict(String message) { return new CareWorkflowException(409,message); }
    public static CareWorkflowException invalid(String message) { return new CareWorkflowException(400,message); }
}
