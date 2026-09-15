package com.usth.hospital.service;

import com.usth.hospital.dto.request.CreateAdmissionRequest;
import com.usth.hospital.dto.request.UpdateAdmissionRequest;
import com.usth.hospital.dto.response.AdmissionEditDataResponse;
import com.usth.hospital.dto.response.AdmissionOptionsResponse;
import com.usth.hospital.dto.response.AdmissionResponse;

public interface AdmissionService {

    AdmissionOptionsResponse getOptions();

    AdmissionEditDataResponse getEditData(String patientId, String admissionId);

    AdmissionResponse create(String patientId, CreateAdmissionRequest request);

    AdmissionResponse update(String patientId, String admissionId, UpdateAdmissionRequest request);

    AdmissionResponse discharge(String patientId, String admissionId);

    void delete(String patientId, String admissionId);
}
