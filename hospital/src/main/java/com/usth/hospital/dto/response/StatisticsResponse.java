package com.usth.hospital.dto.response;

import java.util.List;

/** A statistics result: how many patients matched, and who they are. */
public record StatisticsResponse(long total, List<PatientRefResponse> patients) {
}
