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
import com.usth.hospital.model.PatientEntity;
import com.usth.hospital.model.PatientHistoryEntity;
import com.usth.hospital.repository.PatientHistoryRepository;
import com.usth.hospital.repository.PatientRepository;
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

    private static final String SYSTEM_USER = "SYSTEM";
    private static final int VALUE_MAX_LENGTH = 255;

    private final PatientRepository patientRepository;
    private final PatientHistoryRepository patientHistoryRepository;
    private final PatientMapper patientMapper;
    private final PatientIdGenerator patientIdGenerator;

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
        LocalDate now = LocalDate.now();
        entity.setId(nextPatientId(now.getYear()));
        entity.setCreatedBy(SYSTEM_USER);
        entity.setCreatedDate(now);
        entity.setLastUpdatedBy(SYSTEM_USER);
        entity.setLastUpdatedDate(now);
        PatientEntity saved = patientRepository.save(entity);
        return patientMapper.toDetail(saved);
    }

    @Override
    public PatientDetailResponse update(String id, UpdatePatientRequest request) {
        PatientEntity entity = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));
        requireUniqueIdentity(request.identityNo(), id);
        Map<String, String> before = snapshot(entity);
        patientMapper.updateEntity(request, entity);
        LocalDate now = LocalDate.now();
        entity.setLastUpdatedBy(SYSTEM_USER);
        entity.setLastUpdatedDate(now);
        PatientEntity saved = patientRepository.save(entity);
        recordHistory(id, before, snapshot(saved), now);
        return patientMapper.toDetail(saved);
    }

    @Override
    public void delete(String id) {
        if (!patientRepository.existsById(id)) {
            throw new PatientNotFoundException(id);
        }
        patientRepository.deleteById(id);
    }

    @Override
    public void bulkDelete(List<String> ids) {
        patientRepository.deleteAllById(ids);
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

    private void recordHistory(String patientId, Map<String, String> before,
                               Map<String, String> after, LocalDate changedDate) {
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
                        .changedBy(SYSTEM_USER)
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

    private static String str(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.length() > VALUE_MAX_LENGTH ? text.substring(0, VALUE_MAX_LENGTH) : text;
    }
}
