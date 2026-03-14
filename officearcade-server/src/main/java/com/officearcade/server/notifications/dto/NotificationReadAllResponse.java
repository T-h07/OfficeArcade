package com.officearcade.server.notifications.dto;

public record NotificationReadAllResponse(
        String status,
        String message,
        int markedCount
) {
}
