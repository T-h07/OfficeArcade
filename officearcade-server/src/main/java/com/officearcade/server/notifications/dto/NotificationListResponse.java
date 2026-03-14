package com.officearcade.server.notifications.dto;

import java.util.List;

public record NotificationListResponse(
        int total,
        int unreadCount,
        List<NotificationResponse> notifications
) {
}
