package com.officearcade.server.notifications.dto;

public record NotificationResponse(
        String id,
        String type,
        String title,
        String message,
        boolean unread,
        String navigationPath,
        String sourceRoomId,
        String sourceGameSessionId,
        String sourceChallengeId,
        String sourceStoreItemId,
        String sourceReportId,
        String createdAt,
        String readAt
) {
}
