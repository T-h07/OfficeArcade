package com.officearcade.server.playlimits.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPlayLimitStateEntityRepository extends JpaRepository<UserPlayLimitStateEntity, UUID> {

    Optional<UserPlayLimitStateEntity> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from UserPlayLimitStateEntity p where p.userId = :userId")
    Optional<UserPlayLimitStateEntity> findByUserIdForUpdate(@Param("userId") UUID userId);
}
