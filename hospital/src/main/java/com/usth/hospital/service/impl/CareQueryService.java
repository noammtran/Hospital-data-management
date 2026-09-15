package com.usth.hospital.service.impl;

import com.usth.hospital.dto.response.CareResponses;
import com.usth.hospital.dto.response.PageResponse;
import com.usth.hospital.model.*;
import com.usth.hospital.repository.*;
import com.usth.hospital.security.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.usth.hospital.service.impl.CareAccessService.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class CareQueryService {
    private final CareAccessService access;
    private final CareVisitRepository visits;
    private final CareTaskRepository tasks;
    private final CareResultRepository results;
    private final CareNoteRepository notes;
    private final CareHandoffRepository handoffs;
    private final CarePointRepository points;
    private final CareServiceRepository services;
    private final CareAssignmentRepository assignments;
    private final AppUserRepository users;
    private final PatientRepository patients;
    private final AdmissionRepository admissions;
    private final CareReferenceRepository references;
    private final DiagnosisRepository diagnoses;
    private final PrescriptionRepository prescriptions;

    public CareResponses.Options options() {
        AppUserEntity actor=access.actor(); access.require(actor,AppPermission.CARE_VIEW);
        return new CareResponses.Options(actor.getId(),points.findAllByOrderByNameAsc().stream().map(p ->
            new CareResponses.Point(p.getId(),p.getCode(),p.getName(),p.getKind(),p.getDepartmentId(),p.getActive()==1)).toList(),
            services.findAllByOrderByNameAsc().stream().map(s -> new CareResponses.Service(s.getId(),s.getCode(),s.getName(),s.getPointId(),s.getActive()==1)).toList(),
            users.findByPatientIdIsNullOrderByFullNameAsc().stream().map(u -> new CareResponses.Staff(u.getId(),u.getFullName(),u.getRole().name(),u.getDoctorId(),u.isActive())).toList(),
            assignments.findAll().stream().map(a -> new CareResponses.Assignment(a.getId(),a.getUserId(),a.getPointId(),a.getCanPerform()==1,a.getCanVerify()==1,a.getActive()==1)).toList(),
            references.departments().stream().map(r -> new CareResponses.Reference(r.getId(),r.getName())).toList(),
            references.doctors().stream().map(r -> new CareResponses.Reference(r.getId(),r.getName())).toList(),
            admissions.findAvailableRoomOptions().stream().map(r -> new CareResponses.Room(r.getId(),r.getName(),r.getDepartmentId(),r.getBedCount()-r.getCurrentOccupancy())).toList());
    }

    public PageResponse<CareResponses.Visit> visits(String patientId, Long pointId, String status, boolean openOnly, String search, int page, int pageSize) {
        AppUserEntity actor=access.actor(); access.require(actor,AppPermission.CARE_VIEW);
        var rows=visits.search(access.allStaff(actor),actor.getId(),clean(patientId),pointId,clean(status),openOnly,clean(search),PageRequest.of(page-1,pageSize));
        Context context=context(rows.getContent());
        return PageResponse.from(rows.map(v -> visit(v,context)));
    }

    public PageResponse<CareResponses.Task> tasks(Long pointId,String kind,String status,String arrival,boolean openOnly,String search,int page,int pageSize) {
        AppUserEntity actor=access.actor(); access.require(actor,AppPermission.CARE_VIEW);
        var rows=tasks.search(access.allStaff(actor),actor.getId(),pointId,clean(kind),clean(status),clean(arrival),openOnly,clean(search),PageRequest.of(page-1,pageSize));
        List<CareVisitEntity> visitRows=visits.findAllById(rows.map(CareTaskEntity::getVisitId).toList());
        Context context=context(visitRows);
        Map<String,CareVisitEntity> visitMap=visitRows.stream().collect(Collectors.toMap(CareVisitEntity::getId,Function.identity()));
        return PageResponse.from(rows.map(t -> task(t,visitMap.get(t.getVisitId()),actor,context)));
    }

    public PageResponse<CareResponses.Inpatient> inpatients(Long departmentId,String search,int page,int pageSize) {
        AppUserEntity actor=access.actor(); access.require(actor,AppPermission.CARE_VIEW);
        return PageResponse.from(references.inpatients(access.allStaff(actor)?1:0,actor.getId(),departmentId,clean(search),PageRequest.of(page-1,pageSize))
            .map(r -> new CareResponses.Inpatient(r.getAdmissionId(),r.getPatientId(),r.getPatientName(),r.getPatientStatus(),r.getDepartmentName(),r.getDoctorName(),r.getRoomName(),r.getDepartmentId(),r.getVisitId())));
    }

    public PageResponse<CareResponses.Reference> medicines(String search,int page,int pageSize) {
        AppUserEntity actor=access.actor(); access.require(actor,AppPermission.CARE_EXAMINE);
        return PageResponse.from(references.medicines(clean(search),PageRequest.of(page-1,pageSize)).map(r -> new CareResponses.Reference(r.getId(),r.getName())));
    }

    public String taskVisit(Long id) { return tasks.visitId(id).orElseThrow(() -> missing("Work item")); }

    public CareResponses.Detail detail(String id) {
        AppUserEntity actor=access.actor(); CareVisitEntity v=visits.findById(id).orElseThrow(() -> missing("Visit"));
        access.requireRead(actor,v);
        return detail(actor,v,context(List.of(v)));
    }

    /** Reuse the workspace's clinical visibility boundary for the consolidated record. */
    public List<CareResponses.Detail> medicalRecords(List<CareVisitEntity> rows) {
        if (rows.isEmpty()) return List.of();
        AppUserEntity actor=access.actor();
        access.require(actor,AppPermission.VIEW_MEDICAL_RECORD);
        var visible=rows.stream().filter(v -> access.clinical(actor,v)).toList();
        Context context=context(visible);
        return visible.stream().map(v -> detail(actor,v,context)).toList();
    }

    /** Called only after the portal resolves the patient from the authenticated account. */
    List<CareResponses.Detail> patientMedicalRecords(List<CareVisitEntity> rows) {
        Context context=context(rows);
        return rows.stream().map(v -> {
            boolean completed="COMPLETED".equals(v.getStatus()) && v.getClosedAt()!=null;
            List<CareResponses.Task> serviceRows=completed?context.tasks().getOrDefault(v.getId(),List.of()).stream()
                .filter(t -> "ORDER".equals(t.getKind())).map(t -> {
                    CareResultEntity latest=context.results().getOrDefault(t.getId(),List.of()).stream()
                        .filter(r -> r.getPublishedAt()!=null)
                        .max(Comparator.comparing(CareResultEntity::getRevision)).orElse(null);
                    List<CareResponses.Result> published=latest!=null && latest.getReadAt()!=null
                        ?List.of(new CareResponses.Result(latest.getId(),latest.getRevision(),latest.getBody(),
                            latest.getCritical()==1,latest.getCorrectionReason(),context.user(latest.getCreatedBy()),
                            latest.getCreatedAt(),context.user(latest.getPublishedBy()),latest.getPublishedAt(),
                            context.user(latest.getReadBy()),latest.getReadAt())):List.of();
                    return new CareResponses.Task(t.getId(),v.getId(),v.getPatientId(),null,0,t.getPointId(),
                        context.point(t.getPointId()),t.getKind(),t.getServiceId(),context.service(t.getServiceId()),
                        latest!=null && latest.getReadAt()==null?"AWAITING_REVIEW":t.getStatus(),null,t.getReason(),
                        null,null,t.getCreatedAt(),null,null,null,null,null,published,List.of());
                }).toList():List.of();
            return new CareResponses.Detail(visit(v,context),v.getComplaint(),
                completed?v.getConclusion():null,completed?v.getPlan():null,completed?v.getOutcome():null,
                completed?v.getFollowupPlan():null,completed?v.getFollowupAt():null,null,
                serviceRows,List.of(),List.of(),List.of(),List.of());
        }).toList();
    }

    private CareResponses.Detail detail(AppUserEntity actor,CareVisitEntity v,Context context) {
        String id=v.getId();
        boolean clinical=access.clinical(actor,v);
        List<CareResponses.Task> work=context.tasks().getOrDefault(id,List.of()).stream()
            .filter(t -> clinical || access.allStaff(actor) || access.assigned(actor,t.getPointId()))
            .map(t -> task(t,v,actor,context)).toList();
        List<CareResponses.Note> noteRows=clinical?notes.findByVisitIdOrderByCreatedAtAscIdAsc(id).stream()
            .map(n -> new CareResponses.Note(n.getId(),n.getKind(),n.getBody(),context.user(n.getCreatedBy()),n.getCreatedAt())).toList():List.of();
        List<CareResponses.Handoff> handoffRows=clinical?handoffs.findByVisitIdOrderByCreatedAtAsc(id).stream()
            .map(h -> new CareResponses.Handoff(h.getId(),h.getKind(),h.getStatus(),context.user(h.getFromUserId()),h.getToUserId(),context.user(h.getToUserId()),h.getPointId(),context.point(h.getPointId()),h.getReason(),h.getResponse(),h.getCreatedAt(),h.getRespondedAt(),
                "PENDING".equals(h.getStatus()) && Objects.equals(h.getToUserId(),actor.getId()) && access.examine(actor,h.getPointId()))).toList():List.of();
        List<CareResponses.Diagnosis> diagnosisRows=List.of();
        if (clinical) {
            var rx=prescriptions.findByVisitIdOrderByDateAsc(id).stream().collect(Collectors.groupingBy(PrescriptionEntity::getDiagnosisId));
            diagnosisRows=diagnoses.findByVisitIdOrderByDateAsc(id).stream().map(d -> new CareResponses.Diagnosis(d.getId(),d.getDescription(),d.getIcdCode(),references.doctorName(d.getDoctorId()),
                rx.getOrDefault(d.getId(),List.of()).stream().map(p -> new CareResponses.Prescription(p.getId(),references.medicineName(p.getMedicineId()),p.getDosage(),p.getFrequency(),p.getDuration(),p.getInstructions())).toList())).toList();
        }
        return new CareResponses.Detail(visit(v,context),clinical||access.allStaff(actor)?v.getComplaint():null,
            clinical?v.getConclusion():null,clinical?v.getPlan():null,v.getOutcome(),clinical?v.getFollowupPlan():null,
            v.getFollowupAt(),context.user(v.getFollowupOwnerId()),work,noteRows,handoffRows,diagnosisRows,visitActions(actor,v,context));
    }

    private List<String> visitActions(AppUserEntity actor,CareVisitEntity v,Context context) {
        List<String> actions=new ArrayList<>();
        boolean owner=access.owner(actor,v), active=v.getClosedAt()==null;
        if ("WAITING".equals(v.getStatus()) && (access.has(actor,AppPermission.CARE_REGISTER)||access.coordinate(actor,v.getPointId()))) {
            actions.add("ROUTE"); actions.add("CANCEL_VISIT");
        }
        if (active && (access.coordinate(actor,v.getPointId())||access.coordinate(actor,v.getWardPointId()))) actions.add("NOTE");
        if (active && owner) { actions.add("ORDER"); actions.add("DIAGNOSIS"); actions.add("REVIEW"); }
        if (owner && !active) actions.add("REVIEW");
        if (owner && "ACTIVE".equals(v.getStatus())) { actions.add("FINISH"); actions.add("REQUEST_ADMISSION"); }
        if (owner && "AWAITING_ADMISSION".equals(v.getStatus())) actions.add("CANCEL_ADMISSION");
        if ("AWAITING_ADMISSION".equals(v.getStatus()) && access.has(actor,AppPermission.CARE_ADMIT) && access.coordinate(actor,v.getWardPointId())) actions.add("ACCEPT_ADMISSION");
        if (owner && "INPATIENT".equals(v.getStatus())) actions.add("DISCHARGE");
        if ("INPATIENT".equals(v.getStatus()) && access.coordinate(actor,v.getWardPointId())) actions.add("WARD_ARRIVAL");
        boolean remaining=context.tasks().getOrDefault(v.getId(),List.of()).stream().anyMatch(t -> "ORDER".equals(t.getKind()) && t.getClosedAt()==null);
        if (v.getOwnerId()!=null && (active||remaining) && !"AWAITING_ADMISSION".equals(v.getStatus())
                && (owner || access.has(actor,AppPermission.CARE_MANAGE))) actions.add("HANDOFF");
        return actions;
    }

    private CareResponses.Visit visit(CareVisitEntity v,Context c) {
        List<CareTaskEntity> orders=c.tasks().getOrDefault(v.getId(),List.of()).stream().filter(t -> "ORDER".equals(t.getKind())).toList();
        int published=0,unread=0; boolean critical=false;
        for (CareTaskEntity t:orders) {
            var history=c.results().getOrDefault(t.getId(),List.of());
            CareResultEntity latest=history.stream().filter(r -> r.getPublishedAt()!=null).max(Comparator.comparing(CareResultEntity::getRevision)).orElse(null);
            if (latest!=null) { published++; if (latest.getReadAt()==null) { unread++; critical|=latest.getCritical()==1; } }
        }
        String progress=v.getStatus();
        if (v.getClosedAt()!=null && orders.stream().anyMatch(t -> t.getClosedAt()==null)) progress="FOLLOWUP_PENDING";
        else if ("ACTIVE".equals(v.getStatus())) {
            if (critical) progress="CRITICAL_RESULT";
            else if (unread>0) progress="RESULTS_TO_REVIEW";
            else if (orders.stream().anyMatch(t -> t.getClosedAt()==null)) progress="WAITING_RESULTS";
            else progress="EXAMINING";
        }
        PatientEntity p=c.patients().get(v.getPatientId());
        return new CareResponses.Visit(v.getId(),v.getPatientId(),p.getName(),p.getStatus(),v.getStatus(),progress,
            v.getPointId(),c.point(v.getPointId()),v.getLocationPointId(),c.point(v.getLocationPointId()),v.getOwnerId(),c.user(v.getOwnerId()),
            v.getPriority(),v.getCreatedAt(),v.getClosedAt(),orders.size(),published,unread,critical,v.getAdmissionId(),v.getWardPointId());
    }

    private CareResponses.Task task(CareTaskEntity t,CareVisitEntity v,AppUserEntity actor,Context c) {
        boolean clinical=access.clinical(actor,v), serviceReader=access.assigned(actor,t.getPointId()) && !access.allStaff(actor);
        boolean resultReader=clinical||serviceReader;
        List<CareResultEntity> history=c.results().getOrDefault(t.getId(),List.of());
        List<CareResponses.Result> resultRows=resultReader?history.stream()
            .filter(r -> r.getPublishedAt()!=null || access.perform(actor,t.getPointId()) || access.verify(actor,t.getPointId()))
            .map(r -> new CareResponses.Result(r.getId(),r.getRevision(),r.getBody(),r.getCritical()==1,r.getCorrectionReason(),c.user(r.getCreatedBy()),r.getCreatedAt(),c.user(r.getPublishedBy()),r.getPublishedAt(),c.user(r.getReadBy()),r.getReadAt())).toList():List.of();
        List<String> actions=new ArrayList<>();
        boolean open=t.getClosedAt()==null, order="ORDER".equals(t.getKind()), owner=access.owner(actor,v);
        if (open && access.coordinate(actor,t.getPointId())) actions.add("ARRIVAL");
        if (open && Set.of("ARRIVED","CALLED").contains(t.getArrival()) &&
                (order && "QUEUED".equals(t.getStatus()) && access.perform(actor,t.getPointId())
                 || !order && v.getClosedAt()==null && access.examine(actor,t.getPointId()) && (v.getOwnerId()==null||owner))) actions.add("START");
        if (order && access.perform(actor,t.getPointId()) && Set.of("IN_PROGRESS","AWAITING_VALIDATION","RESULT_READY","COMPLETED").contains(t.getStatus())) actions.add("RESULT");
        if (order && "AWAITING_VALIDATION".equals(t.getStatus()) && access.verify(actor,t.getPointId())) actions.add("PUBLISH");
        CareResultEntity latest=history.stream().max(Comparator.comparing(CareResultEntity::getRevision)).orElse(null);
        if (order && owner && latest!=null && latest.getPublishedAt()!=null && latest.getReadAt()==null) actions.add("ACKNOWLEDGE");
        if (order && open && history.isEmpty() && owner && Set.of("QUEUED","IN_PROGRESS").contains(t.getStatus())) actions.add("CANCEL_ORDER");
        if (order && history.isEmpty() && access.perform(actor,t.getPointId()) && Set.of("QUEUED","IN_PROGRESS","CANCEL_REQUESTED").contains(t.getStatus())) actions.add("DECLINE_ORDER");
        if (order && owner && t.getResolvedAt()==null && Set.of("CANCELLED","DECLINED").contains(t.getStatus())) actions.add("RESOLVE_ORDER");
        PatientEntity p=c.patients().get(v.getPatientId());
        return new CareResponses.Task(t.getId(),v.getId(),p.getId(),p.getName(),p.getStatus(),t.getPointId(),c.point(t.getPointId()),t.getKind(),t.getServiceId(),c.service(t.getServiceId()),
            t.getStatus(),t.getArrival(),resultReader?t.getReason():null,t.getCreatedBy(),c.user(t.getCreatedBy()),t.getCreatedAt(),t.getArrivedAt(),t.getStartedAt(),c.user(t.getPerformedBy()),
            resultReader?t.getResolution():null,t.getResolvedAt(),resultRows,actions);
    }

    // Batch work/results/patients for a page; lookup maps are request-local and never retain medical data.
    private Context context(List<CareVisitEntity> rows) {
        List<CareTaskEntity> work=rows.isEmpty()?List.of():tasks.findByVisitIdIn(rows.stream().map(CareVisitEntity::getId).toList());
        List<CareResultEntity> resultRows=work.isEmpty()?List.of():results.findByTaskIdInOrderByRevisionAsc(work.stream().map(CareTaskEntity::getId).toList());
        return new Context(work.stream().sorted(Comparator.comparing(CareTaskEntity::getCreatedAt).thenComparing(CareTaskEntity::getId)).collect(Collectors.groupingBy(CareTaskEntity::getVisitId)),resultRows.stream().collect(Collectors.groupingBy(CareResultEntity::getTaskId)),
            patients.findAllById(rows.stream().map(CareVisitEntity::getPatientId).toList()).stream().collect(Collectors.toMap(PatientEntity::getId,Function.identity())),
            users.findByPatientIdIsNullOrderByFullNameAsc().stream().collect(Collectors.toMap(AppUserEntity::getId,AppUserEntity::getFullName)),
            points.findAll().stream().collect(Collectors.toMap(CarePointEntity::getId,CarePointEntity::getName)),
            services.findAll().stream().collect(Collectors.toMap(CareServiceEntity::getId,CareServiceEntity::getName)));
    }
    private record Context(Map<String,List<CareTaskEntity>> tasks,Map<Long,List<CareResultEntity>> results,
                           Map<String,PatientEntity> patients,Map<Long,String> users,Map<Long,String> points,Map<Long,String> services) {
        String user(Long id) { return id==null?null:users.getOrDefault(id,"Staff #"+id); }
        String point(Long id) { return id==null?null:points.getOrDefault(id,"Point #"+id); }
        String service(Long id) { return id==null?null:services.getOrDefault(id,"Service #"+id); }
    }
    private static String clean(String text) { return text==null||text.isBlank()?null:text.trim(); }
}
