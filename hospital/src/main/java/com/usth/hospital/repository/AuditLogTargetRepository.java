package com.usth.hospital.repository;

import com.usth.hospital.model.AuditLogTargetEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogTargetRepository extends JpaRepository<AuditLogTargetEntity, Long> {

    List<AuditLogTargetEntity> findByAuditLogIdOrderByDisplayOrderAscIdAsc(Long auditLogId);
}
