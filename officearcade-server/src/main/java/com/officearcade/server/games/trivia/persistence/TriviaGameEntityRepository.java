package com.officearcade.server.games.trivia.persistence;

import com.officearcade.server.games.trivia.TriviaGameStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TriviaGameEntityRepository extends JpaRepository<TriviaGameEntity, UUID> {

    Optional<TriviaGameEntity> findByRoom_Id(UUID roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from TriviaGameEntity g where g.room.id = :roomId")
    Optional<TriviaGameEntity> findByRoomIdForUpdate(@Param("roomId") UUID roomId);

    boolean existsByRoom_IdAndStatus(UUID roomId, TriviaGameStatus status);
}
