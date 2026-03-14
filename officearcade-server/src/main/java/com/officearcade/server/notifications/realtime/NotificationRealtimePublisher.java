package com.officearcade.server.notifications.realtime;

import java.time.Instant;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class NotificationRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishNotificationEvent(
            NotificationRealtimeEventType eventType,
            UUID userId,
            UUID notificationId,
            int unreadCount
    ) {
        Runnable publishAction = () -> {
            NotificationRealtimeEvent event = new NotificationRealtimeEvent(
                    eventType.name(),
                    userId.toString(),
                    notificationId == null ? null : notificationId.toString(),
                    Math.max(unreadCount, 0),
                    Instant.now().toString()
            );
            messagingTemplate.convertAndSend(NotificationRealtimeTopics.userTopic(userId.toString()), event);
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishAction.run();
                }
            });
            return;
        }

        publishAction.run();
    }
}
