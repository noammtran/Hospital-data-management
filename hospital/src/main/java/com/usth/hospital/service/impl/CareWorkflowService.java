package com.usth.hospital.service.impl;

import com.usth.hospital.dto.request.CareRequests;
import com.usth.hospital.dto.request.CreateAdmissionRequest;
import com.usth.hospital.model.*;
import com.usth.hospital.repository.*;
import com.usth.hospital.security.*;
import com.usth.hospital.service.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.usth.hospital.service.impl.CareAccessService.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CareWorkflowService {
    private final CareAccessService access;
    private final CareVisitRepository visits;
    private final CareTaskRepository tasks;
    private final CareResultRepository results;
    private final CareNoteRepository notes;
    private final CareHandoffRepository handoffs;
    private final CarePointRepository points;
    private final CareServiceRepository services;
    private final PatientRepository patients;
    private final AppUserRepository users;
    private final AdmissionRepository admissions;
    private final DiagnosisRepository diagnoses;
    private final PrescriptionRepository prescriptions;
    private final CareReferenceRepository references;
    private final PatientStatusTransitionService patientStatus;
    private final AdmissionServiceImpl admissionService;
    private final AuditLogService audit;

    public String register(CareRequests.Register request) {
        AppUserEntity actor=access.actor(); access.require(actor,AppPermission.CARE_REGISTER);
        PatientEntity patient=patients.findByIdForUpdate(request.patientId()).orElseThrow(() -> missing("Patient"));
        Optional<CareVisitEntity> existing=visits.findByPatientIdAndClosedAtIsNull(patient.getId());
        if (existing.isPresent()) {
            CareVisitEntity visit=existing.get();
            if ("WAITING".equals(visit.getStatus()) && Objects.equals(visit.getPointId(),request.pointId())
                    && Objects.equals(visit.getComplaint(),request.complaint().trim())) return visit.getId();
            throw conflict("Patient already has an open visit. Open that visit instead");
        }
        if (admissions.existsByPatientIdAndDischargeDateIsNull(patient.getId()))
            throw conflict("Patient has an active treatment episode. Use the inpatient workspace or complete the existing episode");
        if (!Set.of(0,3).contains(patient.getStatus())) throw conflict("Complete the current examination before registering another visit");
        CarePointEntity point=point(request.pointId(),"CLINIC");
        int previous=patient.getStatus();
        if (previous==3) patientStatus.checkIn(patient.getId());
        CareVisitEntity visit=CareVisitEntity.builder().id(UUID.randomUUID().toString()).patientId(patient.getId())
                .status("WAITING").previousPatientStatus(previous).originalPointId(point.getId()).pointId(point.getId())
                .complaint(request.complaint().trim()).priority(request.priority()).createdAt(LocalDateTime.now())
                .createdBy(actor.getId()).build();
        visits.saveAndFlush(visit);
        newExam(visit,actor);
        event(visit,AuditLogEntityType.CARE_VISIT,visit.getId(),AuditLogAction.CREATE,"Registered at "+point.getName());
        return visit.getId();
    }

    public void arrival(Long taskId, CareRequests.Arrival request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockTaskVisit(taskId);
        CareTaskEntity task=task(taskId);
        if (!access.coordinate(actor,task.getPointId())) throw denied();
        if (task.getClosedAt()!=null) throw conflict("This work item is closed");
        String before=task.getArrival();
        if (before.equals(request.state())) return;
        if ("CALLED".equals(request.state()) && !"ARRIVED".equals(before)) throw conflict("Confirm arrival before calling the patient");
        if ("ARRIVED".equals(request.state())) {
            for (CareTaskEntity other: visitTasks(visit)) {
                if (!other.getId().equals(taskId) && Set.of("ARRIVED","CALLED").contains(other.getArrival())) other.setArrival("LEFT");
            }
            visit.setLocationPointId(task.getPointId()); task.setArrivedAt(LocalDateTime.now());
        } else if (Set.of("LEFT","ABSENT").contains(request.state()) && Objects.equals(visit.getLocationPointId(),task.getPointId())) {
            visit.setLocationPointId(null);
        }
        task.setArrival(request.state());
        event(visit,AuditLogEntityType.CARE_TASK,taskId,AuditLogAction.STATUS_UPDATE,before+" to "+request.state());
    }

    public void start(Long taskId) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockTaskVisit(taskId); CareTaskEntity task=task(taskId);
        if (!Set.of("ARRIVED","CALLED").contains(task.getArrival())) throw conflict("Patient arrival must be confirmed first");
        if (task.getClosedAt()!=null) throw conflict("This work item is closed");
        if ("EXAM".equals(task.getKind())) {
            active(visit);
            if (!access.examine(actor,task.getPointId())) throw denied();
            if (visit.getOwnerId()!=null && !Objects.equals(visit.getOwnerId(),actor.getId())) throw conflict("Another doctor owns this visit; request a handoff");
            if ("WAITING".equals(visit.getStatus())) {
                patientStatus.beginCareExamination(patients.findById(visit.getPatientId()).orElseThrow());
                visit.setStatus("ACTIVE"); visit.setStartedAt(LocalDateTime.now()); visit.setOwnerId(actor.getId());
            }
        } else {
            if (!access.perform(actor,task.getPointId())) throw denied();
            if (!"QUEUED".equals(task.getStatus())) throw conflict("The service has already been accepted or completed");
        }
        task.setStatus("IN_PROGRESS"); task.setStartedAt(LocalDateTime.now()); task.setPerformedBy(actor.getId());
        event(visit,AuditLogEntityType.CARE_TASK,taskId,AuditLogAction.STATUS_UPDATE,"Work accepted by assigned staff");
    }

    public void note(String visitId, CareRequests.Note request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockVisit(visitId);
        if (Set.of("EXAM","REVIEW").contains(request.kind())) access.requireOwner(actor,visit);
        else if (!access.coordinate(actor,visit.getPointId()) && !access.coordinate(actor,visit.getWardPointId())) throw denied();
        if (visit.getClosedAt()!=null && !"REVIEW".equals(request.kind())) throw conflict("The visit is closed");
        addNote(visit,actor,request.kind(),request.body().trim());
        event(visit,AuditLogEntityType.CARE_VISIT,visitId,AuditLogAction.UPDATE,"Clinical note recorded");
    }

    public void order(String visitId, CareRequests.Orders request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockVisit(visitId); active(visit); access.requireOwner(actor,visit);
        if (visitTasks(visit).size()+request.items().size()>200) throw invalid("A visit supports up to 200 work items");
        int created=0;
        for (int i=0;i<request.items().size();i++) {
            CareRequests.Order item=request.items().get(i); String key=request.requestId()+"-"+i;
            Optional<CareTaskEntity> duplicate=tasks.findByVisitIdAndRequestKey(visitId,key);
            if (duplicate.isPresent()) {
                CareTaskEntity old=duplicate.get();
                if (!Objects.equals(old.getServiceId(),item.serviceId()) || !Objects.equals(old.getReason(),item.reason().trim()))
                    throw conflict("This request identifier was already used for different orders");
                continue;
            }
            CareServiceEntity service=services.findById(item.serviceId()).filter(s -> s.getActive()==1).orElseThrow(() -> invalid("Service is unavailable"));
            point(service.getPointId(),"SERVICE");
            CareTaskEntity task=CareTaskEntity.builder().visitId(visitId).pointId(service.getPointId()).kind("ORDER")
                .serviceId(service.getId()).status("QUEUED").arrival("EXPECTED").reason(item.reason().trim())
                .createdBy(actor.getId()).createdAt(LocalDateTime.now()).requestKey(key).build();
            tasks.saveAndFlush(task); created++;
            event(visit,AuditLogEntityType.CARE_TASK,task.getId(),AuditLogAction.CREATE,"Requested "+service.getName());
        }
        if (created>0) exam(visit).ifPresent(t -> t.setStatus("WAITING_RESULTS"));
    }

    public void draftResult(Long taskId, CareRequests.Result request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockTaskVisit(taskId); CareTaskEntity task=task(taskId);
        if (!"ORDER".equals(task.getKind()) || !access.perform(actor,task.getPointId())) throw denied();
        if (!Set.of("IN_PROGRESS","AWAITING_VALIDATION","RESULT_READY","COMPLETED").contains(task.getStatus()))
            throw conflict("Accept and start the service before recording a result");
        List<CareResultEntity> history=results.findByTaskIdOrderByRevisionAsc(taskId);
        CareResultEntity draft=history.stream().filter(r -> r.getPublishedAt()==null).findFirst().orElse(null);
        if (history.stream().anyMatch(r -> r.getPublishedAt()!=null) && blank(request.correctionReason()))
            throw invalid("A correction reason is required for a new result revision");
        if (draft==null) draft=CareResultEntity.builder().taskId(taskId).revision(history.size()+1)
                .createdBy(actor.getId()).createdAt(LocalDateTime.now()).build();
        draft.setBody(request.body().trim()); draft.setCritical(request.critical()?1:0); draft.setCorrectionReason(clean(request.correctionReason()));
        results.saveAndFlush(draft); task.setStatus("AWAITING_VALIDATION"); task.setClosedAt(null);
        event(visit,AuditLogEntityType.CARE_RESULT,draft.getId(),AuditLogAction.UPDATE,"Result draft saved; publication pending");
    }

    public void publish(Long taskId) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockTaskVisit(taskId); CareTaskEntity task=task(taskId);
        if (!access.verify(actor,task.getPointId())) throw denied();
        if (!"AWAITING_VALIDATION".equals(task.getStatus())) throw conflict("No result is awaiting validation");
        CareResultEntity draft=results.findByTaskIdOrderByRevisionAsc(taskId).stream().filter(r -> r.getPublishedAt()==null)
                .findFirst().orElseThrow(() -> conflict("No result draft was found"));
        draft.setPublishedAt(LocalDateTime.now()); draft.setPublishedBy(actor.getId()); task.setStatus("RESULT_READY"); task.setClosedAt(null);
        refreshExam(visit);
        event(visit,AuditLogEntityType.CARE_RESULT,draft.getId(),AuditLogAction.STATUS_UPDATE,"Draft to Published, revision "+draft.getRevision());
    }

    public void acknowledgeResult(Long taskId, CareRequests.Acknowledge request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockTaskVisit(taskId); access.requireOwner(actor,visit);
        CareTaskEntity task=task(taskId); CareResultEntity latest=latest(taskId);
        if (latest==null || latest.getPublishedAt()==null) throw conflict("The latest result is not published");
        if (!latest.getId().equals(request.resultId())) throw conflict("The result changed. Read the latest published revision before acknowledging it");
        if (latest.getReadAt()!=null) return;
        latest.setReadAt(LocalDateTime.now()); latest.setReadBy(actor.getId()); task.setStatus("COMPLETED"); task.setClosedAt(LocalDateTime.now());
        event(visit,AuditLogEntityType.CARE_RESULT,latest.getId(),AuditLogAction.STATUS_UPDATE,"Clinician review recorded for revision "+latest.getRevision());
        refreshExam(visit);
    }

    public void cancelOrder(Long taskId, CareRequests.Reason request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockTaskVisit(taskId); access.requireOwner(actor,visit);
        CareTaskEntity task=task(taskId);
        if (!"ORDER".equals(task.getKind()) || task.getClosedAt()!=null) throw conflict("This service cannot be cancelled");
        if (!results.findByTaskIdOrderByRevisionAsc(taskId).isEmpty()) throw conflict("A result already exists; review it or record a correction");
        task.setResolution(request.reason().trim());
        if ("QUEUED".equals(task.getStatus())) {
            task.setStatus("CANCELLED"); task.setClosedAt(LocalDateTime.now()); task.setResolvedBy(actor.getId()); task.setResolvedAt(LocalDateTime.now());
        } else if ("IN_PROGRESS".equals(task.getStatus())) task.setStatus("CANCEL_REQUESTED");
        else throw conflict("Cancellation cannot be requested in the current state");
        event(visit,AuditLogEntityType.CARE_TASK,taskId,AuditLogAction.STATUS_UPDATE,"Service cancellation "+task.getStatus());
        refreshExam(visit);
    }

    public void declineOrder(Long taskId, CareRequests.Reason request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockTaskVisit(taskId); CareTaskEntity task=task(taskId);
        if (!"ORDER".equals(task.getKind()) || !access.perform(actor,task.getPointId())) throw denied();
        if (!Set.of("QUEUED","IN_PROGRESS","CANCEL_REQUESTED").contains(task.getStatus()) || !results.findByTaskIdOrderByRevisionAsc(taskId).isEmpty())
            throw conflict("This work item already has results or is closed");
        task.setStatus("CANCEL_REQUESTED".equals(task.getStatus())?"CANCELLED":"DECLINED");
        task.setResolution(request.reason().trim()); task.setClosedAt(null);
        event(visit,AuditLogEntityType.CARE_TASK,taskId,AuditLogAction.STATUS_UPDATE,"Execution stopped; requesting clinician acknowledgement");
        refreshExam(visit);
    }

    public void resolveOrder(Long taskId, CareRequests.Reason request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockTaskVisit(taskId); access.requireOwner(actor,visit); CareTaskEntity task=task(taskId);
        if (!Set.of("CANCELLED","DECLINED").contains(task.getStatus())) throw conflict("This work item has no unresolved cancellation or refusal");
        if (task.getResolvedAt()!=null) return;
        task.setResolvedAt(LocalDateTime.now()); task.setResolvedBy(actor.getId()); task.setClosedAt(LocalDateTime.now());
        addNote(visit,actor,"REVIEW","Service #"+taskId+": "+request.reason().trim());
        event(visit,AuditLogEntityType.CARE_TASK,taskId,AuditLogAction.UPDATE,"Clinician acknowledged service not performed");
        refreshExam(visit);
    }

    public void reroute(String visitId, CareRequests.Route request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockVisit(visitId); active(visit);
        if (!"WAITING".equals(visit.getStatus())) throw conflict("Use a confirmed doctor handoff after examination starts");
        if (!access.has(actor,AppPermission.CARE_REGISTER) && !access.coordinate(actor,visit.getPointId())) throw denied();
        CarePointEntity destination=point(request.pointId(),"CLINIC");
        exam(visit).ifPresent(t -> { t.setStatus("CANCELLED"); t.setClosedAt(LocalDateTime.now()); t.setResolution(request.reason().trim()); t.setArrival("LEFT"); });
        tasks.flush(); visit.setPointId(destination.getId()); visit.setLocationPointId(null); newExam(visit,actor);
        addNote(visit,actor,"COORDINATION",request.reason().trim());
        event(visit,AuditLogEntityType.CARE_VISIT,visitId,AuditLogAction.UPDATE,"Waiting room changed to "+destination.getName());
    }

    public void cancelVisit(String visitId, CareRequests.Reason request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockVisit(visitId); active(visit);
        if (!access.has(actor,AppPermission.CARE_REGISTER) && !access.coordinate(actor,visit.getPointId())) throw denied();
        if (!"WAITING".equals(visit.getStatus())) throw conflict("An examination has started. The doctor must record the outcome");
        visit.setStatus("CANCELLED"); visit.setClosedAt(LocalDateTime.now()); visit.setOutcome("CANCELLED"); visit.setLocationPointId(null);
        for (CareTaskEntity task:visitTasks(visit)) { task.setStatus("CANCELLED"); task.setClosedAt(LocalDateTime.now()); task.setResolution(request.reason().trim()); }
        patientStatus.cancelCareRegistration(patients.findById(visit.getPatientId()).orElseThrow(),visit.getPreviousPatientStatus());
        addNote(visit,actor,"COORDINATION",request.reason().trim());
        event(visit,AuditLogEntityType.CARE_VISIT,visitId,AuditLogAction.STATUS_UPDATE,"Registration cancelled before examination");
    }

    public void finish(String visitId, CareRequests.Finish request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockVisit(visitId); active(visit); access.requireOwner(actor,visit);
        if (!"ACTIVE".equals(visit.getStatus())) throw conflict("Only an active outpatient visit can be completed here");
        requireNoHandoff(visit); validateCompletion(visit,request.followupPlan(),request.followupAt());
        close(visit,actor,request);
        patientStatus.discharge(patients.findById(visit.getPatientId()).orElseThrow());
    }

    public void requestAdmission(String visitId, CareRequests.AdmissionRequest request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockVisit(visitId); active(visit); access.requireOwner(actor,visit);
        if (!"ACTIVE".equals(visit.getStatus())) throw conflict("An admission request requires an active outpatient examination");
        requireNoHandoff(visit); point(request.wardPointId(),"WARD");
        visit.setConclusion(request.conclusion().trim()); visit.setPlan(request.plan().trim()); visit.setWardPointId(request.wardPointId());
        visit.setStatus("AWAITING_ADMISSION");
        event(visit,AuditLogEntityType.CARE_VISIT,visitId,AuditLogAction.STATUS_UPDATE,"Inpatient reception requested; still Examining");
    }

    public void cancelAdmission(String visitId, CareRequests.Reason request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockVisit(visitId); access.requireOwner(actor,visit);
        if (!"AWAITING_ADMISSION".equals(visit.getStatus())) throw conflict("No admission request is pending");
        visit.setStatus("ACTIVE"); visit.setWardPointId(null); addNote(visit,actor,"REVIEW",request.reason().trim());
        event(visit,AuditLogEntityType.CARE_VISIT,visitId,AuditLogAction.STATUS_UPDATE,"Admission request withdrawn");
    }

    public void acceptAdmission(String visitId, CareRequests.AcceptAdmission request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockVisit(visitId);
        access.require(actor,AppPermission.CARE_ADMIT);
        if (!"AWAITING_ADMISSION".equals(visit.getStatus())) throw conflict("No admission request is pending");
        if (!access.coordinate(actor,visit.getWardPointId())) throw denied();
        CarePointEntity ward=point(visit.getWardPointId(),"WARD"); AppUserEntity doctor=doctor(request.doctorUserId(),ward.getId());
        var admission=admissionService.createFromCare(visit.getPatientId(),new CreateAdmissionRequest(ward.getDepartmentId(),doctor.getDoctorId(),
            request.roomId(),LocalDate.now(),"See linked care visit for clinical indication",null,"Received from outpatient visit "+visitId));
        AdmissionEntity entity=admissions.findById(admission.admissionId()).orElseThrow(); entity.setInpatientFlag(1);
        patientStatus.admitCarePatient(patients.findById(visit.getPatientId()).orElseThrow());
        visit.setAdmissionId(entity.getId()); visit.setStatus("INPATIENT"); visit.setOutcome("ADMITTED");
        visit.setPointId(ward.getId()); visit.setLocationPointId(ward.getId()); visit.setOwnerId(doctor.getId());
        diagnoses.findByVisitIdOrderByDateAsc(visitId).forEach(d -> d.setAdmissionId(entity.getId()));
        prescriptions.findByVisitIdOrderByDateAsc(visitId).forEach(p -> p.setAdmissionId(entity.getId()));
        exam(visit).ifPresent(t -> { t.setStatus("COMPLETED"); t.setClosedAt(LocalDateTime.now()); t.setArrival("LEFT"); });
        addNote(visit,actor,"COORDINATION","Inpatient reception confirmed; remaining service requests handed to the inpatient doctor.");
        event(visit,AuditLogEntityType.CARE_VISIT,visitId,AuditLogAction.STATUS_UPDATE,"Inpatient reception confirmed with responsible doctor");
    }

    public void discharge(String visitId, CareRequests.Finish request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockVisit(visitId); access.requireOwner(actor,visit);
        if (!"INPATIENT".equals(visit.getStatus()) || visit.getAdmissionId()==null) throw conflict("No inpatient episode is attached");
        requireNoHandoff(visit); validateCompletion(visit,request.followupPlan(),request.followupAt());
        close(visit,actor,request);
        admissionService.discharge(visit.getPatientId(),visit.getAdmissionId());
    }

    public String adoptAdmission(String admissionId, CareRequests.AdoptAdmission request) {
        AppUserEntity actor=access.actor(); access.require(actor,AppPermission.CARE_ADMIT);
        // Read only the patient identifier before taking the shared patient lock.
        String patientId=admissions.patientId(admissionId).orElseThrow(() -> missing("Admission"));
        PatientEntity patient=patients.findByIdForUpdate(patientId).orElseThrow(() -> missing("Patient"));
        AdmissionEntity admission=admissions.findById(admissionId).orElseThrow(() -> missing("Admission"));
        if (admission.getDischargeDate()!=null || patient.getStatus()!=2) throw conflict("Only an active inpatient episode can be brought into the care workspace");
        Optional<CareVisitEntity> existing=visits.findByPatientIdAndClosedAtIsNull(patientId);
        if (existing.isPresent()) { access.requireRead(actor,existing.get()); return existing.get().getId(); }
        CarePointEntity ward=point(request.wardPointId(),"WARD");
        if (!Objects.equals(ward.getDepartmentId(),admission.getDepartmentId())) throw invalid("Ward must belong to the existing admission department");
        if (!access.coordinate(actor,ward.getId())) throw denied();
        AppUserEntity doctor=doctor(request.doctorUserId(),ward.getId());
        if (!Objects.equals(doctor.getDoctorId(),admission.getDoctorId())) throw invalid("Select the existing responsible doctor's linked staff account");
        CareVisitEntity visit=CareVisitEntity.builder().id(UUID.randomUUID().toString()).patientId(patientId)
            .status("INPATIENT").previousPatientStatus(2).originalPointId(ward.getId()).pointId(ward.getId())
            .wardPointId(ward.getId()).ownerId(doctor.getId()).admissionId(admissionId).complaint("Continuation of an existing inpatient episode")
            .priority(0).createdAt(LocalDateTime.now()).startedAt(LocalDateTime.now()).createdBy(actor.getId()).outcome("ADMITTED").build();
        visits.saveAndFlush(visit); admission.setInpatientFlag(1);
        addNote(visit,actor,"COORDINATION","Care workspace opened for an existing admission. Earlier care remains in the original medical record; arrival has not been confirmed here.");
        event(visit,AuditLogEntityType.CARE_VISIT,visit.getId(),AuditLogAction.CREATE,"Existing inpatient episode brought into the care workspace");
        return visit.getId();
    }

    public void handoff(String visitId, CareRequests.Handoff request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockVisit(visitId);
        if (!access.owner(actor,visit) && !access.has(actor,AppPermission.CARE_MANAGE)) throw denied();
        if (visit.getOwnerId()==null) throw conflict("Receive the first examination before requesting a doctor handoff");
        if (visit.getClosedAt()!=null && visitTasks(visit).stream().noneMatch(t -> "ORDER".equals(t.getKind()) && t.getClosedAt()==null))
            throw conflict("The closed visit has no remaining follow-up work");
        if ("AWAITING_ADMISSION".equals(visit.getStatus())) throw conflict("Withdraw or complete inpatient reception before changing the doctor");
        CarePointEntity destination=points.findById(request.pointId()).filter(p -> p.getActive()==1).orElseThrow(() -> invalid("Destination is unavailable"));
        if ("SERVICE".equals(destination.getKind())) throw invalid("Choose a clinical or inpatient point");
        if ("INPATIENT".equals(visit.getStatus()) && "TRANSFER".equals(request.kind()) && !Objects.equals(destination.getId(),visit.getWardPointId()))
            throw invalid("Inpatient doctor handoff must stay in the current ward; ward transfer is outside this workflow");
        AppUserEntity recipient=doctor(request.toUserId(),request.pointId());
        if (Objects.equals(recipient.getId(),visit.getOwnerId())) throw invalid("Choose another doctor");
        if ("TRANSFER".equals(request.kind())) requireNoHandoff(visit);
        handoffs.save(CareHandoffEntity.builder().visitId(visitId).fromUserId(visit.getOwnerId()).toUserId(recipient.getId())
            .pointId(request.pointId()).kind(request.kind()).status("PENDING").reason(request.reason().trim())
            .createdBy(actor.getId()).createdAt(LocalDateTime.now()).build());
        event(visit,AuditLogEntityType.CARE_VISIT,visitId,AuditLogAction.UPDATE,"Doctor "+request.kind().toLowerCase()+" requested; acceptance pending");
    }

    public void respond(String visitId, Long handoffId, CareRequests.Respond request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockVisit(visitId);
        CareHandoffEntity handoff=handoffs.findById(handoffId).filter(h -> h.getVisitId().equals(visitId)).orElseThrow(() -> missing("Handoff"));
        if (!Objects.equals(handoff.getToUserId(),actor.getId()) || !access.examine(actor,handoff.getPointId())) throw denied();
        if (!"PENDING".equals(handoff.getStatus())) throw conflict("The request was already answered");
        handoff.setResponse(request.response().trim()); handoff.setRespondedAt(LocalDateTime.now()); handoff.setStatus(request.accept()?"ACCEPTED":"DECLINED");
        if (request.accept() && "TRANSFER".equals(handoff.getKind())) {
            if (!Objects.equals(visit.getOwnerId(),handoff.getFromUserId())) throw conflict("The responsible doctor changed; request a new handoff");
            visit.setOwnerId(actor.getId()); visit.setPointId(handoff.getPointId());
            if (visit.getClosedAt()!=null) visit.setFollowupOwnerId(actor.getId());
            exam(visit).ifPresent(t -> { t.setPointId(handoff.getPointId()); t.setArrival("EXPECTED"); t.setPerformedBy(actor.getId()); });
            if ("INPATIENT".equals(visit.getStatus()) && visit.getAdmissionId()!=null) admissions.findById(visit.getAdmissionId()).ifPresent(a -> a.setDoctorId(actor.getDoctorId()));
        }
        event(visit,AuditLogEntityType.CARE_VISIT,visitId,AuditLogAction.UPDATE,handoff.getKind()+" "+handoff.getStatus().toLowerCase());
    }

    public void cancelHandoff(String visitId, Long handoffId, CareRequests.Reason request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockVisit(visitId);
        if (!access.owner(actor,visit) && !access.has(actor,AppPermission.CARE_MANAGE)) throw denied();
        CareHandoffEntity h=handoffs.findById(handoffId).filter(row -> row.getVisitId().equals(visitId)).orElseThrow(() -> missing("Handoff"));
        if (!"PENDING".equals(h.getStatus())) throw conflict("The request was already answered");
        h.setStatus("DECLINED"); h.setResponse("Withdrawn by requesting team: "+request.reason().trim()); h.setRespondedAt(LocalDateTime.now());
        event(visit,AuditLogEntityType.CARE_VISIT,visitId,AuditLogAction.UPDATE,"Pending doctor request withdrawn");
    }

    public void wardArrival(String visitId) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockVisit(visitId);
        if (!"INPATIENT".equals(visit.getStatus())) throw conflict("The patient is not in an active inpatient visit");
        if (!access.coordinate(actor,visit.getWardPointId())) throw denied();
        visit.setLocationPointId(visit.getWardPointId());
        visitTasks(visit).stream().filter(t -> Set.of("ARRIVED","CALLED").contains(t.getArrival())).forEach(t -> t.setArrival("LEFT"));
        addNote(visit,actor,"COORDINATION","Patient presence confirmed at the inpatient ward");
        event(visit,AuditLogEntityType.CARE_VISIT,visitId,AuditLogAction.UPDATE,"Patient arrival confirmed at the ward");
    }

    public void diagnosis(String visitId, CareRequests.Diagnosis request) {
        AppUserEntity actor=access.actor(); CareVisitEntity visit=access.lockVisit(visitId); active(visit); access.requireOwner(actor,visit);
        // Existing clinical columns use byte semantics, so validate their UTF-8 capacity too.
        byteLimit(request.description(),500,"Diagnosis");
        String code=clean(request.icdCode());
        if (code!=null && references.activeIcd(code)!=1) throw invalid("ICD-10 code is not active");
        DiagnosisEntity diagnosis=DiagnosisEntity.builder().id(UUID.randomUUID().toString()).visitId(visitId).patientId(visit.getPatientId())
                .admissionId(visit.getAdmissionId()).doctorId(actor.getDoctorId()).date(LocalDate.now()).description(request.description().trim()).icdCode(code).build();
        diagnoses.saveAndFlush(diagnosis);
        for (CareRequests.Prescription item: request.prescriptions()==null?List.<CareRequests.Prescription>of():request.prescriptions()) {
            if (references.activeMedicine(item.medicineId())!=1) throw invalid("Medicine is not available for new prescriptions");
            byteLimit(item.dosage(),100,"Dosage"); byteLimit(item.frequency(),100,"Frequency"); byteLimit(item.duration(),50,"Duration"); byteLimit(item.instructions(),500,"Instructions");
            prescriptions.save(PrescriptionEntity.builder().id(UUID.randomUUID().toString()).visitId(visitId).patientId(visit.getPatientId())
                .admissionId(visit.getAdmissionId()).doctorId(actor.getDoctorId()).diagnosisId(diagnosis.getId()).medicineId(item.medicineId())
                .date(LocalDate.now()).dosage(item.dosage().trim()).frequency(item.frequency().trim()).duration(item.duration().trim()).instructions(clean(item.instructions())).build());
        }
        event(visit,AuditLogEntityType.CARE_VISIT,visitId,AuditLogAction.UPDATE,"Clinical diagnosis and treatment instructions recorded");
    }

    private void validateCompletion(CareVisitEntity visit, String followup, LocalDateTime due) {
        boolean outstanding=false;
        for (CareTaskEntity task:visitTasks(visit)) {
            if (!"ORDER".equals(task.getKind())) continue;
            CareResultEntity published=results.findByTaskIdOrderByRevisionAsc(task.getId()).stream()
                .filter(r -> r.getPublishedAt()!=null).max(Comparator.comparing(CareResultEntity::getRevision)).orElse(null);
            if (published!=null && published.getReadAt()==null)
                throw conflict("A published result still needs clinician review");
            if (Set.of("CANCELLED","DECLINED","CANCEL_REQUESTED").contains(task.getStatus()) && task.getResolvedAt()==null)
                throw conflict("Resolve cancelled or declined service requests before completion");
            if (task.getClosedAt()==null) outstanding=true;
        }
        if (outstanding && (blank(followup) || due==null || !due.isAfter(LocalDateTime.now())))
            throw invalid("Open service requests require a follow-up plan and a future review time");
    }
    private void close(CareVisitEntity visit, AppUserEntity actor, CareRequests.Finish request) {
        String outcome="INPATIENT".equals(visit.getStatus())?"DISCHARGED":request.outcome();
        visit.setConclusion(request.conclusion().trim()); visit.setPlan(request.plan().trim()); visit.setOutcome(outcome);
        visit.setFollowupPlan(clean(request.followupPlan())); visit.setFollowupAt(request.followupAt());
        if (!blank(request.followupPlan())) visit.setFollowupOwnerId(actor.getId());
        visit.setStatus("COMPLETED"); visit.setClosedAt(LocalDateTime.now()); visit.setLocationPointId(null);
        exam(visit).ifPresent(t -> { t.setStatus("COMPLETED"); t.setClosedAt(LocalDateTime.now()); t.setArrival("LEFT"); });
        addNote(visit,actor,"CONCLUSION",request.conclusion().trim()+"\n"+request.plan().trim());
        event(visit,AuditLogEntityType.CARE_VISIT,visit.getId(),AuditLogAction.STATUS_UPDATE,"Visit closed with "+outcome.toLowerCase()+" outcome");
    }
    private void refreshExam(CareVisitEntity visit) {
        if (visit.getClosedAt()!=null) return;
        List<CareTaskEntity> orders=visitTasks(visit).stream().filter(t -> "ORDER".equals(t.getKind())).toList();
        boolean ready=!orders.isEmpty() && orders.stream().allMatch(t -> Set.of("RESULT_READY","COMPLETED","CANCELLED","DECLINED").contains(t.getStatus()));
        if (ready) exam(visit).ifPresent(t -> t.setStatus("RESULT_READY"));
    }
    private void requireNoHandoff(CareVisitEntity visit) {
        if (handoffs.findByVisitIdOrderByCreatedAtAsc(visit.getId()).stream().anyMatch(h -> "PENDING".equals(h.getStatus()))) throw conflict("Resolve pending doctor handoffs and consultations first");
    }
    private void newExam(CareVisitEntity visit,AppUserEntity actor) {
        tasks.saveAndFlush(CareTaskEntity.builder().visitId(visit.getId()).pointId(visit.getPointId()).kind("EXAM")
                .status("QUEUED").arrival("EXPECTED").createdBy(actor.getId()).createdAt(LocalDateTime.now()).build());
    }
    private Optional<CareTaskEntity> exam(CareVisitEntity visit) {
        return visitTasks(visit).stream().filter(t -> "EXAM".equals(t.getKind()) && t.getClosedAt()==null).findFirst();
    }
    private List<CareTaskEntity> visitTasks(CareVisitEntity visit) { return tasks.findByVisitIdOrderByCreatedAtAscIdAsc(visit.getId()); }
    private CareTaskEntity task(Long id) { return tasks.findById(id).orElseThrow(() -> missing("Work item")); }
    private CareResultEntity latest(Long taskId) { return results.findByTaskIdOrderByRevisionAsc(taskId).stream().max(Comparator.comparing(CareResultEntity::getRevision)).orElse(null); }
    private CarePointEntity point(Long id,String kind) { return points.findById(id).filter(p -> p.getActive()==1 && kind.equals(p.getKind())).orElseThrow(() -> invalid("Service point is unavailable or has the wrong type")); }
    private AppUserEntity doctor(Long id,Long pointId) {
        return users.findById(id).filter(u -> u.isActive() && access.examine(u,pointId)).orElseThrow(() -> invalid("Doctor is not assigned to the destination"));
    }
    private void addNote(CareVisitEntity visit,AppUserEntity actor,String kind,String body) {
        notes.save(CareNoteEntity.builder().visitId(visit.getId()).kind(kind).body(body).createdBy(actor.getId()).createdAt(LocalDateTime.now()).build());
    }
    private void event(CareVisitEntity visit,AuditLogEntityType type,Object id,AuditLogAction action,String summary) {
        audit.record(AuditEventCommand.builder().entityType(type).entityId(String.valueOf(id)).action(action)
                .targetLabel(type==AuditLogEntityType.CARE_VISIT?"Clinical visit":"Clinical work item")
                .subjectType(AuditLogEntityType.PATIENT).subjectId(visit.getPatientId()).subjectLabel(visit.getPatientId()).summary(summary).build());
    }
    private static String clean(String text) { return blank(text)?null:text.trim(); }
    private static boolean blank(String text) { return text==null || text.isBlank(); }
    private static void byteLimit(String text,int max,String label) {
        if (text!=null && text.getBytes(StandardCharsets.UTF_8).length>max) throw invalid(label+" exceeds the existing database text capacity");
    }
}
