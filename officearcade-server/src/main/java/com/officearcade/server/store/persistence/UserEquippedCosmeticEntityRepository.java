package com.officearcade.server.store.persistence;

import com.officearcade.server.store.CosmeticCategory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEquippedCosmeticEntityRepository extends JpaRepository<UserEquippedCosmeticEntity, UUID> {

    long countByUser_Id(UUID userId);

    Optional<UserEquippedCosmeticEntity> findByUser_IdAndCosmeticItem_Id(UUID userId, UUID cosmeticItemId);

    Optional<UserEquippedCosmeticEntity> findByUser_IdAndCategory(UUID userId, CosmeticCategory category);

    List<UserEquippedCosmeticEntity> findAllByUser_IdOrderByEquippedAtDesc(UUID userId);

    List<UserEquippedCosmeticEntity> findAllByUser_IdInOrderByUser_IdAscEquippedAtDesc(Collection<UUID> userIds);

    void deleteByUser_IdAndCosmeticItem_Id(UUID userId, UUID cosmeticItemId);
}
