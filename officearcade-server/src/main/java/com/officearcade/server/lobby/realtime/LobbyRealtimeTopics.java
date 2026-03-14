package com.officearcade.server.lobby.realtime;

public final class LobbyRealtimeTopics {

    public static final String LOBBY_TOPIC = "/topic/lobby";
    private static final String ROOM_TOPIC_PREFIX = "/topic/rooms/";

    private LobbyRealtimeTopics() {
    }

    public static String roomTopic(String roomId) {
        return ROOM_TOPIC_PREFIX + roomId;
    }
}
