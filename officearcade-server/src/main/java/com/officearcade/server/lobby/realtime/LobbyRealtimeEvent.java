package com.officearcade.server.lobby.realtime;

public record LobbyRealtimeEvent(
        String eventType,
        String roomId,
        String triggeredByUserId,
        String occurredAt
) {
}
