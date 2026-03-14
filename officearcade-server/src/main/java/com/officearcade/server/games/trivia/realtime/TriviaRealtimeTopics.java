package com.officearcade.server.games.trivia.realtime;

public final class TriviaRealtimeTopics {

    private static final String ROOM_GAME_TOPIC_PREFIX = "/topic/games/trivia/";

    private TriviaRealtimeTopics() {
    }

    public static String roomGameTopic(String roomId) {
        return ROOM_GAME_TOPIC_PREFIX + roomId;
    }
}
