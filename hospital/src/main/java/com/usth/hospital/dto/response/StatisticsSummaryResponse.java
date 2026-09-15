package com.usth.hospital.dto.response;

import java.util.List;

/** Aggregate clinical activity and patient-cohort breakdowns. */
public record StatisticsSummaryResponse(
        long admissions,
        long diagnoses,
        long prescriptions,
        List<StatisticsBreakdownResponse> patientStatuses,
        List<StatisticsBreakdownResponse> genders,
        List<StatisticsBreakdownResponse> ageGroups
) {
}
