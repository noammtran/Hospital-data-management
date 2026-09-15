package com.usth.hospital.controller;

import com.usth.hospital.dto.request.CareRequests;
import com.usth.hospital.dto.response.*;
import com.usth.hospital.service.impl.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/care")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('CARE_VIEW')")
public class CareController {
    private final CareQueryService query;
    private final CareWorkflowService workflow;
    private final CareConfigurationService configuration;

    @GetMapping("/options")
    public ApiResponse<CareResponses.Options> options() { return ApiResponse.ok(query.options()); }

    @GetMapping("/visits")
    public ApiResponse<PageResponse<CareResponses.Visit>> visits(
            @RequestParam(required=false) @Size(max=50) String patientId,
            @RequestParam(required=false) @Positive Long pointId,
            @RequestParam(required=false) @Pattern(regexp="WAITING|ACTIVE|AWAITING_ADMISSION|INPATIENT|COMPLETED|CANCELLED") String status,
            @RequestParam(defaultValue="true") boolean openOnly,
            @RequestParam(required=false) @Size(max=100) String search,
            @RequestParam(defaultValue="1") @Min(1) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(query.visits(patientId,pointId,status,openOnly,search,page,pageSize));
    }

    @GetMapping("/tasks")
    public ApiResponse<PageResponse<CareResponses.Task>> tasks(
            @RequestParam(required=false) @Positive Long pointId,
            @RequestParam(required=false) @Pattern(regexp="EXAM|ORDER") String kind,
            @RequestParam(required=false) @Pattern(regexp="QUEUED|IN_PROGRESS|WAITING_RESULTS|AWAITING_VALIDATION|RESULT_READY|COMPLETED|CANCEL_REQUESTED|CANCELLED|DECLINED") String status,
            @RequestParam(required=false) @Pattern(regexp="EXPECTED|ARRIVED|CALLED|ABSENT|LEFT") String arrival,
            @RequestParam(defaultValue="true") boolean openOnly,
            @RequestParam(required=false) @Size(max=100) String search,
            @RequestParam(defaultValue="1") @Min(1) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(query.tasks(pointId,kind,status,arrival,openOnly,search,page,pageSize));
    }

    @GetMapping("/inpatients")
    public ApiResponse<PageResponse<CareResponses.Inpatient>> inpatients(
            @RequestParam(required=false) @Positive Long departmentId,
            @RequestParam(required=false) @Size(max=100) String search,
            @RequestParam(defaultValue="1") @Min(1) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(query.inpatients(departmentId,search,page,pageSize));
    }

    @GetMapping("/medicines")
    public ApiResponse<PageResponse<CareResponses.Reference>> medicines(
            @RequestParam(required=false) @Size(max=100) String search,
            @RequestParam(defaultValue="1") @Min(1) int page,
            @RequestParam(defaultValue="30") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(query.medicines(search,page,pageSize));
    }

    @GetMapping("/visits/{id}")
    public ApiResponse<CareResponses.Detail> detail(@PathVariable String id) { return ApiResponse.ok(query.detail(id)); }

    @PostMapping("/visits")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CARE_REGISTER')")
    public ApiResponse<CareResponses.Detail> register(@Valid @RequestBody CareRequests.Register request) {
        return ApiResponse.created(query.detail(workflow.register(request)));
    }

    @PostMapping("/admissions/{id}/workspace")
    @PreAuthorize("hasAuthority('CARE_ADMIT')")
    public ApiResponse<CareResponses.Detail> adoptAdmission(@PathVariable String id,@Valid @RequestBody CareRequests.AdoptAdmission request) {
        return ApiResponse.ok(query.detail(workflow.adoptAdmission(id,request)));
    }

    @PostMapping("/visits/{id}/notes")
    public ApiResponse<CareResponses.Detail> note(@PathVariable String id, @Valid @RequestBody CareRequests.Note request) {
        workflow.note(id,request);
        return ApiResponse.ok(query.detail(id));
    }

    @PostMapping("/visits/{id}/orders")
    public ApiResponse<CareResponses.Detail> order(@PathVariable String id, @Valid @RequestBody CareRequests.Orders request) {
        workflow.order(id,request);
        return ApiResponse.ok(query.detail(id));
    }

    @PostMapping("/visits/{id}/route")
    public ApiResponse<CareResponses.Detail> reroute(@PathVariable String id, @Valid @RequestBody CareRequests.Route request) {
        workflow.reroute(id,request);
        return ApiResponse.ok(query.detail(id));
    }

    @PostMapping("/visits/{id}/cancel")
    public ApiResponse<CareResponses.Detail> cancelVisit(@PathVariable String id, @Valid @RequestBody CareRequests.Reason request) {
        workflow.cancelVisit(id,request);
        return ApiResponse.ok(query.detail(id));
    }

    @PostMapping("/visits/{id}/finish")
    public ApiResponse<CareResponses.Detail> finish(@PathVariable String id, @Valid @RequestBody CareRequests.Finish request) {
        workflow.finish(id,request);
        return ApiResponse.ok(query.detail(id));
    }

    @PostMapping("/visits/{id}/admission/request")
    public ApiResponse<CareResponses.Detail> requestAdmission(@PathVariable String id, @Valid @RequestBody CareRequests.AdmissionRequest request) {
        workflow.requestAdmission(id,request);
        return ApiResponse.ok(query.detail(id));
    }

    @PostMapping("/visits/{id}/admission/cancel")
    public ApiResponse<CareResponses.Detail> cancelAdmission(@PathVariable String id, @Valid @RequestBody CareRequests.Reason request) {
        workflow.cancelAdmission(id,request);
        return ApiResponse.ok(query.detail(id));
    }

    @PostMapping("/visits/{id}/admission/accept")
    public ApiResponse<CareResponses.Detail> acceptAdmission(@PathVariable String id, @Valid @RequestBody CareRequests.AcceptAdmission request) {
        workflow.acceptAdmission(id,request);
        return ApiResponse.ok(query.detail(id));
    }

    @PostMapping("/visits/{id}/discharge")
    public ApiResponse<CareResponses.Detail> discharge(@PathVariable String id, @Valid @RequestBody CareRequests.Finish request) {
        workflow.discharge(id,request);
        return ApiResponse.ok(query.detail(id));
    }

    @PostMapping("/visits/{id}/handoffs")
    public ApiResponse<CareResponses.Detail> handoff(@PathVariable String id, @Valid @RequestBody CareRequests.Handoff request) {
        workflow.handoff(id,request);
        return ApiResponse.ok(query.detail(id));
    }

    @PostMapping("/visits/{id}/diagnoses")
    public ApiResponse<CareResponses.Detail> diagnosis(@PathVariable String id, @Valid @RequestBody CareRequests.Diagnosis request) {
        workflow.diagnosis(id,request);
        return ApiResponse.ok(query.detail(id));
    }

    @PostMapping("/visits/{id}/ward-arrival")
    public ApiResponse<CareResponses.Detail> wardArrival(@PathVariable String id) {
        workflow.wardArrival(id);
        return ApiResponse.ok(query.detail(id));
    }

    @PostMapping("/tasks/{id}/arrival")
    public ApiResponse<CareResponses.Detail> arrival(@PathVariable Long id, @Valid @RequestBody CareRequests.Arrival request) {
        workflow.arrival(id,request);
        return ApiResponse.ok(query.detail(query.taskVisit(id)));
    }

    @PostMapping("/tasks/{id}/start")
    public ApiResponse<CareResponses.Detail> start(@PathVariable Long id) {
        workflow.start(id);
        return ApiResponse.ok(query.detail(query.taskVisit(id)));
    }

    @PostMapping("/tasks/{id}/result")
    public ApiResponse<CareResponses.Detail> draftResult(@PathVariable Long id, @Valid @RequestBody CareRequests.Result request) {
        workflow.draftResult(id,request);
        return ApiResponse.ok(query.detail(query.taskVisit(id)));
    }

    @PostMapping("/tasks/{id}/publish")
    public ApiResponse<CareResponses.Detail> publish(@PathVariable Long id) {
        workflow.publish(id);
        return ApiResponse.ok(query.detail(query.taskVisit(id)));
    }

    @PostMapping("/tasks/{id}/acknowledge")
    public ApiResponse<CareResponses.Detail> acknowledgeResult(@PathVariable Long id, @Valid @RequestBody CareRequests.Acknowledge request) {
        workflow.acknowledgeResult(id,request);
        return ApiResponse.ok(query.detail(query.taskVisit(id)));
    }

    @PostMapping("/tasks/{id}/cancel")
    public ApiResponse<CareResponses.Detail> cancelOrder(@PathVariable Long id, @Valid @RequestBody CareRequests.Reason request) {
        workflow.cancelOrder(id,request);
        return ApiResponse.ok(query.detail(query.taskVisit(id)));
    }

    @PostMapping("/tasks/{id}/decline")
    public ApiResponse<CareResponses.Detail> declineOrder(@PathVariable Long id, @Valid @RequestBody CareRequests.Reason request) {
        workflow.declineOrder(id,request);
        return ApiResponse.ok(query.detail(query.taskVisit(id)));
    }

    @PostMapping("/tasks/{id}/resolve")
    public ApiResponse<CareResponses.Detail> resolveOrder(@PathVariable Long id, @Valid @RequestBody CareRequests.Reason request) {
        workflow.resolveOrder(id,request);
        return ApiResponse.ok(query.detail(query.taskVisit(id)));
    }

    @PostMapping("/visits/{id}/handoffs/{handoffId}/response")
    public ApiResponse<CareResponses.Detail> respond(@PathVariable String id,@PathVariable Long handoffId,@Valid @RequestBody CareRequests.Respond request) {
        workflow.respond(id,handoffId,request); return ApiResponse.ok(query.detail(id));
    }

    @PostMapping("/visits/{id}/handoffs/{handoffId}/cancel")
    public ApiResponse<CareResponses.Detail> cancelHandoff(@PathVariable String id,@PathVariable Long handoffId,@Valid @RequestBody CareRequests.Reason request) {
        workflow.cancelHandoff(id,handoffId,request); return ApiResponse.ok(query.detail(id));
    }

    @PostMapping("/points")
    @PreAuthorize("hasAuthority('CARE_MANAGE')")
    public ApiResponse<CareResponses.Options> createPoint(@Valid @RequestBody CareRequests.Point request) {
        configuration.point(null,request); return ApiResponse.ok(query.options());
    }

    @PutMapping("/points/{id}")
    @PreAuthorize("hasAuthority('CARE_MANAGE')")
    public ApiResponse<CareResponses.Options> updatePoint(@PathVariable Long id,@Valid @RequestBody CareRequests.Point request) {
        configuration.point(id,request); return ApiResponse.ok(query.options());
    }

    @PostMapping("/services")
    @PreAuthorize("hasAuthority('CARE_MANAGE')")
    public ApiResponse<CareResponses.Options> createService(@Valid @RequestBody CareRequests.Service request) {
        configuration.service(null,request); return ApiResponse.ok(query.options());
    }

    @PutMapping("/services/{id}")
    @PreAuthorize("hasAuthority('CARE_MANAGE')")
    public ApiResponse<CareResponses.Options> updateService(@PathVariable Long id,@Valid @RequestBody CareRequests.Service request) {
        configuration.service(id,request); return ApiResponse.ok(query.options());
    }

    @PutMapping("/assignments")
    @PreAuthorize("hasAuthority('CARE_MANAGE')")
    public ApiResponse<CareResponses.Options> assignment(@Valid @RequestBody CareRequests.Assignment request) {
        configuration.assignment(request); return ApiResponse.ok(query.options());
    }
}
