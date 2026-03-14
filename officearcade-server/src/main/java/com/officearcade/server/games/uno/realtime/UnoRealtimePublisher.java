package com.officearcade.server.games.uno.realtime;

import java.time.Instant;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class UnoRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public UnoRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishGameEvent(
            UnoRealtimeEventType eventType,
            UUID roomId,
            UUID gameSessionId,
            String triggeredByUserId
    ) {
        Runnable publishAction = () -> {
            UnoRealtimeEvent event = new UnoRealtimeEvent(
                    eventType.name(),
                    roomId.toString(),
                    gameSessionId == null ? null : gameSessionId.toString(),
                    triggeredByUserId,
                    Instant.now().toString()
            );
            messagingTemplate.convertAndSend(UnoRealtimeTopics.roomGameTopic(roomId.toString()), event);
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
