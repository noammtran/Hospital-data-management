package com.usth.hospital.dto.response;

/** Minimal patient identity used in statistics result lists. The id stays for
 *  drill-down; the identity card number is the human-readable label shown. */
public record PatientRefResponse(String id, String name, String identityNo) {
}
