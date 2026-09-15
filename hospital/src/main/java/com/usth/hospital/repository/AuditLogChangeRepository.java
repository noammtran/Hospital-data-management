package com.usth.hospital.repository;

import com.usth.hospital.model.AuditLogChangeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogChangeRepository extends JpaRepository<AuditLogChangeEntity, Long> {

    List<AuditLogChangeEntity> findByAuditLogIdOrderByDisplayOrderAscIdAsc(Long auditLogId);
}
