package com.officearcade.server.profiles.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerProfileEntityRepository extends JpaRepository<PlayerProfileEntity, UUID> {

    boolean existsByUserId(UUID userId);

    Optional<PlayerProfileEntity> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PlayerProfileEntity p where p.userId = :userId")
    Optional<PlayerProfileEntity> findByUserIdForUpdate(@Param("userId") UUID userId);
}
