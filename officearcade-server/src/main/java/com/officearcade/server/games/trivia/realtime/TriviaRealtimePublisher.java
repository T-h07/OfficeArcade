package com.officearcade.server.games.trivia.realtime;

import java.time.Instant;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class TriviaRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public TriviaRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishGameEvent(
            TriviaRealtimeEventType eventType,
            UUID roomId,
            UUID gameSessionId,
            String triggeredByUserId
    ) {
        Runnable publishAction = () -> {
            TriviaRealtimeEvent event = new TriviaRealtimeEvent(
                    eventType.name(),
                    roomId.toString(),
                    gameSessionId == null ? null : gameSessionId.toString(),
                    triggeredByUserId,
                    Instant.now().toString()
            );
            messagingTemplate.convertAndSend(TriviaRealtimeTopics.roomGameTopic(roomId.toString()), event);
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
