package com.officearcade.server.games.trivia.realtime;

public record TriviaRealtimeEvent(
        String eventType,
        String roomId,
        String gameSessionId,
        String triggeredByUserId,
        String occurredAt
) {
}
