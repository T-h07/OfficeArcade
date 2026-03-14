package com.officearcade.server.games.trivia.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TriviaRoundAnswerEntityRepository extends JpaRepository<TriviaRoundAnswerEntity, UUID> {

    List<TriviaRoundAnswerEntity> findAllByGame_IdAndRoundNumberOrderBySubmittedAtAsc(UUID gameId, int roundNumber);

    Optional<TriviaRoundAnswerEntity> findByGame_IdAndRoundNumberAndUser_Id(UUID gameId, int roundNumber, UUID userId);

    void deleteAllByGame_Id(UUID gameId);
}
