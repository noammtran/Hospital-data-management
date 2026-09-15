package com.usth.hospital.repository;

import com.usth.hospital.model.CareVisitEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CareVisitRepository extends JpaRepository<CareVisitEntity, String> {
    Optional<CareVisitEntity> findByPatientIdAndClosedAtIsNull(String patientId);
    boolean existsByPatientIdAndClosedAtIsNull(String patientId);
    Optional<CareVisitEntity> findByAdmissionIdAndClosedAtIsNull(String admissionId);
    List<CareVisitEntity> findByPatientIdOrderByCreatedAtDesc(String patientId);

    @Query("select v.patientId from CareVisitEntity v where v.id=:id")
    Optional<String> patientId(String id);

    @Query("""
        select v from CareVisitEntity v join PatientEntity p on p.id=v.patientId
        where (:allStaff=true or v.ownerId=:userId or v.followupOwnerId=:userId
          or exists (select a.id from CareAssignmentEntity a where a.userId=:userId and a.active=1
            and (a.pointId=v.pointId or a.pointId=v.wardPointId))
          or exists (select t.id from CareTaskEntity t, CareAssignmentEntity a
            where t.visitId=v.id and a.pointId=t.pointId and a.userId=:userId and a.active=1)
          or exists (select h.id from CareHandoffEntity h where h.visitId=v.id and h.toUserId=:userId))
        and (:patientId is null or v.patientId=:patientId)
        and (:pointId is null or v.pointId=:pointId or v.wardPointId=:pointId
          or exists (select t.id from CareTaskEntity t where t.visitId=v.id and t.pointId=:pointId and t.closedAt is null))
        and (:status is null or v.status=:status)
        and (:openOnly=false or v.closedAt is null
          or exists (select t.id from CareTaskEntity t where t.visitId=v.id and t.kind='ORDER' and t.closedAt is null))
        and (:search is null or lower(p.name) like lower(concat('%',concat(:search,'%')))
          or lower(p.id) like lower(concat('%',concat(:search,'%'))))
        order by v.priority desc, v.createdAt, v.id
        """)
    Page<CareVisitEntity> search(boolean allStaff, Long userId, String patientId, Long pointId,
                               String status, boolean openOnly, String search, Pageable pageable);
}
