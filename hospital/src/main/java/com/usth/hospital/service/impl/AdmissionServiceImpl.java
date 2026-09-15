package com.usth.hospital.service.impl;

import com.usth.hospital.dto.request.CreateAdmissionRequest;
import com.usth.hospital.dto.request.UpdateAdmissionRequest;
import com.usth.hospital.dto.response.AdmissionEditDataResponse;
import com.usth.hospital.dto.response.AdmissionOptionsResponse;
import com.usth.hospital.dto.response.AdmissionOptionsResponse.DepartmentOption;
import com.usth.hospital.dto.response.AdmissionOptionsResponse.DoctorOption;
import com.usth.hospital.dto.response.AdmissionOptionsResponse.LookupOption;
import com.usth.hospital.dto.response.AdmissionOptionsResponse.RoomOption;
import com.usth.hospital.dto.response.AdmissionResponse;
import com.usth.hospital.exception.AdmissionConflictException;
import com.usth.hospital.exception.AdmissionNotFoundException;
import com.usth.hospital.exception.InvalidAdmissionOperationException;
import com.usth.hospital.exception.PatientNotFoundException;
import com.usth.hospital.model.AdmissionEntity;
import com.usth.hospital.model.AuditLogAction;
import com.usth.hospital.model.AuditLogEntityType;
import com.usth.hospital.model.AuditLogValueType;
import com.usth.hospital.model.PatientEntity;
import com.usth.hospital.repository.AdmissionRepository;
import com.usth.hospital.repository.AdmissionRepository.DepartmentRow;
import com.usth.hospital.repository.AdmissionRepository.DoctorRow;
import com.usth.hospital.repository.AdmissionRepository.RoomRow;
import com.usth.hospital.repository.PatientRepository;
import com.usth.hospital.repository.CareVisitRepository;
import com.usth.hospital.service.AdmissionService;
import com.usth.hospital.service.AuditEventCommand;
import com.usth.hospital.service.AuditLogChange;
import com.usth.hospital.service.AuditLogService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdmissionServiceImpl implements AdmissionService {

    private static final int ACTIVE_STATUS = 1;

    private final AdmissionRepository admissionRepository;
    private final PatientRepository patientRepository;
    private final AuditLogService auditLogService;
    private final PatientStatusTransitionService patientStatusTransitionService;
    private final CareVisitRepository careVisitRepository;
    private final CareAccessService careAccessService;

    @Override
    public AdmissionOptionsResponse getOptions() {
        return buildOptions(null);
    }

    @Override
    public AdmissionEditDataResponse getEditData(String patientId, String admissionId) {
        AdmissionEntity admission = findAdmission(patientId, admissionId);
        requireActive(admission);
        return new AdmissionEditDataResponse(toResponse(admission), buildOptions(admission));
    }

    @Override
    @Transactional
    public AdmissionResponse create(String patientId, CreateAdmissionRequest request) {
        patientStatusTransitionService.requireLegacyWorkflow(patientId);
        return createInternal(patientId, request);
    }

    @Transactional
    public AdmissionResponse createFromCare(String patientId, CreateAdmissionRequest request) {
        return createInternal(patientId, request);
    }

    private AdmissionResponse createInternal(String patientId, CreateAdmissionRequest request) {
        PatientEntity patient = lockPatient(patientId);
        patientStatusTransitionService.requireExamining(patient);
        if (admissionRepository.existsByPatientIdAndDischargeDateIsNull(patientId)) {
            throw new AdmissionConflictException("Patient already has an active admission");
        }

        DepartmentRow department = admissionRepository.findDepartment(request.departmentId())
                .orElseThrow(() -> invalid("departmentId", "Department is unavailable"));
        DoctorRow doctor = admissionRepository.findDoctor(request.doctorId())
                .orElseThrow(() -> invalid("doctorId", "Doctor is unavailable"));
        if (!Objects.equals(doctor.getStatus(), 1)) {
            throw invalid("doctorId", "Doctor is inactive");
        }
        if (request.type() != null && admissionRepository.countActiveAdmissionType(request.type()) == 0) {
            throw invalid("type", "Admission type is unavailable");
        }

        Long roomId = request.roomId();
        RoomRow room = null;
        if (roomId != null) {
            room = validateAndOccupyRoom(roomId, request.departmentId());
        }

        AdmissionEntity entity = AdmissionEntity.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patientId)
                .departmentId(request.departmentId())
                .doctorId(request.doctorId())
                .roomId(roomId)
                .admissionDate(request.admissionDate())
                .reason(clean(request.reason()))
                .type(request.type())
                .status(ACTIVE_STATUS)
                .notes(clean(request.notes()))
                .build();

        AdmissionEntity saved;
        try {
            saved = admissionRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new AdmissionConflictException(
                    "Admission conflicts with the current patient or reference data", ex);
        }
        patientStatusTransitionService.alignWithActiveAdmissionRoom(patient, roomId != null);
        AdmissionSnapshot after = snapshot(
                saved,
                department.getName(),
                doctor.getName(),
                room == null ? null : room.getName(),
                findAdmissionTypeLabel(saved.getType())
        );
        List<AuditLogChange> changes = admissionChanges(null, after);
        auditLogService.record(admissionCommand(
                AuditLogAction.CREATE,
                saved,
                patient,
                "Record created",
                changes
        ));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AdmissionResponse update(
            String patientId,
            String admissionId,
            UpdateAdmissionRequest request
    ) {
        PatientEntity patient = lockPatient(patientId);
        AdmissionEntity admission = findAdmission(patientId, admissionId);
        requireActive(admission);
        AdmissionSnapshot before = snapshot(admission, null, null, null, null);

        careVisitRepository.findByAdmissionIdAndClosedAtIsNull(admissionId).ifPresent(visit -> {
            careAccessService.requireOwner(careAccessService.actor(), visit);
            if (!Objects.equals(admission.getDepartmentId(), request.departmentId())
                    || !Objects.equals(admission.getDoctorId(), request.doctorId())) {
                throw new AdmissionConflictException("Use the care workspace to hand over the responsible doctor");
            }
        });

        DepartmentRow department = admissionRepository.findDepartment(request.departmentId())
                .orElseThrow(() -> invalid("departmentId", "Department is unavailable"));
        DoctorRow doctor = admissionRepository.findDoctor(request.doctorId())
                .orElseThrow(() -> invalid("doctorId", "Doctor is unavailable"));
        if (!Objects.equals(admission.getDoctorId(), request.doctorId())
                && !Objects.equals(doctor.getStatus(), 1)) {
            throw invalid("doctorId", "Doctor is inactive");
        }
        if (!Objects.equals(admission.getType(), request.type())
                && request.type() != null
                && admissionRepository.countActiveAdmissionType(request.type()) == 0) {
            throw invalid("type", "Admission type is unavailable");
        }

        Long previousRoomId = admission.getRoomId();
        Long requestedRoomId = request.roomId();
        boolean roomChanged = !Objects.equals(previousRoomId, requestedRoomId);
        RoomRow requestedRoom = null;
        if (requestedRoomId != null) {
            requestedRoom = requireRoom(requestedRoomId);
            validateRoomDepartment(requestedRoom, request.departmentId());
        }

        before = enrichSnapshotLabels(before);
        if (requestedRoom != null && roomChanged) {
            occupyRoom(requestedRoom, request.departmentId());
        }

        admission.setDepartmentId(request.departmentId());
        admission.setDoctorId(request.doctorId());
        admission.setRoomId(requestedRoomId);
        admission.setAdmissionDate(request.admissionDate());
        admission.setReason(clean(request.reason()));
        admission.setType(request.type());
        admission.setNotes(clean(request.notes()));

        AdmissionEntity updated;
        try {
            updated = admissionRepository.saveAndFlush(admission);
        } catch (DataIntegrityViolationException ex) {
            throw new AdmissionConflictException(
                    "Admission conflicts with the current patient or reference data", ex);
        }

        if (roomChanged && previousRoomId != null
                && admissionRepository.releaseRoom(previousRoomId) != 1) {
            throw new AdmissionConflictException("Previous room occupancy could not be released");
        }
        patientStatusTransitionService.alignWithActiveAdmissionRoom(patient,
                requestedRoomId != null || Integer.valueOf(1).equals(admission.getInpatientFlag()));
        AdmissionSnapshot after = snapshot(
                updated,
                department.getName(),
                doctor.getName(),
                requestedRoom == null ? null : requestedRoom.getName(),
                findAdmissionTypeLabel(updated.getType())
        );
        List<AuditLogChange> changes = admissionChanges(before, after);
        auditLogService.record(admissionCommand(
                AuditLogAction.UPDATE,
                updated,
                patient,
                changeSummary(changes.size()),
                changes
        ));
        return toResponse(updated);
    }

    @Override
    @Transactional
    public AdmissionResponse discharge(String patientId, String admissionId) {
        PatientEntity patient = lockPatient(patientId);
        patientStatusTransitionService.requireLegacyWorkflow(patientId);
        AdmissionEntity admission = findAdmission(patientId, admissionId);
        requireActive(admission);
        AdmissionSnapshot before = snapshotWithLabels(admission);

        admission.setDischargeDate(LocalDate.now());
        admission.setStatus(2);
        AdmissionEntity discharged;
        try {
            discharged = admissionRepository.saveAndFlush(admission);
        } catch (DataIntegrityViolationException ex) {
            throw new AdmissionConflictException(
                    "Admission conflicts with the current patient or reference data", ex);
        }

        if (admission.getRoomId() != null
                && admissionRepository.releaseRoom(admission.getRoomId()) != 1) {
            throw new AdmissionConflictException("Room occupancy could not be released");
        }
        patientStatusTransitionService.discharge(patient);

        AdmissionSnapshot after = snapshotWithLabels(discharged);
        List<AuditLogChange> changes = admissionChanges(before, after);
        auditLogService.record(admissionCommand(
                AuditLogAction.STATUS_UPDATE,
                discharged,
                patient,
                "In treatment to Discharged",
                changes
        ));
        return toResponse(discharged);
    }

    @Override
    @Transactional
    public void delete(String patientId, String admissionId) {
        PatientEntity patient = lockPatient(patientId);
        if (careVisitRepository.findByAdmissionIdAndClosedAtIsNull(admissionId).isPresent()) {
            throw new AdmissionConflictException("Complete the linked care visit before deleting this admission");
        }
        AdmissionEntity admission = findAdmission(patientId, admissionId);
        AdmissionSnapshot before = snapshotWithLabels(admission);
        boolean wasActive = admission.getDischargeDate() == null;

        admissionRepository.delete(admission);
        admissionRepository.flush();
        if (wasActive && admission.getRoomId() != null) {
            admissionRepository.releaseRoom(admission.getRoomId());
        }
        if (wasActive) {
            patientStatusTransitionService.restoreExaminingAfterActiveAdmissionDeletion(patient);
        }
        List<AuditLogChange> changes = admissionChanges(before, null);
        auditLogService.record(admissionCommand(
                AuditLogAction.DELETE,
                admission,
                patient,
                "Permanently deleted",
                changes
        ));
    }

    private RoomRow validateAndOccupyRoom(Long roomId, Long departmentId) {
        RoomRow room = requireRoom(roomId);
        validateRoomDepartment(room, departmentId);
        occupyRoom(room, departmentId);
        return room;
    }

    private RoomRow requireRoom(Long roomId) {
        return admissionRepository.findRoom(roomId)
                .orElseThrow(() -> invalid("roomId", "Room is unavailable"));
    }

    private void validateRoomDepartment(RoomRow room, Long departmentId) {
        if (!Objects.equals(room.getDepartmentId(), departmentId)) {
            throw invalid("roomId", "Room does not belong to the selected department");
        }
    }

    private void occupyRoom(RoomRow room, Long departmentId) {
        if (!Objects.equals(room.getStatus(), 1)
                || room.getCurrentOccupancy() >= room.getBedCount()
                || admissionRepository.occupyRoom(room.getId(), departmentId) != 1) {
            throw invalid("roomId", "Room has no available bed");
        }
    }

    private AdmissionOptionsResponse buildOptions(AdmissionEntity current) {
        List<DepartmentOption> departments = new ArrayList<>(
                admissionRepository.findDepartmentOptions().stream()
                        .map(row -> new DepartmentOption(row.getId(), row.getCode(), row.getName()))
                        .toList());
        List<DoctorOption> doctors = new ArrayList<>(
                admissionRepository.findActiveDoctorOptions().stream()
                        .map(row -> new DoctorOption(
                                row.getId(), row.getCode(), row.getName(), row.getDepartmentId()))
                        .toList());
        List<RoomOption> rooms = new ArrayList<>(
                admissionRepository.findAvailableRoomOptions().stream()
                        .map(row -> new RoomOption(
                                row.getId(), row.getName(), row.getDepartmentId(),
                                row.getBedCount(), row.getCurrentOccupancy()))
                        .toList());
        List<LookupOption> types = new ArrayList<>(
                admissionRepository.findActiveAdmissionTypes().stream()
                        .map(row -> new LookupOption(row.getValue(), row.getLabel()))
                        .toList());

        if (current != null) {
            appendCurrentDepartment(departments, current.getDepartmentId());
            appendCurrentDoctor(doctors, current.getDoctorId());
            appendCurrentRoom(rooms, current.getRoomId());
            appendCurrentType(types, current.getType());
        }
        return new AdmissionOptionsResponse(departments, doctors, rooms, types);
    }

    private void appendCurrentDepartment(List<DepartmentOption> options, Long departmentId) {
        if (options.stream().noneMatch(option -> Objects.equals(option.id(), departmentId))) {
            admissionRepository.findDepartment(departmentId)
                    .map(row -> new DepartmentOption(row.getId(), row.getCode(), row.getName()))
                    .ifPresent(options::add);
        }
    }

    private void appendCurrentDoctor(List<DoctorOption> options, Long doctorId) {
        if (options.stream().noneMatch(option -> Objects.equals(option.id(), doctorId))) {
            admissionRepository.findDoctor(doctorId)
                    .map(row -> new DoctorOption(
                            row.getId(), row.getCode(), row.getName(), row.getDepartmentId()))
                    .ifPresent(options::add);
        }
    }

    private void appendCurrentRoom(List<RoomOption> options, Long roomId) {
        if (roomId != null && options.stream().noneMatch(option -> Objects.equals(option.id(), roomId))) {
            admissionRepository.findRoom(roomId)
                    .map(row -> new RoomOption(
                            row.getId(), row.getName(), row.getDepartmentId(),
                            row.getBedCount(), row.getCurrentOccupancy()))
                    .ifPresent(options::add);
        }
    }

    private void appendCurrentType(List<LookupOption> options, Integer type) {
        if (type != null && options.stream().noneMatch(option -> Objects.equals(option.value(), type))) {
            LookupOption currentType = admissionRepository.findAdmissionType(type)
                    .map(row -> new LookupOption(row.getValue(), row.getLabel()))
                    .orElseGet(() -> new LookupOption(type, "Admission type " + type));
            options.add(currentType);
        }
    }

    private AdmissionEntity findAdmission(String patientId, String admissionId) {
        return admissionRepository.findByIdAndPatientId(admissionId, patientId)
                .orElseThrow(() -> new AdmissionNotFoundException(admissionId, patientId));
    }

    private static void requireActive(AdmissionEntity admission) {
        if (admission.getDischargeDate() != null) {
            throw invalid("admissionId", "Only active admissions can be edited");
        }
    }

    private PatientEntity lockPatient(String patientId) {
        return patientRepository.findByIdForUpdate(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
    }

    private static InvalidAdmissionOperationException invalid(String field, String message) {
        return new InvalidAdmissionOperationException(field, message);
    }

    private static AdmissionResponse toResponse(AdmissionEntity entity) {
        return new AdmissionResponse(
                entity.getId(), entity.getPatientId(), entity.getDepartmentId(),
                entity.getDoctorId(), entity.getRoomId(), entity.getAdmissionDate(),
                entity.getDischargeDate(), entity.getReason(), entity.getType(),
                entity.getStatus(), entity.getNotes());
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static AuditEventCommand admissionCommand(
            AuditLogAction action,
            AdmissionEntity admission,
            PatientEntity patient,
            String summary,
            List<AuditLogChange> changes
    ) {
        return AuditEventCommand.builder()
                .action(action)
                .entityType(AuditLogEntityType.ADMISSION)
                .entityId(admission.getId())
                .targetLabel(admissionLabel(admission))
                .subjectType(AuditLogEntityType.PATIENT)
                .subjectId(patient.getId())
                .subjectLabel(patientLabel(patient))
                .summary(summary)
                .changes(changes)
                .build();
    }

    private static List<AuditLogChange> admissionChanges(
            AdmissionSnapshot before,
            AdmissionSnapshot after
    ) {
        List<AuditLogChange> changes = new ArrayList<>();
        addReferenceChange(changes, "departmentId", value(before, AdmissionSnapshot::departmentId),
                value(after, AdmissionSnapshot::departmentId),
                value(before, AdmissionSnapshot::departmentDisplay),
                value(after, AdmissionSnapshot::departmentDisplay), 0);
        addReferenceChange(changes, "doctorId", value(before, AdmissionSnapshot::doctorId),
                value(after, AdmissionSnapshot::doctorId),
                value(before, AdmissionSnapshot::doctorDisplay),
                value(after, AdmissionSnapshot::doctorDisplay), 1);
        addReferenceChange(changes, "roomId", value(before, AdmissionSnapshot::roomId),
                value(after, AdmissionSnapshot::roomId),
                value(before, AdmissionSnapshot::roomDisplay),
                value(after, AdmissionSnapshot::roomDisplay), 2);
        addGeneralChange(changes, "admissionDate", AuditLogValueType.DATE,
                value(before, AdmissionSnapshot::admissionDate),
                value(after, AdmissionSnapshot::admissionDate), 3);
        addGeneralChange(changes, "dischargeDate", AuditLogValueType.DATE,
                value(before, AdmissionSnapshot::dischargeDate),
                value(after, AdmissionSnapshot::dischargeDate), 4);
        addMedicalChange(changes, "reason", value(before, AdmissionSnapshot::reason),
                value(after, AdmissionSnapshot::reason), 5);
        addReferenceChange(changes, "type", value(before, AdmissionSnapshot::type),
                value(after, AdmissionSnapshot::type),
                value(before, AdmissionSnapshot::typeDisplay),
                value(after, AdmissionSnapshot::typeDisplay), 6);
        addReferenceChange(changes, "status", value(before, AdmissionSnapshot::status),
                value(after, AdmissionSnapshot::status),
                value(before, AdmissionSnapshot::statusDisplay),
                value(after, AdmissionSnapshot::statusDisplay), 7);
        addMedicalChange(changes, "notes", value(before, AdmissionSnapshot::notes),
                value(after, AdmissionSnapshot::notes), 8);
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

    private static void addReferenceChange(
            List<AuditLogChange> changes,
            String fieldKey,
            Object oldValue,
            Object newValue,
            Object oldDisplayValue,
            Object newDisplayValue,
            int displayOrder
    ) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(AuditLogChange.generalReference(
                    fieldKey,
                    AuditLogValueType.REFERENCE,
                    oldValue,
                    newValue,
                    oldDisplayValue,
                    newDisplayValue,
                    displayOrder
            ));
        }
    }

    private static void addMedicalChange(
            List<AuditLogChange> changes,
            String fieldKey,
            Object oldValue,
            Object newValue,
            int displayOrder
    ) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(AuditLogChange.medical(
                    fieldKey, AuditLogValueType.TEXT, oldValue, newValue, displayOrder));
        }
    }

    private static AdmissionSnapshot snapshot(
            AdmissionEntity admission,
            String departmentDisplay,
            String doctorDisplay,
            String roomDisplay,
            String typeDisplay
    ) {
        return new AdmissionSnapshot(
                admission.getDepartmentId(),
                referenceDisplay(admission.getDepartmentId(), departmentDisplay),
                admission.getDoctorId(),
                referenceDisplay(admission.getDoctorId(), doctorDisplay),
                admission.getRoomId(),
                referenceDisplay(admission.getRoomId(), roomDisplay),
                admission.getAdmissionDate(),
                admission.getDischargeDate(),
                admission.getReason(),
                admission.getType(),
                referenceDisplay(admission.getType(), typeDisplay),
                admission.getStatus(),
                admissionStatusLabel(admission.getStatus()),
                admission.getNotes()
        );
    }

    private AdmissionSnapshot snapshotWithLabels(AdmissionEntity admission) {
        return enrichSnapshotLabels(snapshot(admission, null, null, null, null));
    }

    private AdmissionSnapshot enrichSnapshotLabels(AdmissionSnapshot snapshot) {
        String department = admissionRepository.findDepartment(snapshot.departmentId())
                .map(DepartmentRow::getName)
                .orElse(null);
        String doctor = admissionRepository.findDoctor(snapshot.doctorId())
                .map(DoctorRow::getName)
                .orElse(null);
        String room = snapshot.roomId() == null
                ? null
                : admissionRepository.findRoom(snapshot.roomId())
                        .map(RoomRow::getName)
                        .orElse(null);
        return new AdmissionSnapshot(
                snapshot.departmentId(),
                referenceDisplay(snapshot.departmentId(), department),
                snapshot.doctorId(),
                referenceDisplay(snapshot.doctorId(), doctor),
                snapshot.roomId(),
                referenceDisplay(snapshot.roomId(), room),
                snapshot.admissionDate(),
                snapshot.dischargeDate(),
                snapshot.reason(),
                snapshot.type(),
                referenceDisplay(snapshot.type(), findAdmissionTypeLabel(snapshot.type())),
                snapshot.status(),
                admissionStatusLabel(snapshot.status()),
                snapshot.notes()
        );
    }

    private static String referenceDisplay(Object value, String display) {
        return display == null && value != null ? String.valueOf(value) : display;
    }

    private String findAdmissionTypeLabel(Integer type) {
        if (type == null) {
            return null;
        }
        return admissionRepository.findAdmissionType(type)
                .map(row -> row.getLabel())
                .orElse("Admission type " + type);
    }

    private static String admissionStatusLabel(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 1 -> "In treatment";
            case 2 -> "Discharged";
            case 3 -> "Transferred";
            default -> String.valueOf(status);
        };
    }

    private static String patientLabel(PatientEntity patient) {
        return patient.getName() + " · " + patient.getId();
    }

    private static String admissionLabel(AdmissionEntity admission) {
        String id = admission.getId();
        String shortId = id == null || id.length() <= 8 ? id : id.substring(0, 8);
        return "Admission " + admission.getAdmissionDate() + " · " + shortId;
    }

    private static String changeSummary(int count) {
        if (count == 0) {
            return "No field changes";
        }
        return count + (count == 1 ? " field changed" : " fields changed");
    }

    private static <T> T value(AdmissionSnapshot snapshot, java.util.function.Function<AdmissionSnapshot, T> field) {
        return snapshot == null ? null : field.apply(snapshot);
    }

    private record AdmissionSnapshot(
            Long departmentId,
            String departmentDisplay,
            Long doctorId,
            String doctorDisplay,
            Long roomId,
            String roomDisplay,
            java.time.LocalDate admissionDate,
            java.time.LocalDate dischargeDate,
            String reason,
            Integer type,
            String typeDisplay,
            Integer status,
            String statusDisplay,
            String notes
    ) {
    }
}
