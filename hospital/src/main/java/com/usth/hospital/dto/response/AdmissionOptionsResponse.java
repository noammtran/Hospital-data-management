package com.usth.hospital.dto.response;

import java.util.List;

public record AdmissionOptionsResponse(
        List<DepartmentOption> departments,
        List<DoctorOption> doctors,
        List<RoomOption> rooms,
        List<LookupOption> types
) {

    public record DepartmentOption(Long id, String code, String name) {
    }

    public record DoctorOption(Long id, String code, String name, Long departmentId) {
    }

    public record RoomOption(
            Long id,
            String name,
            Long departmentId,
            Integer bedCount,
            Integer currentOccupancy
    ) {
    }

    public record LookupOption(Integer value, String label) {
    }
}
