package com.usth.hospital.service;

import com.usth.hospital.dto.response.PatientSelfRecordResponse;

public interface PatientPortalService {
    PatientSelfRecordResponse getOwnRecord();
}
