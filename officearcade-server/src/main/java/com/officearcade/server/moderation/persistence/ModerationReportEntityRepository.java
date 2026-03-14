package com.officearcade.server.moderation.persistence;

import com.officearcade.server.moderation.ModerationReportStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ModerationReportEntityRepository
        extends JpaRepository<ModerationReportEntity, UUID>, JpaSpecificationExecutor<ModerationReportEntity> {

    @Query("""
            select r
            from ModerationReportEntity r
            where r.reporterUser.id = :reporterUserId
            order by r.createdAt desc
            """)
    List<ModerationReportEntity> findAllByReporterUserId(@Param("reporterUserId") UUID reporterUserId);

    @Query("""
            select r
            from ModerationReportEntity r
            where r.status in :statuses
            order by r.createdAt asc
            """)
    List<ModerationReportEntity> findAllByStatusInOrderByCreatedAtAsc(
            @Param("statuses") Collection<ModerationReportStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ModerationReportEntity r where r.id = :reportId")
    Optional<ModerationReportEntity> findByIdForUpdate(@Param("reportId") UUID reportId);
}
