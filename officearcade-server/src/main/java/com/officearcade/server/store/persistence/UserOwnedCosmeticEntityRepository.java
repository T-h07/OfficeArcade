package com.officearcade.server.store.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOwnedCosmeticEntityRepository extends JpaRepository<UserOwnedCosmeticEntity, UUID> {

    boolean existsByUser_IdAndCosmeticItem_Id(UUID userId, UUID cosmeticItemId);

    long countByUser_Id(UUID userId);

    Optional<UserOwnedCosmeticEntity> findByUser_IdAndCosmeticItem_Id(UUID userId, UUID cosmeticItemId);

    List<UserOwnedCosmeticEntity> findAllByUser_IdOrderByAcquiredAtDesc(UUID userId);
}
