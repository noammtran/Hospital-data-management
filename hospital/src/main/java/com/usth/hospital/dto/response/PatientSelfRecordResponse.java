package com.usth.hospital.dto.response;

import java.util.List;

public record PatientSelfRecordResponse(
        PatientSelfProfileResponse patient,
        List<VisitResponse> records
) {
}
