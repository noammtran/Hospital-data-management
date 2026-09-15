package com.usth.hospital.dto.response;

import java.util.List;

public record StaffWorkProfileResponse(
        UserResponse user,
        List<String> rolePermissions,
        Doctor doctor,
        List<Assignment> assignments,
        List<String> setupIssues
) {
    public record Doctor(Long id, String code, String name, String specialization,
                         Long departmentId, String departmentName, boolean active) {}
    public record Assignment(Long id, Long pointId, String pointName, String pointKind,
                             String departmentName, boolean pointActive, boolean active,
                             boolean perform, boolean verify) {}
}
