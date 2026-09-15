package com.usth.hospital.service.impl;

import com.usth.hospital.dto.request.CreatePatientRequest;
import com.usth.hospital.dto.request.UpdatePatientRequest;
import com.usth.hospital.dto.response.PatientDetailResponse;
import com.usth.hospital.dto.response.PatientHistoryEntryResponse;
import com.usth.hospital.dto.response.PatientHistoryResponse;
import com.usth.hospital.dto.response.PatientListResponse;
import com.usth.hospital.exception.DuplicateFieldException;
import com.usth.hospital.exception.PatientNotFoundException;
import com.usth.hospital.mapper.PatientMapper;
import com.usth.hospital.model.AuditLogAction;
import com.usth.hospital.model.AuditLogEntityType;
import com.usth.hospital.model.AuditLogValueType;
import com.usth.hospital.model.PatientEntity;
import com.usth.hospital.model.PatientHistoryEntity;
import com.usth.hospital.repository.AdmissionRepository;
import com.usth.hospital.repository.PatientHistoryRepository;
import com.usth.hospital.repository.PatientRepository;
import com.usth.hospital.security.CurrentUserProvider;
import com.usth.hospital.service.AuditLogService;
import com.usth.hospital.service.AuditEventCommand;
import com.usth.hospital.service.AuditLogChange;
import com.usth.hospital.service.AuditLogTarget;
import com.usth.hospital.service.PatientIdGenerator;
import com.usth.hospital.service.PatientService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientServiceImpl implements PatientService {

    private static final int VALUE_MAX_LENGTH = 255;
    private static final Set<String> PERSONAL_AUDIT_FIELDS = Set.of(
            "birthDate", "gender", "address", "phoneNo", "email", "identityNo", "insuranceNo",
            "insuranceExpiry",
            "emergencyContactName", "emergencyContactPhone", "emergencyContactRelation"
    );
    private static final Set<String> MEDICAL_AUDIT_FIELDS = Set.of(
            "bloodType", "allergies", "medicalHistory", "note"
    );

    private final PatientRepository patientRepository;
    private final AdmissionRepository admissionRepository;
    private final PatientHistoryRepository patientHistoryRepository;
    private final PatientMapper patientMapper;
    private final PatientIdGenerator patientIdGenerator;
    private final CurrentUserProvider currentUserProvider;
    private final AuditLogService auditLogService;
    private final PatientStatusTransitionService patientStatusTransitionService;
    private final PatientAccountLifecycleService patientAccountLifecycleService;

    @Override
    @Transactional(readOnly = true)
    public Page<PatientListResponse> list(String search, String searchField, Integer status, Integer gender,
                                          Pageable pageable) {
        String keyword = (search == null || search.isBlank()) ? null : search.trim();
        String field = (searchField == null || searchField.isBlank()) ? "name" : searchField;
        Page<PatientEntity> page = patientRepository.search(keyword, field, status, gender, pageable);
        Set<String> withHistory = page.isEmpty()
                ? Set.of()
                : new HashSet<>(patientHistoryRepository.findDistinctPatientIdsByPatientIdIn(
                        page.getContent().stream().map(PatientEntity::getId).toList()));
        return page.map(e -> patientMapper.toListItem(e, withHistory.contains(e.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public PatientDetailResponse getDetail(String id) {
        PatientEntity entity = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));
        return patientMapper.toDetail(entity);
    }

    @Override
    public PatientDetailResponse create(CreatePatientRequest request) {
        requireUniqueIdentity(request.identityNo(), null);
        PatientEntity entity = patientMapper.toEntity(request);
        entity.setIdentityNo(request.identityNo().trim());
        LocalDate now = LocalDate.now();
        String actor = currentUserProvider.username();
        entity.setId(nextPatientId(now.getYear()));
        entity.setCreatedBy(actor);
        entity.setCreatedDate(now);
        entity.setLastUpdatedBy(actor);
        entity.setLastUpdatedDate(now);
        PatientEntity saved = patientRepository.save(entity);
        patientAccountLifecycleService.createFor(saved);
        List<AuditLogChange> changes = patientChanges(Map.of(), snapshot(saved));
        auditLogService.record(AuditEventCommand.builder()
                .action(AuditLogAction.CREATE)
                .entityType(AuditLogEntityType.PATIENT)
                .entityId(saved.getId())
                .targetLabel(patientLabel(saved))
                .summary("Record created")
                .changes(changes)
                .build());
        return patientMapper.toDetail(saved);
    }

    @Override
    public PatientDetailResponse update(String id, UpdatePatientRequest request) {
        PatientEntity entity = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));
        requireUniqueIdentity(request.identityNo(), id);
        Map<String, String> before = snapshot(entity);
        patientMapper.updateEntity(request, entity);
        entity.setIdentityNo(normalizeIdentity(request.identityNo()));
        LocalDate now = LocalDate.now();
        String actor = currentUserProvider.username();
        entity.setLastUpdatedBy(actor);
        entity.setLastUpdatedDate(now);
        PatientEntity saved = patientRepository.save(entity);
        patientAccountLifecycleService.synchronize(saved);
        recordHistory(id, before, snapshot(saved), actor, now);
        Map<String, String> after = snapshot(saved);
        List<AuditLogChange> changes = patientChanges(before, after);
        auditLogService.record(AuditEventCommand.builder()
                .action(AuditLogAction.UPDATE)
                .entityType(AuditLogEntityType.PATIENT)
                .entityId(saved.getId())
                .targetLabel(patientLabel(saved))
                .summary(changeSummary(changes.size()))
                .changes(changes)
                .build());
        return patientMapper.toDetail(saved);
    }

    @Override
    public PatientDetailResponse checkIn(String id) {
        return patientStatusTransitionService.checkIn(id);
    }

    @Override
    public PatientDetailResponse startExamination(String id) {
        return patientStatusTransitionService.startExamination(id);
    }

    @Override
    public PatientDetailResponse completeExamination(String id) {
        return patientStatusTransitionService.completeExamination(id);
    }

    @Override
    public void delete(String id) {
        PatientEntity patient = patientRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new PatientNotFoundException(id));
        Map<String, String> before = snapshot(patient);
        releaseActiveAdmissionRooms(List.of(id));
        patientAccountLifecycleService.deleteFor(id);
        patientRepository.deleteById(id);
        List<AuditLogChange> changes = patientChanges(before, Map.of());
        auditLogService.record(AuditEventCommand.builder()
                .action(AuditLogAction.DELETE)
                .entityType(AuditLogEntityType.PATIENT)
                .entityId(id)
                .targetLabel(patientLabel(patient))
                .summary("Permanently deleted")
                .changes(changes)
                .build());
    }

    @Override
    public void bulkDelete(List<String> ids) {
        List<String> orderedIds = ids.stream().distinct().sorted().toList();
        List<PatientEntity> patients = orderedIds.stream()
                .map(patientRepository::findByIdForUpdate)
                .flatMap(java.util.Optional::stream)
                .toList();
        releaseActiveAdmissionRooms(orderedIds);
        patientAccountLifecycleService.deleteFor(orderedIds);
        patientRepository.deleteAllById(orderedIds);
        List<AuditLogTarget> targets = new ArrayList<>();
        for (int index = 0; index < patients.size(); index++) {
            PatientEntity patient = patients.get(index);
            targets.add(new AuditLogTarget(
                    AuditLogEntityType.PATIENT,
                    patient.getId(),
                    patientLabel(patient),
                    index
            ));
        }
        auditLogService.record(AuditEventCommand.builder()
                .action(AuditLogAction.BULK_DELETE)
                .entityType(AuditLogEntityType.PATIENT)
                .targetLabel(patients.size() + " patients")
                .summary(patients.size() + " records deleted")
                .affectedTargets(targets)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientHistoryResponse> getHistory(String id, Pageable pageable) {
        if (!patientRepository.existsById(id)) {
            throw new PatientNotFoundException(id);
        }
        return patientHistoryRepository.findByPatientIdOrderByChangedDateDesc(id, pageable)
                .map(patientMapper::toHistory);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientHistoryEntryResponse> getAllHistory(Pageable pageable) {
        Page<PatientHistoryEntity> page = patientHistoryRepository.findAllByOrderByChangedDateDesc(pageable);
        Set<String> patientIds = page.getContent().stream()
                .map(PatientHistoryEntity::getPatientId)
                .collect(Collectors.toSet());
        Map<String, String> names = patientRepository.findAllById(patientIds).stream()
                .collect(Collectors.toMap(PatientEntity::getId, PatientEntity::getName));
        return page.map(e -> patientMapper.toHistoryEntry(e, names.get(e.getPatientId())));
    }

    private String nextPatientId(int year) {
        String prefix = "P" + String.format("%02d", year % 100);
        int next = patientRepository.findIdsByPrefix(prefix).stream()
                .mapToInt(patientIdGenerator::extractSequence)
                .max()
                .orElse(0) + 1;
        return patientIdGenerator.generate(year, next);
    }

    private void releaseActiveAdmissionRooms(List<String> patientIds) {
        admissionRepository.findActiveRoomIdsByPatientIds(patientIds)
                .forEach(admissionRepository::releaseRoom);
    }

    private void requireUniqueIdentity(String identityNo, String currentId) {
        if (identityNo == null || identityNo.isBlank()) {
            return;
        }
        boolean taken = currentId == null
                ? patientRepository.existsByIdentityNo(identityNo)
                : patientRepository.existsByIdentityNoAndIdNot(identityNo, currentId);
        if (taken) {
            throw new DuplicateFieldException("identityNo", "Identity card number already exists");
        }
    }

    private static String normalizeIdentity(String identityNo) {
        return identityNo == null || identityNo.isBlank() ? null : identityNo.trim();
    }

    private void recordHistory(String patientId, Map<String, String> before,
                               Map<String, String> after, String changedBy, LocalDate changedDate) {
        List<PatientHistoryEntity> rows = new ArrayList<>();
        for (Map.Entry<String, String> field : before.entrySet()) {
            String oldValue = field.getValue();
            String newValue = after.get(field.getKey());
            if (!Objects.equals(oldValue, newValue)) {
                rows.add(PatientHistoryEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .patientId(patientId)
                        .fieldName(field.getKey())
                        .oldValue(oldValue)
                        .newValue(newValue)
                        .changedBy(changedBy)
                        .changedDate(changedDate)
                        .build());
            }
        }
        if (!rows.isEmpty()) {
            patientHistoryRepository.saveAll(rows);
        }
    }

    private static Map<String, String> snapshot(PatientEntity e) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("name", str(e.getName()));
        values.put("birthDate", str(e.getBirthDate()));
        values.put("gender", str(e.getGender()));
        values.put("address", str(e.getAddress()));
        values.put("phoneNo", str(e.getPhoneNo()));
        values.put("email", str(e.getEmail()));
        values.put("identityNo", str(e.getIdentityNo()));
        values.put("bloodType", str(e.getBloodType()));
        values.put("allergies", str(e.getAllergies()));
        values.put("medicalHistory", str(e.getMedicalHistory()));
        values.put("insuranceNo", str(e.getInsuranceNo()));
        values.put("insuranceExpiry", str(e.getInsuranceExpiry()));
        values.put("emergencyContactName", str(e.getEmergencyContactName()));
        values.put("emergencyContactPhone", str(e.getEmergencyContactPhone()));
        values.put("emergencyContactRelation", str(e.getEmergencyContactRelation()));
        values.put("status", str(e.getStatus()));
        values.put("note", str(e.getNote()));
        return values;
    }

    private static List<AuditLogChange> patientChanges(
            Map<String, String> before,
            Map<String, String> after
    ) {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        snapshotKeys().forEach(key -> fields.put(key, null));
        List<AuditLogChange> changes = new ArrayList<>();
        int displayOrder = 0;
        for (String fieldKey : fields.keySet()) {
            String oldValue = before.get(fieldKey);
            String newValue = after.get(fieldKey);
            if (Objects.equals(oldValue, newValue)) {
                displayOrder++;
                continue;
            }
            AuditLogValueType valueType = patientValueType(fieldKey);
            if (MEDICAL_AUDIT_FIELDS.contains(fieldKey)) {
                changes.add(AuditLogChange.medical(
                        fieldKey, valueType, oldValue, newValue, displayOrder));
            } else if (PERSONAL_AUDIT_FIELDS.contains(fieldKey)) {
                changes.add(AuditLogChange.personal(
                        fieldKey, valueType, oldValue, newValue, displayOrder));
            } else if (valueType == AuditLogValueType.REFERENCE) {
                changes.add(AuditLogChange.generalReference(
                        fieldKey,
                        valueType,
                        oldValue,
                        newValue,
                        patientDisplayValue(fieldKey, oldValue),
                        patientDisplayValue(fieldKey, newValue),
                        displayOrder
                ));
            } else {
                changes.add(AuditLogChange.general(
                        fieldKey, valueType, oldValue, newValue, displayOrder));
            }
            displayOrder++;
        }
        return changes;
    }

    private static List<String> snapshotKeys() {
        return List.of(
                "name", "birthDate", "gender", "address", "phoneNo", "email", "identityNo",
                "bloodType", "allergies", "medicalHistory", "insuranceNo", "insuranceExpiry",
                "emergencyContactName", "emergencyContactPhone", "emergencyContactRelation",
                "status", "note"
        );
    }

    private static AuditLogValueType patientValueType(String fieldKey) {
        return switch (fieldKey) {
            case "birthDate", "insuranceExpiry" -> AuditLogValueType.DATE;
            case "gender", "bloodType", "status" -> AuditLogValueType.REFERENCE;
            default -> AuditLogValueType.TEXT;
        };
    }

    private static String patientDisplayValue(String fieldKey, String value) {
        if (value == null) {
            return null;
        }
        int code;
        try {
            code = Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return value;
        }
        return switch (fieldKey) {
            case "gender" -> switch (code) {
                case 1 -> "Male";
                case 2 -> "Female";
                case 3 -> "Other";
                default -> value;
            };
            case "bloodType" -> switch (code) {
                case 1 -> "A+";
                case 2 -> "A-";
                case 3 -> "B+";
                case 4 -> "B-";
                case 5 -> "AB+";
                case 6 -> "AB-";
                case 7 -> "O+";
                case 8 -> "O-";
                default -> value;
            };
            case "status" -> switch (code) {
                case 0 -> "Waiting";
                case 1 -> "Examining";
                case 2 -> "Admitted";
                case 3 -> "Discharged";
                default -> value;
            };
            default -> value;
        };
    }

    private static String patientLabel(PatientEntity patient) {
        return patient.getName() + " · " + patient.getId();
    }

    private static String changeSummary(int count) {
        if (count == 0) {
            return "No field changes";
        }
        return count + (count == 1 ? " field changed" : " fields changed");
    }

    private static String str(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.length() > VALUE_MAX_LENGTH ? text.substring(0, VALUE_MAX_LENGTH) : text;
    }
}
