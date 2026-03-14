package com.officearcade.server.notifications;

import com.officearcade.server.notifications.dto.NotificationListResponse;
import com.officearcade.server.notifications.dto.NotificationReadAllResponse;
import com.officearcade.server.notifications.dto.NotificationResponse;
import com.officearcade.server.notifications.dto.NotificationUnreadCountResponse;
import com.officearcade.server.notifications.persistence.NotificationEntity;
import com.officearcade.server.notifications.persistence.NotificationEntityRepository;
import com.officearcade.server.notifications.realtime.NotificationRealtimeEventType;
import com.officearcade.server.notifications.realtime.NotificationRealtimePublisher;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private final NotificationEntityRepository notificationEntityRepository;
    private final NotificationRealtimePublisher notificationRealtimePublisher;

    public NotificationService(
            NotificationEntityRepository notificationEntityRepository,
            NotificationRealtimePublisher notificationRealtimePublisher
    ) {
        this.notificationEntityRepository = notificationEntityRepository;
        this.notificationRealtimePublisher = notificationRealtimePublisher;
    }

    @Transactional(readOnly = true)
    public NotificationListResponse listNotificationsForUser(String userIdText, Integer limitValue) {
        UUID userId = parseUuid(userIdText, HttpStatus.UNAUTHORIZED, "Invalid authenticated user context.");
        int limit = resolveLimit(limitValue);

        List<NotificationResponse> notifications = notificationEntityRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit)).stream()
                .map(this::toResponse)
                .toList();

        int total = (int) notificationEntityRepository.countByUserId(userId);
        int unreadCount = (int) notificationEntityRepository.countByUserIdAndReadAtIsNull(userId);
        return new NotificationListResponse(total, unreadCount, notifications);
    }

    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse getUnreadCountForUser(String userIdText) {
        UUID userId = parseUuid(userIdText, HttpStatus.UNAUTHORIZED, "Invalid authenticated user context.");
        int unreadCount = (int) notificationEntityRepository.countByUserIdAndReadAtIsNull(userId);
        return new NotificationUnreadCountResponse(unreadCount);
    }

    @Transactional
    public NotificationResponse markNotificationAsRead(String userIdText, String notificationIdText) {
        UUID userId = parseUuid(userIdText, HttpStatus.UNAUTHORIZED, "Invalid authenticated user context.");
        UUID notificationId = parseUuid(
                notificationIdText,
                HttpStatus.BAD_REQUEST,
                "Notification id must be a valid UUID."
        );

        NotificationEntity notification = notificationEntityRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification not found: " + notificationIdText
                ));

        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notificationEntityRepository.save(notification);
        }

        int unreadCount = (int) notificationEntityRepository.countByUserIdAndReadAtIsNull(userId);
        notificationRealtimePublisher.publishNotificationEvent(
                NotificationRealtimeEventType.READ,
                userId,
                notification.getId(),
                unreadCount
        );

        return toResponse(notification);
    }

    @Transactional
    public NotificationReadAllResponse markAllNotificationsAsRead(String userIdText) {
        UUID userId = parseUuid(userIdText, HttpStatus.UNAUTHORIZED, "Invalid authenticated user context.");
        Instant readAt = Instant.now();
        int markedCount = notificationEntityRepository.markAllReadForUser(userId, readAt);

        int unreadCount = (int) notificationEntityRepository.countByUserIdAndReadAtIsNull(userId);
        notificationRealtimePublisher.publishNotificationEvent(
                NotificationRealtimeEventType.READ_ALL,
                userId,
                null,
                unreadCount
        );

        String message = markedCount == 0
                ? "No unread notifications to update."
                : "All notifications marked as read.";
        return new NotificationReadAllResponse("OK", message, markedCount);
    }

    public void safeCreateNotification(NotificationCreateCommand command) {
        try {
            createNotification(command);
        } catch (Exception ex) {
            LOGGER.warn(
                    "Unable to create notification. userId={}, type={}, message={}",
                    command.userId(),
                    command.type(),
                    ex.getMessage()
            );
        }
    }

    public void safeCreateNotifications(List<NotificationCreateCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        for (NotificationCreateCommand command : commands) {
            if (command == null) {
                continue;
            }
            safeCreateNotification(command);
        }
    }

    private Optional<NotificationEntity> createNotification(NotificationCreateCommand command) {
        if (command == null || command.userId() == null || command.type() == null) {
            return Optional.empty();
        }

        String title = normalizeRequiredText(command.title(), 140, "Notification title is required.");
        String message = normalizeRequiredText(command.message(), 320, "Notification message is required.");
        String eventKey = normalizeOptionalText(command.eventKey(), 160);

        if (eventKey != null && notificationEntityRepository.existsByUserIdAndEventKey(command.userId(), eventKey)) {
            return Optional.empty();
        }

        NotificationEntity notification = new NotificationEntity();
        notification.setUserId(command.userId());
        notification.setType(command.type());
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNavigationPath(normalizeOptionalText(command.navigationPath(), 200));
        notification.setSourceRoomId(command.sourceRoomId());
        notification.setSourceGameSessionId(command.sourceGameSessionId());
        notification.setSourceChallengeId(command.sourceChallengeId());
        notification.setSourceStoreItemId(command.sourceStoreItemId());
        notification.setSourceReportId(command.sourceReportId());
        notification.setEventKey(eventKey);
        notification.setReadAt(null);

        NotificationEntity saved = notificationEntityRepository.save(notification);
        int unreadCount = (int) notificationEntityRepository.countByUserIdAndReadAtIsNull(saved.getUserId());
        notificationRealtimePublisher.publishNotificationEvent(
                NotificationRealtimeEventType.CREATED,
                saved.getUserId(),
                saved.getId(),
                unreadCount
        );
        return Optional.of(saved);
    }

    private static int resolveLimit(Integer limitValue) {
        if (limitValue == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limitValue, MAX_LIMIT));
    }

    private static UUID parseUuid(String value, HttpStatus status, String message) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(status, message);
        }
    }

    private static String normalizeRequiredText(String value, int maxLength, String errorMessage) {
        String normalized = normalizeOptionalText(value, maxLength);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            return normalized.substring(0, maxLength);
        }
        return normalized;
    }

    private NotificationResponse toResponse(NotificationEntity notification) {
        return new NotificationResponse(
                notification.getId().toString(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReadAt() == null,
                notification.getNavigationPath(),
                nullableUuid(notification.getSourceRoomId()),
                nullableUuid(notification.getSourceGameSessionId()),
                nullableUuid(notification.getSourceChallengeId()),
                nullableUuid(notification.getSourceStoreItemId()),
                nullableUuid(notification.getSourceReportId()),
                notification.getCreatedAt().toString(),
                nullableInstant(notification.getReadAt())
        );
    }

    private static String nullableUuid(UUID value) {
        return value == null ? null : value.toString();
    }

    private static String nullableInstant(Instant value) {
        return value == null ? null : value.toString();
    }
}
