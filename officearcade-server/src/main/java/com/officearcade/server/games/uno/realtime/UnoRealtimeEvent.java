package com.officearcade.server.games.uno.realtime;

public record UnoRealtimeEvent(
        String eventType,
        String roomId,
        String gameSessionId,
        String triggeredByUserId,
        String occurredAt
) {
}
