package com.officearcade.server.games.connectfour.persistence;

import com.officearcade.server.games.connectfour.ConnectFourGameStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConnectFourGameEntityRepository extends JpaRepository<ConnectFourGameEntity, UUID> {

    Optional<ConnectFourGameEntity> findByRoom_Id(UUID roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from ConnectFourGameEntity g where g.room.id = :roomId")
    Optional<ConnectFourGameEntity> findByRoomIdForUpdate(@Param("roomId") UUID roomId);

    boolean existsByRoom_IdAndStatus(UUID roomId, ConnectFourGameStatus status);
}
