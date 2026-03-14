package com.officearcade.server.catalog.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameTypeEntityRepository extends JpaRepository<GameTypeEntity, UUID> {

    Optional<GameTypeEntity> findByCode(String code);

    List<GameTypeEntity> findAllByEnabledTrueOrderByDisplayNameAsc();
}
