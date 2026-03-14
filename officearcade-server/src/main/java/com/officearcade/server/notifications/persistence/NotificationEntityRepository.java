package com.officearcade.server.notifications.persistence;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationEntityRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);

    long countByUserIdAndReadAtIsNull(UUID userId);

    Optional<NotificationEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndEventKey(UUID userId, String eventKey);

    @Modifying
    @Transactional
    @Query("""
            update NotificationEntity n
            set n.readAt = :readAt,
                n.updatedAt = :readAt
            where n.userId = :userId
              and n.readAt is null
            """)
    int markAllReadForUser(@Param("userId") UUID userId, @Param("readAt") Instant readAt);
}
