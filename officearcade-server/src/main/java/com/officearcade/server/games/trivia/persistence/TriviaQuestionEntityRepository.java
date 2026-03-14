package com.officearcade.server.games.trivia.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TriviaQuestionEntityRepository extends JpaRepository<TriviaQuestionEntity, UUID> {

    List<TriviaQuestionEntity> findAllByEnabledTrueOrderByCodeAsc();

    Optional<TriviaQuestionEntity> findByCodeIgnoreCase(String code);
}
