package com.officearcade.server.lobby.realtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class LobbyRealtimePublisherTest {

    @Test
    void shouldPublishLobbyAndRoomTopics() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        LobbyRealtimePublisher publisher = new LobbyRealtimePublisher(messagingTemplate);

        UUID roomId = UUID.randomUUID();
        publisher.publishLobbyAndRoom(LobbyRealtimeEventType.ROOM_JOINED, roomId, "user-1");

        verify(messagingTemplate, times(1)).convertAndSend(eq(LobbyRealtimeTopics.LOBBY_TOPIC), any(LobbyRealtimeEvent.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq(LobbyRealtimeTopics.roomTopic(roomId.toString())), any(LobbyRealtimeEvent.class));
    }
}
