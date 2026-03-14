package com.officearcade.server.games.connectfour.realtime;

public record ConnectFourRealtimeEvent(
        String eventType,
        String roomId,
        String gameSessionId,
        String triggeredByUserId,
        String occurredAt
) {
}
