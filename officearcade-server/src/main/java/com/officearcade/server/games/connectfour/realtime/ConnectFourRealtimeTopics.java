package com.officearcade.server.games.connectfour.realtime;

public final class ConnectFourRealtimeTopics {

    private static final String ROOM_GAME_TOPIC_PREFIX = "/topic/games/connect-four/";

    private ConnectFourRealtimeTopics() {
    }

    public static String roomGameTopic(String roomId) {
        return ROOM_GAME_TOPIC_PREFIX + roomId;
    }
}
