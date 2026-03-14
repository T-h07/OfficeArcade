package com.officearcade.server.moderation.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationAuditLogEntityRepository extends JpaRepository<ModerationAuditLogEntity, UUID> {

    List<ModerationAuditLogEntity> findTop100ByOrderByCreatedAtDesc();
}
