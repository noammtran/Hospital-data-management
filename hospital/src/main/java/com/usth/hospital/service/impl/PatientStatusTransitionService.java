package com.usth.hospital.service.impl;

import com.usth.hospital.dto.response.PatientDetailResponse;
import com.usth.hospital.exception.PatientNotFoundException;
import com.usth.hospital.exception.PatientStatusConflictException;
import com.usth.hospital.mapper.PatientMapper;
import com.usth.hospital.model.AuditLogAction;
import com.usth.hospital.model.AuditLogEntityType;
import com.usth.hospital.model.AuditLogValueType;
import com.usth.hospital.model.PatientEntity;
import com.usth.hospital.model.PatientHistoryEntity;
import com.usth.hospital.model.PatientStatus;
import com.usth.hospital.repository.AdmissionRepository;
import com.usth.hospital.repository.CareVisitRepository;
import com.usth.hospital.repository.PatientHistoryRepository;
import com.usth.hospital.repository.PatientRepository;
import com.usth.hospital.security.CurrentUserProvider;
import com.usth.hospital.service.AuditEventCommand;
import com.usth.hospital.service.AuditLogChange;
import com.usth.hospital.service.AuditLogService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientStatusTransitionService {

    private final PatientRepository patientRepository;
    private final AdmissionRepository admissionRepository;
    private final CareVisitRepository careVisitRepository;
    private final PatientHistoryRepository patientHistoryRepository;
    private final PatientMapper patientMapper;
    private final CurrentUserProvider currentUserProvider;
    private final AuditLogService auditLogService;

    public PatientDetailResponse checkIn(String patientId) {
        PatientEntity patient = lockPatient(patientId);
        requireLegacyWorkflow(patientId);
        transition(patient, PatientStatus.DISCHARGED, PatientStatus.WAITING);
        return patientMapper.toDetail(patient);
    }

    public PatientDetailResponse startExamination(String patientId) {
        PatientEntity patient = lockPatient(patientId);
        requireLegacyWorkflow(patientId);
        transition(patient, PatientStatus.WAITING, PatientStatus.EXAMINING);
        return patientMapper.toDetail(patient);
    }

    public PatientDetailResponse completeExamination(String patientId) {
        PatientEntity patient = lockPatient(patientId);
        requireLegacyWorkflow(patientId);
        if (admissionRepository.existsByPatientIdAndDischargeDateIsNull(patientId)) {
            throw new PatientStatusConflictException(
                    "Complete the active treatment episode from the patient medical record"
            );
        }
        transition(patient, PatientStatus.EXAMINING, PatientStatus.DISCHARGED);
        return patientMapper.toDetail(patient);
    }

    public void requireExamining(PatientEntity patient) {
        requireStatus(patient, PatientStatus.EXAMINING);
    }

    public void beginCareExamination(PatientEntity patient) {
        transition(patient, PatientStatus.WAITING, PatientStatus.EXAMINING);
    }

    public void admitCarePatient(PatientEntity patient) {
        if (PatientStatus.fromCode(patient.getStatus()) == PatientStatus.EXAMINING) {
            transition(patient, PatientStatus.EXAMINING, PatientStatus.ADMITTED);
        } else {
            requireStatus(patient, PatientStatus.ADMITTED);
        }
    }

    public void cancelCareRegistration(PatientEntity patient, int previousStatus) {
        requireStatus(patient, PatientStatus.WAITING);
        if (previousStatus == PatientStatus.DISCHARGED.code()) {
            transition(patient, PatientStatus.WAITING, PatientStatus.DISCHARGED);
        }
    }

    public void requireLegacyWorkflow(String patientId) {
        if (careVisitRepository.existsByPatientIdAndClosedAtIsNull(patientId)) {
            throw new PatientStatusConflictException("Use the active care visit to perform this action");
        }
    }

    public void alignWithActiveAdmissionRoom(PatientEntity patient, boolean hasRoom) {
        PatientStatus current = PatientStatus.fromCode(patient.getStatus());
        if (hasRoom && current == PatientStatus.EXAMINING) {
            transition(patient, PatientStatus.EXAMINING, PatientStatus.ADMITTED);
            return;
        }
        if (!hasRoom && current == PatientStatus.ADMITTED) {
            transition(patient, PatientStatus.ADMITTED, PatientStatus.EXAMINING);
            return;
        }
        if (current != PatientStatus.EXAMINING && current != PatientStatus.ADMITTED) {
            throw new PatientStatusConflictException(
                    "An active treatment episode requires the patient to be Examining or Admitted"
            );
        }
    }

    public void discharge(PatientEntity patient) {
        PatientStatus current = PatientStatus.fromCode(patient.getStatus());
        if (current != PatientStatus.EXAMINING && current != PatientStatus.ADMITTED) {
            throw new PatientStatusConflictException(
                    "Only an Examining or Admitted patient can complete an active treatment episode"
            );
        }
        transition(patient, current, PatientStatus.DISCHARGED);
    }

    public void restoreExaminingAfterActiveAdmissionDeletion(PatientEntity patient) {
        if (PatientStatus.fromCode(patient.getStatus()) == PatientStatus.ADMITTED) {
            transition(patient, PatientStatus.ADMITTED, PatientStatus.EXAMINING);
        }
    }

    private PatientEntity lockPatient(String patientId) {
        return patientRepository.findByIdForUpdate(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
    }

    private static void requireStatus(PatientEntity patient, PatientStatus expected) {
        PatientStatus current = PatientStatus.fromCode(patient.getStatus());
        if (current != expected) {
            throw new PatientStatusConflictException(
                    "Patient must be " + expected.displayName() + " before this action"
            );
        }
    }

    private void transition(PatientEntity patient, PatientStatus expected, PatientStatus target) {
        requireStatus(patient, expected);
        LocalDate now = LocalDate.now();
        String actor = currentUserProvider.username();

        patient.setStatus(target.code());
        patient.setLastUpdatedBy(actor);
        patient.setLastUpdatedDate(now);
        patientRepository.save(patient);
        patientHistoryRepository.save(PatientHistoryEntity.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patient.getId())
                .fieldName("status")
                .oldValue(String.valueOf(expected.code()))
                .newValue(String.valueOf(target.code()))
                .changedBy(actor)
                .changedDate(now)
                .build());

        auditLogService.record(AuditEventCommand.builder()
                .action(AuditLogAction.STATUS_UPDATE)
                .entityType(AuditLogEntityType.PATIENT)
                .entityId(patient.getId())
                .targetLabel(patient.getName() + " · " + patient.getId())
                .summary(expected.displayName() + " to " + target.displayName())
                .changes(List.of(AuditLogChange.generalReference(
                        "status",
                        AuditLogValueType.REFERENCE,
                        expected.code(),
                        target.code(),
                        expected.displayName(),
                        target.displayName(),
                        0
                )))
                .build());
    }
}
