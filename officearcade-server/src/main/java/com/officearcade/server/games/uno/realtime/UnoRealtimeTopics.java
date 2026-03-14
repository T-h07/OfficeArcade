package com.officearcade.server.games.uno.realtime;

public final class UnoRealtimeTopics {

    private static final String ROOM_GAME_TOPIC_PREFIX = "/topic/games/uno/";

    private UnoRealtimeTopics() {
    }

    public static String roomGameTopic(String roomId) {
        return ROOM_GAME_TOPIC_PREFIX + roomId;
    }
}
