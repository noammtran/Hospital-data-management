package com.usth.hospital.service;

import com.usth.hospital.dto.request.CareRequests;
import com.usth.hospital.exception.CareWorkflowException;
import com.usth.hospital.model.*;
import com.usth.hospital.repository.*;
import com.usth.hospital.security.UserRole;
import com.usth.hospital.service.impl.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareWorkflowServiceTest {
    @Mock CareAccessService access;
    @Mock CareVisitRepository visits;
    @Mock CareTaskRepository tasks;
    @Mock CareResultRepository results;
    @Mock CareNoteRepository notes;
    @Mock CareHandoffRepository handoffs;
    @Mock CarePointRepository points;
    @Mock CareServiceRepository services;
    @Mock PatientRepository patients;
    @Mock AppUserRepository users;
    @Mock AdmissionRepository admissions;
    @Mock DiagnosisRepository diagnoses;
    @Mock PrescriptionRepository prescriptions;
    @Mock CareReferenceRepository references;
    @Mock PatientStatusTransitionService patientStatus;
    @Mock AdmissionServiceImpl admissionService;
    @Mock AuditLogService audit;
    @InjectMocks CareWorkflowService service;

    CareVisitEntity visit;
    AppUserEntity doctor;
    PatientEntity patient;
    final List<CareTaskEntity> work=new ArrayList<>();
    final List<CareResultEntity> resultRows=new ArrayList<>();

    @BeforeEach void setup() {
        doctor=AppUserEntity.builder().id(2L).role(UserRole.DOCTOR).doctorId(3L).activeFlag(1).build();
        patient=PatientEntity.builder().id("P26TEST001").name("Fictional care patient").status(1).build();
        visit=CareVisitEntity.builder().id("visit-1").patientId(patient.getId()).status("ACTIVE").pointId(1L).ownerId(2L).priority(0).createdAt(LocalDateTime.now()).build();
        lenient().when(access.actor()).thenReturn(doctor);
        lenient().when(access.lockVisit(anyString())).thenAnswer(i -> visit);
        lenient().when(access.lockTaskVisit(anyLong())).thenAnswer(i -> visit);
        lenient().when(access.owner(any(),any())).thenReturn(true);
        lenient().when(access.perform(any(),anyLong())).thenReturn(true);
        lenient().when(access.verify(any(),anyLong())).thenReturn(true);
        lenient().when(access.coordinate(any(),anyLong())).thenReturn(true);
        lenient().when(access.examine(any(),anyLong())).thenReturn(true);
        lenient().when(patients.findById(patient.getId())).thenReturn(Optional.of(patient));
        lenient().when(patients.findByIdForUpdate(patient.getId())).thenReturn(Optional.of(patient));
        lenient().when(tasks.findByVisitIdOrderByCreatedAtAscIdAsc(anyString())).thenAnswer(i -> List.copyOf(work));
        lenient().when(tasks.findById(anyLong())).thenAnswer(i -> work.stream().filter(t -> t.getId().equals(i.getArgument(0))).findFirst());
        lenient().when(tasks.findByVisitIdAndRequestKey(anyString(),anyString())).thenAnswer(i -> work.stream().filter(t -> Objects.equals(t.getRequestKey(),i.getArgument(1))).findFirst());
        AtomicLong ids=new AtomicLong(100);
        lenient().when(tasks.saveAndFlush(any())).thenAnswer(i -> { CareTaskEntity t=i.getArgument(0); t.setId(ids.incrementAndGet()); work.add(t); return t; });
        lenient().when(results.findByTaskIdOrderByRevisionAsc(anyLong())).thenAnswer(i -> resultRows.stream().filter(r -> r.getTaskId().equals(i.getArgument(0))).sorted(Comparator.comparing(CareResultEntity::getRevision)).toList());
        lenient().when(results.saveAndFlush(any())).thenAnswer(i -> { CareResultEntity r=i.getArgument(0); if (r.getId()==null) { r.setId(ids.incrementAndGet()); resultRows.add(r); } return r; });
        lenient().when(visits.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }
    private CareTaskEntity task(String kind,String status) {
        CareTaskEntity t=CareTaskEntity.builder().id((long)work.size()+1).visitId(visit.getId()).pointId("ORDER".equals(kind)?10L:1L).kind(kind).status(status).arrival("EXPECTED").createdAt(LocalDateTime.now()).build();
        work.add(t); return t;
    }
    private CareResultEntity published(CareTaskEntity task,int revision,boolean read) {
        CareResultEntity r=CareResultEntity.builder().id(500L+revision).taskId(task.getId()).revision(revision).body("Fictional result revision "+revision).critical(1).createdAt(LocalDateTime.now()).publishedAt(LocalDateTime.now()).publishedBy(8L).build();
        if (read) { r.setReadAt(LocalDateTime.now()); r.setReadBy(2L); }
        resultRows.add(r); return r;
    }
    private CareRequests.Finish finish(String plan,LocalDateTime due) { return new CareRequests.Finish("Clinical conclusion","Treatment instructions","OUTPATIENT",plan,due); }

    @Test void start_requiresActualArrival_beforeChangingPatientStatus() {
        visit.setStatus("WAITING"); visit.setOwnerId(null); CareTaskEntity t=task("EXAM","QUEUED");
        assertThatThrownBy(() -> service.start(t.getId())).isInstanceOf(CareWorkflowException.class);
        verifyNoInteractions(patientStatus);
        service.arrival(t.getId(),new CareRequests.Arrival("ARRIVED")); service.start(t.getId());
        assertThat(visit.getOwnerId()).isEqualTo(doctor.getId()); assertThat(visit.getStatus()).isEqualTo("ACTIVE");
        verify(patientStatus).beginCareExamination(patient);
    }

    @Test void arrival_changesOnlyConfirmedLocation_andLeavesPatientAdmitted() {
        patient.setStatus(2); visit.setStatus("INPATIENT");
        CareTaskEntity old=task("ORDER","IN_PROGRESS"); old.setArrival("ARRIVED"); old.setPointId(11L);
        CareTaskEntity next=task("ORDER","QUEUED");
        service.arrival(next.getId(),new CareRequests.Arrival("ARRIVED"));
        assertThat(old.getArrival()).isEqualTo("LEFT"); assertThat(visit.getLocationPointId()).isEqualTo(10L);
        assertThat(patient.getStatus()).isEqualTo(2); verifyNoInteractions(patientStatus);
    }

    @Test void repeatedOrders_areSeparateRounds_andRequestRetryDoesNotDuplicate() {
        CareTaskEntity exam=task("EXAM","IN_PROGRESS");
        when(services.findById(7L)).thenReturn(Optional.of(CareServiceEntity.builder().id(7L).active(1).pointId(10L).name("Fictional lab service").build()));
        when(points.findById(10L)).thenReturn(Optional.of(CarePointEntity.builder().id(10L).kind("SERVICE").active(1).build()));
        var request=new CareRequests.Orders("round-a",List.of(new CareRequests.Order(7L,"Clinical indication")));
        service.order(visit.getId(),request); service.order(visit.getId(),request);
        assertThat(work).hasSize(2);
        service.order(visit.getId(),new CareRequests.Orders("round-b",request.items()));
        assertThat(work).hasSize(3); assertThat(exam.getStatus()).isEqualTo("WAITING_RESULTS"); assertThat(visit.getOwnerId()).isEqualTo(2L);
    }

    @Test void drafts_doNotCountAsReturned_andPublicationNeedsPermission() {
        CareTaskEntity t=task("ORDER","IN_PROGRESS");
        service.draftResult(t.getId(),new CareRequests.Result("Fictional result",false,null));
        assertThat(resultRows.getFirst().getPublishedAt()).isNull(); assertThat(t.getStatus()).isEqualTo("AWAITING_VALIDATION");
        when(access.verify(doctor,10L)).thenReturn(false);
        assertThatThrownBy(() -> service.publish(t.getId())).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(resultRows.getFirst().getPublishedAt()).isNull();
    }

    @Test void correction_keepsPublishedBody_andRejectsAcknowledgementOfAnOlderRevision() {
        CareTaskEntity t=task("ORDER","RESULT_READY"); CareResultEntity original=published(t,1,false);
        assertThatThrownBy(() -> service.draftResult(t.getId(),new CareRequests.Result("Corrected",true,null))).isInstanceOf(CareWorkflowException.class);
        service.draftResult(t.getId(),new CareRequests.Result("Corrected",true,"Repeat verification")); service.publish(t.getId());
        assertThat(original.getBody()).isEqualTo("Fictional result revision 1"); assertThat(resultRows).hasSize(2);
        assertThatThrownBy(() -> service.acknowledgeResult(t.getId(),new CareRequests.Acknowledge(original.getId()))).isInstanceOf(CareWorkflowException.class);
        assertThat(resultRows.getLast().getReadAt()).isNull();
        service.acknowledgeResult(t.getId(),new CareRequests.Acknowledge(resultRows.getLast().getId()));
        assertThat(t.getStatus()).isEqualTo("COMPLETED");
        service.finish(visit.getId(),finish(null,null)); assertThat(visit.getClosedAt()).isNotNull();
    }

    @Test void completion_blocksUnreadResults_evenWhenANewerDraftExists() {
        CareTaskEntity t=task("ORDER","RESULT_READY"); published(t,1,false);
        service.draftResult(t.getId(),new CareRequests.Result("Correction draft",false,"Correction pending"));
        assertThatThrownBy(() -> service.finish(visit.getId(),finish("Call patient",LocalDateTime.now().plusDays(1)))).isInstanceOf(CareWorkflowException.class).hasMessageContaining("review");
        assertThat(visit.getClosedAt()).isNull(); verifyNoInteractions(patientStatus);
    }

    @Test void delayedResult_requiresFutureFollowup_andRetainsDoctorAfterClosure() {
        CareTaskEntity t=task("ORDER","IN_PROGRESS");
        assertThatThrownBy(() -> service.finish(visit.getId(),finish(null,null))).isInstanceOf(CareWorkflowException.class);
        service.finish(visit.getId(),finish("Review delayed test and contact patient",LocalDateTime.now().plusDays(1)));
        assertThat(visit.getFollowupOwnerId()).isEqualTo(2L); assertThat(t.getClosedAt()).isNull();
        service.draftResult(t.getId(),new CareRequests.Result("Late result",false,null)); service.publish(t.getId());
        assertThat(t.getStatus()).isEqualTo("RESULT_READY"); assertThat(visit.getStatus()).isEqualTo("COMPLETED");
        verify(patientStatus,times(1)).discharge(patient);
    }

    @Test void cancellation_inProgressRequiresPerformerAndDoctorResolution() {
        CareTaskEntity t=task("ORDER","IN_PROGRESS");
        service.cancelOrder(t.getId(),new CareRequests.Reason("No longer indicated"));
        assertThat(t.getStatus()).isEqualTo("CANCEL_REQUESTED");
        service.declineOrder(t.getId(),new CareRequests.Reason("Execution stopped before result"));
        assertThatThrownBy(() -> service.finish(visit.getId(),finish(null,null))).isInstanceOf(CareWorkflowException.class);
        service.resolveOrder(t.getId(),new CareRequests.Reason("Alternative assessment documented"));
        service.finish(visit.getId(),finish(null,null)); assertThat(visit.getStatus()).isEqualTo("COMPLETED");
    }

    @Test void handoff_doesNotChangeDoctorUntilRecipientAccepts() {
        CareHandoffEntity h=CareHandoffEntity.builder().id(9L).visitId(visit.getId()).kind("TRANSFER").status("PENDING").fromUserId(2L).toUserId(4L).pointId(12L).build();
        when(handoffs.findById(9L)).thenReturn(Optional.of(h));
        assertThat(visit.getOwnerId()).isEqualTo(2L);
        AppUserEntity recipient=AppUserEntity.builder().id(4L).doctorId(5L).role(UserRole.DOCTOR).activeFlag(1).build();
        when(access.actor()).thenReturn(recipient);
        service.respond(visit.getId(),9L,new CareRequests.Respond(true,"I have received the clinical handoff"));
        assertThat(visit.getOwnerId()).isEqualTo(4L); assertThat(visit.getPointId()).isEqualTo(12L);
    }

    @Test void consultation_returnsOpinion_withoutChangingResponsibility() {
        CareHandoffEntity h=CareHandoffEntity.builder().id(9L).visitId(visit.getId()).kind("CONSULT").status("PENDING").fromUserId(4L).toUserId(2L).pointId(10L).build();
        visit.setOwnerId(4L); when(handoffs.findById(9L)).thenReturn(Optional.of(h));
        service.respond(visit.getId(),9L,new CareRequests.Respond(true,"Clinical opinion recorded"));
        assertThat(visit.getOwnerId()).isEqualTo(4L); assertThat(h.getResponse()).isEqualTo("Clinical opinion recorded");
    }

    @Test void admissionRequest_keepsExaminingUntilWardReception() {
        when(points.findById(20L)).thenReturn(Optional.of(CarePointEntity.builder().id(20L).kind("WARD").active(1).build()));
        service.requestAdmission(visit.getId(),new CareRequests.AdmissionRequest(20L,"Needs inpatient observation","Ward care plan"));
        assertThat(visit.getStatus()).isEqualTo("AWAITING_ADMISSION"); assertThat(patient.getStatus()).isEqualTo(1);
        verifyNoInteractions(admissionService,patientStatus);
    }

    @Test void inpatientDischarge_recordsTheSameOutcomeInVisitAndAudit() {
        visit.setStatus("INPATIENT"); visit.setAdmissionId("admission-1");
        service.discharge(visit.getId(),finish(null,null));
        assertThat(visit.getOutcome()).isEqualTo("DISCHARGED");
        verify(admissionService).discharge(patient.getId(),"admission-1");
        verify(audit).record(argThat(event -> event.summary().equals("Visit closed with discharged outcome")));
    }

    @Test void register_rejectsAnActiveAdmission_withoutCreatingAnotherEpisode() {
        patient.setStatus(2); when(admissions.existsByPatientIdAndDischargeDateIsNull(patient.getId())).thenReturn(true);
        assertThatThrownBy(() -> service.register(new CareRequests.Register(patient.getId(),1L,"New complaint",0))).isInstanceOf(CareWorkflowException.class);
        verify(visits,never()).saveAndFlush(any()); verify(tasks,never()).saveAndFlush(any());
    }
}
