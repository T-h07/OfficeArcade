package com.officearcade.server.challenges.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeTypeEntityRepository extends JpaRepository<ChallengeTypeEntity, UUID> {

    List<ChallengeTypeEntity> findAllByEnabledTrueOrderByDisplayNameAsc();

    Optional<ChallengeTypeEntity> findByCodeIgnoreCase(String code);
}
