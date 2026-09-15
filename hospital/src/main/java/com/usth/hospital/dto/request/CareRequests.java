package com.usth.hospital.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class CareRequests {
    private CareRequests() {}
    public record Register(@NotBlank @Size(max=50) String patientId, @NotNull @Positive Long pointId,
                           @NotBlank @Size(max=500) String complaint, @Min(0) @Max(2) int priority) {}
    public record Arrival(@NotBlank @Pattern(regexp="ARRIVED|CALLED|ABSENT|LEFT") String state) {}
    public record Note(@NotBlank @Pattern(regexp="VITALS|EXAM|REVIEW|COORDINATION") String kind,
                       @NotBlank @Size(max=4000) String body) {}
    public record Order(@NotNull @Positive Long serviceId, @NotBlank @Size(max=1000) String reason) {}
    public record Orders(@NotBlank @Pattern(regexp="[A-Za-z0-9-]{1,50}") String requestId,
                         @NotEmpty @Size(max=20) List<@Valid Order> items) {}
    public record Result(@NotBlank @Size(max=4000) String body, boolean critical,
                         @Size(max=1000) String correctionReason) {}
    public record Acknowledge(@NotNull @Positive Long resultId) {}
    public record Reason(@NotBlank @Size(max=1000) String reason) {}
    public record Route(@NotNull @Positive Long pointId, @NotBlank @Size(max=1000) String reason) {}
    public record Finish(@NotBlank @Size(max=2000) String conclusion,
                         @NotBlank @Size(max=2000) String plan,
                         @NotBlank @Pattern(regexp="OUTPATIENT|FOLLOW_UP|LEFT_INCOMPLETE|REFERRED") String outcome,
                         @Size(max=1000) String followupPlan, LocalDateTime followupAt) {}
    public record Handoff(@NotNull @Positive Long toUserId, @NotNull @Positive Long pointId,
                          @NotBlank @Pattern(regexp="TRANSFER|CONSULT") String kind,
                          @NotBlank @Size(max=1000) String reason) {}
    public record Respond(boolean accept, @NotBlank @Size(max=2000) String response) {}
    public record AdmissionRequest(@NotNull @Positive Long wardPointId,
                                   @NotBlank @Size(max=2000) String conclusion,
                                   @NotBlank @Size(max=2000) String plan) {}
    public record AcceptAdmission(@NotNull @Positive Long doctorUserId, @Positive Long roomId) {}
    public record AdoptAdmission(@NotNull @Positive Long wardPointId,
                                 @NotNull @Positive Long doctorUserId) {}
    public record Assignment(@NotNull @Positive Long userId, @NotNull @Positive Long pointId,
                             boolean perform, boolean verify, boolean active, @Positive Long doctorId) {}
    public record Point(@NotBlank @Size(max=30) @Pattern(regexp="[A-Z0-9_-]+") String code,
                        @NotBlank @Size(max=100) String name,
                        @NotBlank @Pattern(regexp="CLINIC|SERVICE|WARD") String kind,
                        @NotNull @Positive Long departmentId, boolean active) {}
    public record Service(@NotBlank @Size(max=30) @Pattern(regexp="[A-Z0-9_-]+") String code,
                          @NotBlank @Size(max=100) String name, @NotNull @Positive Long pointId,
                          boolean active) {}
    public record Prescription(@NotNull @Positive Long medicineId,
                               @NotBlank @Size(max=100) String dosage,
                               @NotBlank @Size(max=100) String frequency,
                               @NotBlank @Size(max=50) String duration,
                               @Size(max=500) String instructions) {}
    public record Diagnosis(@NotBlank @Size(max=500) String description,
                            @Size(max=10) String icdCode,
                            @Size(max=30) List<@Valid Prescription> prescriptions) {}
}
