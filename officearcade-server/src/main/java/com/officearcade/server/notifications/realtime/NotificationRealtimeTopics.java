package com.officearcade.server.notifications.realtime;

public final class NotificationRealtimeTopics {

    private static final String USER_NOTIFICATION_TOPIC_PREFIX = "/topic/notifications/";

    private NotificationRealtimeTopics() {
    }

    public static String userTopic(String userId) {
        return USER_NOTIFICATION_TOPIC_PREFIX + userId;
    }
}
