package com.usth.hospital.service.impl;

import com.usth.hospital.dto.request.CareRequests;
import com.usth.hospital.model.*;
import com.usth.hospital.repository.*;
import com.usth.hospital.security.*;
import com.usth.hospital.service.*;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.usth.hospital.service.impl.CareAccessService.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CareConfigurationService {
    private final CareAccessService access;
    private final CarePointRepository points;
    private final CareServiceRepository services;
    private final CareAssignmentRepository assignments;
    private final AppUserRepository users;
    private final AdmissionRepository references;
    private final AuditLogService audit;

    public void point(Long id,CareRequests.Point request) {
        access.require(access.actor(),AppPermission.CARE_MANAGE);
        if (references.findDepartment(request.departmentId()).isEmpty()) throw invalid("Department is unavailable");
        if (points.findAll().stream().anyMatch(p -> p.getCode().equals(request.code()) && !Objects.equals(p.getId(),id))) throw conflict("Point code is already used");
        CarePointEntity p=id==null?new CarePointEntity():points.findById(id).orElseThrow(() -> missing("Point"));
        if (id!=null && (!p.getKind().equals(request.kind()) || !p.getDepartmentId().equals(request.departmentId())))
            throw invalid("Create a new point when changing its type or department, to preserve historical assignments");
        p.setCode(request.code()); p.setName(request.name().trim()); p.setKind(request.kind());
        p.setDepartmentId(request.departmentId()); p.setActive(request.active()?1:0); points.saveAndFlush(p);
        event(p.getId(),id==null?AuditLogAction.CREATE:AuditLogAction.UPDATE,"Care point configuration saved: "+p.getCode());
    }

    public void service(Long id,CareRequests.Service request) {
        access.require(access.actor(),AppPermission.CARE_MANAGE);
        if (points.findById(request.pointId()).filter(p -> "SERVICE".equals(p.getKind()) && p.getActive()==1).isEmpty()) throw invalid("Choose an active service point");
        if (services.findAll().stream().anyMatch(s -> s.getCode().equals(request.code()) && !Objects.equals(s.getId(),id))) throw conflict("Service code is already used");
        CareServiceEntity s=id==null?new CareServiceEntity():services.findById(id).orElseThrow(() -> missing("Service"));
        s.setCode(request.code()); s.setName(request.name().trim()); s.setPointId(request.pointId()); s.setActive(request.active()?1:0); services.saveAndFlush(s);
        event(s.getId(),id==null?AuditLogAction.CREATE:AuditLogAction.UPDATE,"Service catalog configuration saved: "+s.getCode());
    }

    public void assignment(CareRequests.Assignment request) {
        access.require(access.actor(),AppPermission.CARE_MANAGE);
        AppUserEntity user=users.findByIdForUpdate(request.userId()).filter(u -> u.getPatientId()==null && u.getRole()!=UserRole.PATIENT).orElseThrow(() -> invalid("Choose a staff account"));
        CarePointEntity point=points.findById(request.pointId()).orElseThrow(() -> invalid("Point is unavailable"));
        if (request.active() && (!user.isActive()||point.getActive()!=1)) throw invalid("Account and point must be active");
        if (request.perform() && !access.has(user,AppPermission.CARE_PERFORM)) throw invalid("This role cannot perform clinical work");
        if (request.verify() && (!access.has(user,AppPermission.CARE_VERIFY)||!"SERVICE".equals(point.getKind()))) throw invalid("Result validation requires a clinical validator at a service point");
        if (request.doctorId()!=null) {
            if (!access.has(user,AppPermission.CARE_EXAMINE)) throw invalid("Only a clinical doctor account can link to a doctor record");
            if (user.getDoctorId()!=null && !user.getDoctorId().equals(request.doctorId())) throw conflict("This account already represents another doctor");
            if (references.findDoctor(request.doctorId()).filter(d -> d.getStatus()==1).isEmpty()) throw invalid("Doctor record is inactive or unavailable");
            if (users.existsByDoctorIdAndIdNot(request.doctorId(),user.getId())) throw conflict("Doctor record is linked to another staff account");
            user.setDoctorId(request.doctorId());
        }
        if (request.perform() && access.has(user,AppPermission.CARE_EXAMINE) && user.getDoctorId()==null) throw invalid("Link the doctor account to its doctor record first");
        CareAssignmentEntity a=assignments.findByUserIdAndPointId(user.getId(),point.getId()).orElseGet(CareAssignmentEntity::new);
        boolean create=a.getId()==null;
        a.setUserId(user.getId()); a.setPointId(point.getId()); a.setCanPerform(request.perform()?1:0);
        a.setCanVerify(request.verify()?1:0); a.setActive(request.active()?1:0); assignments.saveAndFlush(a);
        event(a.getId(),create?AuditLogAction.CREATE:AuditLogAction.UPDATE,"Staff assignment saved at "+point.getCode()+"; perform="+request.perform()+", verify="+request.verify()+", active="+request.active());
    }

    private void event(Long id,AuditLogAction action,String summary) {
        audit.record(AuditEventCommand.builder().entityType(AuditLogEntityType.CARE_ASSIGNMENT).entityId(String.valueOf(id))
            .action(action).targetLabel("Care configuration").summary(summary).build());
    }
}
