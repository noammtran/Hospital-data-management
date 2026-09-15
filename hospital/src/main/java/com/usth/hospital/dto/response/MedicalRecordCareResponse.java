package com.usth.hospital.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/** Read-only clinical record. Unpublished service revisions are never included. */
public record MedicalRecordCareResponse(
        String id, LocalDateTime createdAt, LocalDateTime closedAt, String status,
        String pointName, String doctorName, String complaint, String conclusion,
        String plan, String outcome, String followupPlan, LocalDateTime followupAt,
        List<Service> services, List<CareResponses.Note> notes,
        List<CareResponses.Handoff> handoffs) {

    public record Service(Long id, String name, String status, String reason,
                          String resolution, List<CareResponses.Result> results) {}

    public static MedicalRecordCareResponse from(CareResponses.Detail detail) {
        var visit = detail.visit();
        return new MedicalRecordCareResponse(visit.id(), visit.createdAt(), visit.closedAt(),
                visit.status(), visit.pointName(), visit.ownerName(), detail.complaint(),
                detail.conclusion(), detail.plan(), detail.outcome(), detail.followupPlan(),
                detail.followupAt(), detail.tasks().stream().filter(t -> "ORDER".equals(t.kind()))
                    .map(t -> new Service(t.id(), t.serviceName(), t.status(), t.reason(),
                            t.resolution(), t.results().stream()
                                .filter(r -> r.publishedAt() != null).toList())).toList(),
                detail.notes(), detail.handoffs().stream().map(h -> new CareResponses.Handoff(
                    h.id(), h.kind(), h.status(), h.fromUser(), h.toUserId(), h.toUser(),
                    h.pointId(), h.pointName(), h.reason(), h.response(), h.createdAt(),
                    h.respondedAt(), false)).toList());
    }
}
