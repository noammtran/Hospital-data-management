package com.usth.hospital.repository;

import com.usth.hospital.model.AuditLogAction;
import com.usth.hospital.model.AuditLogEntity;
import com.usth.hospital.model.AuditLogEntityType;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    @Query("""
            SELECT event FROM AuditLogEntity event
            WHERE (:search IS NULL
                   OR LOWER(event.actor) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(event.actorDisplayName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(event.entityId) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(event.targetLabel) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(event.subjectId) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(event.subjectLabel) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(event.details) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(event.summary) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:actor IS NULL
                   OR LOWER(event.actor) = LOWER(:actor)
                   OR LOWER(event.actorDisplayName) = LOWER(:actor))
              AND (:action IS NULL OR event.action = :action)
              AND (:entityType IS NULL OR event.entityType = :entityType)
              AND (:fromInclusive IS NULL OR event.occurredAt >= :fromInclusive)
              AND (:toExclusive IS NULL OR event.occurredAt < :toExclusive)
            """)
    Page<AuditLogEntity> search(
            @Param("search") String search,
            @Param("actor") String actor,
            @Param("action") AuditLogAction action,
            @Param("entityType") AuditLogEntityType entityType,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive,
            Pageable pageable
    );
}
