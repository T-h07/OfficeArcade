package com.officearcade.server.lobby.realtime;

import java.time.Instant;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class LobbyRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public LobbyRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishLobbyAndRoom(LobbyRealtimeEventType eventType, UUID roomId, String triggeredByUserId) {
        Runnable publishAction = () -> {
            LobbyRealtimeEvent event = new LobbyRealtimeEvent(
                    eventType.name(),
                    roomId.toString(),
                    triggeredByUserId,
                    Instant.now().toString()
            );
            messagingTemplate.convertAndSend(LobbyRealtimeTopics.LOBBY_TOPIC, event);
            messagingTemplate.convertAndSend(LobbyRealtimeTopics.roomTopic(roomId.toString()), event);
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
