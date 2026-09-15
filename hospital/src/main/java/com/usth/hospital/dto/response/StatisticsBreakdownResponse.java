package com.usth.hospital.dto.response;

/** One stable category in a statistics breakdown. */
public record StatisticsBreakdownResponse(String key, String label, long count) {
}
