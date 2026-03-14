package com.officearcade.server.notifications;

import java.util.UUID;

public record NotificationCreateCommand(
        UUID userId,
        NotificationType type,
        String title,
        String message,
        String navigationPath,
        UUID sourceRoomId,
        UUID sourceGameSessionId,
        UUID sourceChallengeId,
        UUID sourceStoreItemId,
        UUID sourceReportId,
        String eventKey
) {
}
