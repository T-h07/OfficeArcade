package com.officearcade.server.profiles.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerProfileEntityRepository extends JpaRepository<PlayerProfileEntity, UUID> {

    boolean existsByUserId(UUID userId);

    Optional<PlayerProfileEntity> findByUserId(UUID userId);
}
