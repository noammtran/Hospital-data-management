package com.usth.hospital.service.impl;

import com.usth.hospital.dto.response.MedicalRecordCareResponse;
import com.usth.hospital.model.*;
import com.usth.hospital.repository.*;
import com.usth.hospital.security.UserRole;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class CareMedicalRecordQueryTest {
    @Mock CareAccessService access;
    @Mock CareVisitRepository visits;
    @Mock CareTaskRepository tasks;
    @Mock CareResultRepository results;
    @Mock CareNoteRepository notes;
    @Mock CareHandoffRepository handoffs;
    @Mock CarePointRepository points;
    @Mock CareServiceRepository services;
    @Mock CareAssignmentRepository assignments;
    @Mock AppUserRepository users;
    @Mock PatientRepository patients;
    @Mock AdmissionRepository admissions;
    @Mock CareReferenceRepository references;
    @Mock DiagnosisRepository diagnoses;
    @Mock PrescriptionRepository prescriptions;
    @InjectMocks CareQueryService query;
    final LocalDateTime time=LocalDateTime.of(2026,9,10,8,0);
    CareVisitEntity visit;
    AppUserEntity doctor;

    @BeforeEach void setup() {
        doctor=AppUserEntity.builder().id(2L).fullName("Doctor Demo").role(UserRole.DOCTOR).build();
        visit=CareVisitEntity.builder().id("v1").patientId("P26AA001").status("COMPLETED")
            .priority(0).pointId(1L).ownerId(2L).createdAt(time).closedAt(time.plusHours(1))
            .conclusion("Final conclusion").plan("Patient instructions").complaint("Complaint").build();
        lenient().when(patients.findAllById(any())).thenReturn(List.of(
            PatientEntity.builder().id("P26AA001").name("Fictional patient").status(3).build()));
        lenient().when(users.findByPatientIdIsNullOrderByFullNameAsc()).thenReturn(List.of(doctor));
    }

    @Test void patientKeepsReviewedPublishedResultWhileCorrectionIsDraft() {
        work(List.of(result(1,true,true),result(2,false,false)));
        var detail=query.patientMedicalRecords(List.of(visit)).getFirst();
        assertThat(detail.conclusion()).isEqualTo("Final conclusion");
        assertThat(detail.tasks().getFirst().results()).extracting("revision").containsExactly(1);
        assertThat(detail.notes()).isEmpty();
        assertThat(detail.handoffs()).isEmpty();
        assertThat(detail.actions()).isEmpty();
    }

    @Test void patientDoesNotReceiveUnreviewedCorrectionOrStaleEarlierResult() {
        work(List.of(result(1,true,true),result(2,true,false)));
        var task=query.patientMedicalRecords(List.of(visit)).getFirst().tasks().getFirst();
        assertThat(task.results()).isEmpty();
        assertThat(task.status()).isEqualTo("AWAITING_REVIEW");
    }

    @Test void patientActiveVisitContainsNoClinicalConclusionOrServices() {
        visit.setStatus("ACTIVE"); visit.setClosedAt(null);
        var detail=query.patientMedicalRecords(List.of(visit)).getFirst();
        assertThat(detail.complaint()).isEqualTo("Complaint");
        assertThat(detail.conclusion()).isNull();
        assertThat(detail.plan()).isNull();
        assertThat(detail.tasks()).isEmpty();
        assertThat(detail.notes()).isEmpty();
    }

    @Test void staffConsolidatedRecordRetainsPublishedRevisionsAndExcludesDraft() {
        work(List.of(result(1,true,true),result(2,true,false),result(3,false,false)));
        when(access.actor()).thenReturn(doctor);
        when(access.clinical(doctor,visit)).thenReturn(true);
        when(access.perform(eq(doctor),anyLong())).thenReturn(true);
        var record=MedicalRecordCareResponse.from(query.medicalRecords(List.of(visit)).getFirst());
        assertThat(record.services().getFirst().results()).extracting("revision").containsExactly(1,2);
    }

    @Test void unrelatedStaffCannotReadClinicalRecord() {
        when(access.actor()).thenReturn(doctor);
        when(access.clinical(doctor,visit)).thenReturn(false);
        assertThat(query.medicalRecords(List.of(visit))).isEmpty();
        verifyNoInteractions(notes, diagnoses, prescriptions, results);
    }

    private void work(List<CareResultEntity> revisions) {
        var task=CareTaskEntity.builder().id(10L).visitId("v1").pointId(1L).kind("ORDER")
            .status("RESULT_READY").arrival("LEFT").serviceId(3L).createdAt(time).build();
        when(tasks.findByVisitIdIn(anyList())).thenReturn(List.of(task));
        when(results.findByTaskIdInOrderByRevisionAsc(anyList())).thenReturn(revisions);
    }
    private CareResultEntity result(int revision,boolean published,boolean reviewed) {
        return CareResultEntity.builder().id((long)revision).taskId(10L).revision(revision)
            .body("Result "+revision).critical(0).createdAt(time).createdBy(2L)
            .publishedAt(published?time:null).publishedBy(published?2L:null)
            .readAt(reviewed?time:null).readBy(reviewed?2L:null).build();
    }
}
