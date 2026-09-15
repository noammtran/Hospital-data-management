package com.usth.hospital.repository;

import com.usth.hospital.model.CareTaskEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CareTaskRepository extends JpaRepository<CareTaskEntity, Long> {
    List<CareTaskEntity> findByVisitIdOrderByCreatedAtAscIdAsc(String visitId);
    List<CareTaskEntity> findByVisitIdIn(List<String> visitIds);
    Optional<CareTaskEntity> findByVisitIdAndRequestKey(String visitId, String requestKey);
    @Query("select t.visitId from CareTaskEntity t where t.id=:id")
    Optional<String> visitId(Long id);
    @Query("""
        select t from CareTaskEntity t join CareVisitEntity v on v.id=t.visitId
        join PatientEntity p on p.id=v.patientId
        where (:allStaff=true or exists (select a.id from CareAssignmentEntity a
          where a.userId=:userId and a.pointId=t.pointId and a.active=1))
        and (:pointId is null or t.pointId=:pointId)
        and (:kind is null or t.kind=:kind)
        and (:status is null or t.status=:status)
        and (:arrival is null or t.arrival=:arrival)
        and (:openOnly=false or t.closedAt is null)
        and (:search is null or lower(p.name) like lower(concat('%',concat(:search,'%')))
          or lower(p.id) like lower(concat('%',concat(:search,'%'))))
        order by v.priority desc, t.createdAt, t.id
        """)
    Page<CareTaskEntity> search(boolean allStaff, Long userId, Long pointId, String kind,
                               String status, String arrival, boolean openOnly, String search, Pageable pageable);
}
