package com.officearcade.server.games.uno.persistence;

import com.officearcade.server.games.uno.UnoGameStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnoGameEntityRepository extends JpaRepository<UnoGameEntity, UUID> {

    Optional<UnoGameEntity> findByRoom_Id(UUID roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from UnoGameEntity g where g.room.id = :roomId")
    Optional<UnoGameEntity> findByRoomIdForUpdate(@Param("roomId") UUID roomId);

    boolean existsByRoom_IdAndStatus(UUID roomId, UnoGameStatus status);
}
