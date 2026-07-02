package com.usth.hospital.service;

import com.usth.hospital.dto.request.CreatePatientRequest;
import com.usth.hospital.dto.request.UpdatePatientRequest;
import com.usth.hospital.dto.response.PatientDetailResponse;
import com.usth.hospital.dto.response.PatientHistoryEntryResponse;
import com.usth.hospital.dto.response.PatientHistoryResponse;
import com.usth.hospital.dto.response.PatientListResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PatientService {

    Page<PatientListResponse> list(String search, String searchField, Integer status, Integer gender,
                                   Pageable pageable);

    PatientDetailResponse getDetail(String id);

    PatientDetailResponse create(CreatePatientRequest request);

    PatientDetailResponse update(String id, UpdatePatientRequest request);

    void delete(String id);

    void bulkDelete(List<String> ids);

    Page<PatientHistoryResponse> getHistory(String id, Pageable pageable);

    Page<PatientHistoryEntryResponse> getAllHistory(Pageable pageable);
}
