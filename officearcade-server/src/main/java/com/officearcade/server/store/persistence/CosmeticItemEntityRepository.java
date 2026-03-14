package com.officearcade.server.store.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CosmeticItemEntityRepository
        extends JpaRepository<CosmeticItemEntity, UUID>, JpaSpecificationExecutor<CosmeticItemEntity> {

    Optional<CosmeticItemEntity> findByCodeIgnoreCase(String code);
}
