package com.officearcade.server.lobby.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMemberEntityRepository extends JpaRepository<RoomMemberEntity, UUID> {

    Optional<RoomMemberEntity> findByUser_Id(UUID userId);

    Optional<RoomMemberEntity> findByRoom_IdAndUser_Id(UUID roomId, UUID userId);

    List<RoomMemberEntity> findAllByRoom_IdOrderByJoinedAtAsc(UUID roomId);

    List<RoomMemberEntity> findAllByRoom_IdIn(Collection<UUID> roomIds);

    long countByRoom_Id(UUID roomId);

    void deleteAllByRoom_Id(UUID roomId);

    long deleteByRoom_IdAndUser_Id(UUID roomId, UUID userId);
}
