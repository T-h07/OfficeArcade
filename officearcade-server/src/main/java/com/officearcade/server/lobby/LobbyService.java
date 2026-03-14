package com.officearcade.server.lobby;

import com.officearcade.server.catalog.persistence.GameTypeEntity;
import com.officearcade.server.catalog.persistence.GameTypeEntityRepository;
import com.officearcade.server.lobby.dto.CreateLobbyRoomRequest;
import com.officearcade.server.lobby.dto.JoinLobbyRoomRequest;
import com.officearcade.server.lobby.dto.LobbyGameTypeResponse;
import com.officearcade.server.lobby.dto.LobbyRoomActionResponse;
import com.officearcade.server.lobby.dto.LobbyRoomDetailResponse;
import com.officearcade.server.lobby.dto.LobbyRoomListResponse;
import com.officearcade.server.lobby.dto.LobbyRoomMemberResponse;
import com.officearcade.server.lobby.dto.LobbyRoomSummaryResponse;
import com.officearcade.server.lobby.dto.MyLobbyRoomResponse;
import com.officearcade.server.lobby.persistence.RoomEntity;
import com.officearcade.server.lobby.persistence.RoomEntityRepository;
import com.officearcade.server.lobby.persistence.RoomMemberEntity;
import com.officearcade.server.lobby.persistence.RoomMemberEntityRepository;
import com.officearcade.server.users.persistence.UserEntity;
import com.officearcade.server.users.persistence.UserEntityRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LobbyService {

    private final RoomEntityRepository roomEntityRepository;
    private final RoomMemberEntityRepository roomMemberEntityRepository;
    private final GameTypeEntityRepository gameTypeEntityRepository;
    private final UserEntityRepository userEntityRepository;
    private final PasswordEncoder passwordEncoder;

    public LobbyService(
            RoomEntityRepository roomEntityRepository,
            RoomMemberEntityRepository roomMemberEntityRepository,
            GameTypeEntityRepository gameTypeEntityRepository,
            UserEntityRepository userEntityRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.roomEntityRepository = roomEntityRepository;
        this.roomMemberEntityRepository = roomMemberEntityRepository;
        this.gameTypeEntityRepository = gameTypeEntityRepository;
        this.userEntityRepository = userEntityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<LobbyGameTypeResponse> listEnabledGameTypes() {
        return gameTypeEntityRepository.findAllByEnabledTrueOrderByDisplayNameAsc().stream()
                .map(gameType -> new LobbyGameTypeResponse(
                        gameType.getId().toString(),
                        gameType.getCode(),
                        gameType.getDisplayName()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public LobbyRoomListResponse listRooms(String gameTypeCode, Boolean isPrivate, RoomStatus status) {
        String normalizedGameTypeCode = normalizeOptionalGameTypeCode(gameTypeCode);

        Specification<RoomEntity> specification = Specification.where(null);
        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        } else {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.notEqual(root.get("status"), RoomStatus.CLOSED));
        }
        if (isPrivate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("privateRoom"), isPrivate));
        }
        if (normalizedGameTypeCode != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(
                            criteriaBuilder.lower(root.join("gameType").get("code")),
                            normalizedGameTypeCode.toLowerCase(Locale.ROOT)
                    ));
        }

        List<RoomEntity> rooms = roomEntityRepository.findAll(
                specification,
                Sort.by(Sort.Order.desc("createdAt"))
        );

        Map<UUID, Integer> memberCountByRoomId = countMembersByRoomId(rooms);

        List<LobbyRoomSummaryResponse> mapped = rooms.stream()
                .map(room -> toSummary(room, memberCountByRoomId.getOrDefault(room.getId(), 0)))
                .toList();

        return new LobbyRoomListResponse(mapped, mapped.size());
    }

    @Transactional(readOnly = true)
    public LobbyRoomDetailResponse getRoomDetails(String roomId) {
        UUID roomUuid = parseRoomId(roomId);
        RoomEntity room = getRequiredRoom(roomUuid);
        List<RoomMemberEntity> members = roomMemberEntityRepository.findAllByRoom_IdOrderByJoinedAtAsc(roomUuid);
        return toDetail(room, members);
    }

    @Transactional(readOnly = true)
    public MyLobbyRoomResponse getMyRoom(String currentUserId) {
        UUID userId = parseUserId(currentUserId);
        Optional<RoomMemberEntity> membership = roomMemberEntityRepository.findByUser_Id(userId);
        if (membership.isEmpty()) {
            return new MyLobbyRoomResponse(null);
        }
        return new MyLobbyRoomResponse(getRoomDetails(membership.get().getRoom().getId().toString()));
    }

    @Transactional
    public LobbyRoomDetailResponse createRoom(String currentUserId, CreateLobbyRoomRequest request) {
        UUID userId = parseUserId(currentUserId);
        assertUserNotAlreadyInRoom(userId, null);

        UserEntity hostUser = getRequiredUser(userId);
        GameTypeEntity gameType = resolveEnabledGameType(request.gameTypeCode());
        String normalizedRoomName = normalizeRoomName(request.roomName());
        boolean privateRoom = request.isPrivate();
        String passwordHash = resolveRoomPasswordHash(privateRoom, request.password());

        RoomEntity room = new RoomEntity();
        room.setHostUser(hostUser);
        room.setGameType(gameType);
        room.setRoomName(normalizedRoomName);
        room.setPrivateRoom(privateRoom);
        room.setPasswordHash(passwordHash);
        room.setMaxPlayers(request.maxPlayers());
        room.setRounds(request.rounds());
        room.setStatus(RoomStatus.OPEN);

        RoomEntity savedRoom = roomEntityRepository.save(room);

        RoomMemberEntity hostMembership = new RoomMemberEntity();
        hostMembership.setRoom(savedRoom);
        hostMembership.setUser(hostUser);
        hostMembership.setMemberRole(RoomMemberRole.HOST);
        roomMemberEntityRepository.save(hostMembership);

        return getRoomDetails(savedRoom.getId().toString());
    }

    @Transactional
    public LobbyRoomDetailResponse joinRoom(String currentUserId, String roomId, JoinLobbyRoomRequest request) {
        UUID userId = parseUserId(currentUserId);
        UUID roomUuid = parseRoomId(roomId);

        RoomEntity room = getRequiredRoom(roomUuid);
        assertRoomCanBeJoined(room);
        assertRoomGameTypeEnabled(room);
        assertUserNotAlreadyInRoom(userId, roomUuid);
        validateJoinPassword(room, request.password());

        long currentMembers = roomMemberEntityRepository.countByRoom_Id(roomUuid);
        if (currentMembers >= room.getMaxPlayers()) {
            updateRoomStatus(room, currentMembers);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is full.");
        }

        UserEntity joiningUser = getRequiredUser(userId);
        RoomMemberEntity membership = new RoomMemberEntity();
        membership.setRoom(room);
        membership.setUser(joiningUser);
        membership.setMemberRole(RoomMemberRole.MEMBER);
        roomMemberEntityRepository.save(membership);

        updateRoomStatus(room, currentMembers + 1);
        return getRoomDetails(roomId);
    }

    @Transactional
    public LobbyRoomActionResponse leaveRoom(String currentUserId, String roomId) {
        UUID userId = parseUserId(currentUserId);
        UUID roomUuid = parseRoomId(roomId);

        RoomMemberEntity membership = roomMemberEntityRepository.findByRoom_IdAndUser_Id(roomUuid, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "You are not a member of this room."
                ));

        RoomEntity room = membership.getRoom();
        if (membership.getMemberRole() == RoomMemberRole.HOST) {
            roomMemberEntityRepository.deleteAllByRoom_Id(roomUuid);
            room.setStatus(RoomStatus.CLOSED);
            RoomEntity saved = roomEntityRepository.save(room);
            LobbyRoomDetailResponse closedRoom = toDetail(saved, List.of());
            return new LobbyRoomActionResponse(
                    "OK",
                    "Host left the room. Room is now closed.",
                    closedRoom
            );
        }

        roomMemberEntityRepository.deleteByRoom_IdAndUser_Id(roomUuid, userId);
        long remainingMembers = roomMemberEntityRepository.countByRoom_Id(roomUuid);
        updateRoomStatus(room, remainingMembers);

        return new LobbyRoomActionResponse(
                "OK",
                "You left the room.",
                getRoomDetails(roomId)
        );
    }

    @Transactional
    public LobbyRoomActionResponse closeRoom(String currentUserId, String roomId) {
        UUID userId = parseUserId(currentUserId);
        UUID roomUuid = parseRoomId(roomId);

        RoomEntity room = getRequiredRoom(roomUuid);
        if (!room.getHostUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the room host can close this room.");
        }

        roomMemberEntityRepository.deleteAllByRoom_Id(roomUuid);
        room.setStatus(RoomStatus.CLOSED);
        RoomEntity saved = roomEntityRepository.save(room);

        LobbyRoomDetailResponse closedRoom = toDetail(saved, List.of());
        return new LobbyRoomActionResponse("OK", "Room closed.", closedRoom);
    }

    private void assertUserNotAlreadyInRoom(UUID userId, UUID targetRoomId) {
        Optional<RoomMemberEntity> membership = roomMemberEntityRepository.findByUser_Id(userId);
        if (membership.isEmpty()) {
            return;
        }

        UUID existingRoomId = membership.get().getRoom().getId();
        if (targetRoomId != null && existingRoomId.equals(targetRoomId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already in this room.");
        }

        throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already in another active room.");
    }

    private void assertRoomCanBeJoined(RoomEntity room) {
        if (room.getStatus() == RoomStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is closed.");
        }
    }

    private void assertRoomGameTypeEnabled(RoomEntity room) {
        if (!room.getGameType().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room game type is disabled.");
        }
    }

    private void validateJoinPassword(RoomEntity room, String rawPassword) {
        if (!room.isPrivateRoom()) {
            return;
        }

        String password = normalizeRequiredPassword(rawPassword, "Private room password is required.");
        if (room.getPasswordHash() == null || !passwordEncoder.matches(password, room.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect room password.");
        }
    }

    private String resolveRoomPasswordHash(boolean isPrivate, String rawPassword) {
        if (!isPrivate) {
            if (rawPassword != null && !rawPassword.trim().isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Public rooms cannot include a password."
                );
            }
            return null;
        }

        String password = normalizeRequiredPassword(rawPassword, "Private rooms require a password.");
        return passwordEncoder.encode(password);
    }

    private static String normalizeRequiredPassword(String rawPassword, String missingMessage) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, missingMessage);
        }
        String normalized = rawPassword.trim();
        if (normalized.length() < 4) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Room password must be at least 4 characters long."
            );
        }
        return normalized;
    }

    private void updateRoomStatus(RoomEntity room, long memberCount) {
        RoomStatus nextStatus;
        if (memberCount <= 0) {
            nextStatus = RoomStatus.CLOSED;
        } else if (memberCount >= room.getMaxPlayers()) {
            nextStatus = RoomStatus.FULL;
        } else {
            nextStatus = RoomStatus.OPEN;
        }

        if (room.getStatus() == nextStatus) {
            return;
        }

        room.setStatus(nextStatus);
        roomEntityRepository.save(room);
    }

    private GameTypeEntity resolveEnabledGameType(String code) {
        String normalized = normalizeRequiredGameTypeCode(code);
        GameTypeEntity gameType = gameTypeEntityRepository.findByCodeIgnoreCase(normalized)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid game type code: " + normalized
                ));

        if (!gameType.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected game type is currently disabled."
            );
        }

        return gameType;
    }

    private RoomEntity getRequiredRoom(UUID roomId) {
        return roomEntityRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found: " + roomId));
    }

    private UserEntity getRequiredUser(UUID userId) {
        return userEntityRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user is no longer available."
                ));
    }

    private Map<UUID, Integer> countMembersByRoomId(List<RoomEntity> rooms) {
        if (rooms.isEmpty()) {
            return Map.of();
        }

        List<UUID> roomIds = rooms.stream().map(RoomEntity::getId).toList();
        List<RoomMemberEntity> memberships = roomMemberEntityRepository.findAllByRoom_IdIn(roomIds);

        Map<UUID, Integer> counts = new HashMap<>();
        for (RoomMemberEntity membership : memberships) {
            UUID roomId = membership.getRoom().getId();
            counts.put(roomId, counts.getOrDefault(roomId, 0) + 1);
        }
        return counts;
    }

    private LobbyRoomSummaryResponse toSummary(RoomEntity room, int currentPlayers) {
        return new LobbyRoomSummaryResponse(
                room.getId().toString(),
                room.getRoomName(),
                room.getHostUser().getId().toString(),
                room.getHostUser().getDisplayName(),
                room.getGameType().getCode(),
                room.getGameType().getDisplayName(),
                room.isPrivateRoom(),
                currentPlayers,
                room.getMaxPlayers(),
                room.getRounds(),
                room.getStatus().name(),
                room.getCreatedAt().toString(),
                room.getUpdatedAt().toString()
        );
    }

    private LobbyRoomDetailResponse toDetail(RoomEntity room, List<RoomMemberEntity> members) {
        List<LobbyRoomMemberResponse> mappedMembers = members.stream()
                .map(member -> new LobbyRoomMemberResponse(
                        member.getUser().getId().toString(),
                        member.getUser().getDisplayName(),
                        member.getMemberRole().name(),
                        member.getJoinedAt().toString()
                ))
                .toList();

        return new LobbyRoomDetailResponse(
                room.getId().toString(),
                room.getRoomName(),
                room.getHostUser().getId().toString(),
                room.getHostUser().getDisplayName(),
                room.getGameType().getCode(),
                room.getGameType().getDisplayName(),
                room.isPrivateRoom(),
                mappedMembers.size(),
                room.getMaxPlayers(),
                room.getRounds(),
                room.getStatus().name(),
                room.getCreatedAt().toString(),
                room.getUpdatedAt().toString(),
                mappedMembers
        );
    }

    private static String normalizeRoomName(String roomName) {
        if (roomName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room name is required.");
        }
        String normalized = roomName.trim();
        if (normalized.length() < 3 || normalized.length() > 80) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Room name must be between 3 and 80 characters."
            );
        }
        return normalized;
    }

    private static String normalizeRequiredGameTypeCode(String gameTypeCode) {
        if (gameTypeCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game type code is required.");
        }
        String normalized = gameTypeCode.trim();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game type code is required.");
        }
        return normalized;
    }

    private static String normalizeOptionalGameTypeCode(String gameTypeCode) {
        if (gameTypeCode == null) {
            return null;
        }
        String normalized = gameTypeCode.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static UUID parseUserId(String userIdText) {
        try {
            return UUID.fromString(userIdText);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authenticated user context.");
        }
    }

    private static UUID parseRoomId(String roomIdText) {
        try {
            return UUID.fromString(roomIdText);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room id must be a valid UUID.");
        }
    }
}
