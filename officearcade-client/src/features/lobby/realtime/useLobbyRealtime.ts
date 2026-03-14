import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import { useEffect, useState } from "react";
import { appConfig } from "../../../lib/config";
import type { LobbyRealtimeConnectionStatus, LobbyRealtimeEvent } from "../types/lobby.types";

type UseLobbyRealtimeParams = {
  accessToken: string | null;
  roomId: string | null;
  onLobbyEvent: (event: LobbyRealtimeEvent) => void;
  onRoomEvent: (event: LobbyRealtimeEvent) => void;
};

function parseRealtimeEvent(message: IMessage): LobbyRealtimeEvent | null {
  try {
    return JSON.parse(message.body) as LobbyRealtimeEvent;
  } catch {
    return null;
  }
}

export function useLobbyRealtime({
  accessToken,
  roomId,
  onLobbyEvent,
  onRoomEvent
}: UseLobbyRealtimeParams): LobbyRealtimeConnectionStatus {
  const [connectionStatus, setConnectionStatus] = useState<LobbyRealtimeConnectionStatus>("offline");

  useEffect(() => {
    if (!accessToken) {
      setConnectionStatus("offline");
      return;
    }

    setConnectionStatus("connecting");
    let lobbySubscription: StompSubscription | undefined;
    let roomSubscription: StompSubscription | undefined;

    const client = new Client({
      brokerURL: appConfig.realtimeWsUrl,
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`
      },
      reconnectDelay: 2500,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000
    });

    client.onConnect = () => {
      setConnectionStatus("connected");

      lobbySubscription = client.subscribe("/topic/lobby", (message) => {
        const event = parseRealtimeEvent(message);
        if (!event) {
          return;
        }
        onLobbyEvent(event);
      });

      if (roomId) {
        roomSubscription = client.subscribe(`/topic/rooms/${roomId}`, (message) => {
          const event = parseRealtimeEvent(message);
          if (!event) {
            return;
          }
          onRoomEvent(event);
        });
      }
    };

    client.onStompError = () => {
      setConnectionStatus("degraded");
    };

    client.onWebSocketError = () => {
      setConnectionStatus("degraded");
    };

    client.onWebSocketClose = () => {
      setConnectionStatus("degraded");
    };

    client.activate();

    return () => {
      if (lobbySubscription) {
        lobbySubscription.unsubscribe();
      }
      if (roomSubscription) {
        roomSubscription.unsubscribe();
      }
      client.deactivate();
      setConnectionStatus("offline");
    };
  }, [accessToken, onLobbyEvent, onRoomEvent, roomId]);

  return connectionStatus;
}
