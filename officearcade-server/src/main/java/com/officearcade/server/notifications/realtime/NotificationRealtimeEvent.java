package com.officearcade.server.notifications.realtime;

public record NotificationRealtimeEvent(
        String eventType,
        String userId,
        String notificationId,
        int unreadCount,
        String occurredAt
) {
}
