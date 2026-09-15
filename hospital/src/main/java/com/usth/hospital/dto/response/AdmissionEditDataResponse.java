package com.usth.hospital.dto.response;

public record AdmissionEditDataResponse(
        AdmissionResponse admission,
        AdmissionOptionsResponse options
) {
}
