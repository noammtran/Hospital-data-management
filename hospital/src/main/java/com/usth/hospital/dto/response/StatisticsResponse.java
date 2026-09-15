package com.usth.hospital.dto.response;

import java.util.List;

/** Statistics for one filtered patient cohort. */
public record StatisticsResponse(
        long total,
        List<PatientRefResponse> patients,
        StatisticsSummaryResponse summary
) {
}
