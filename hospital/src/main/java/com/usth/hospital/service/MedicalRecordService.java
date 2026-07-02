package com.usth.hospital.service;

import com.usth.hospital.dto.response.VisitResponse;
import java.util.List;

public interface MedicalRecordService {

    List<VisitResponse> getMedicalRecord(String patientId);
}
