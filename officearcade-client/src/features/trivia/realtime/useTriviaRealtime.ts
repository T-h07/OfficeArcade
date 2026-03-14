import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import { useEffect, useState } from "react";
import { appConfig } from "../../../lib/config";
import type { TriviaRealtimeConnectionStatus, TriviaRealtimeEvent } from "../types/trivia.types";

type UseTriviaRealtimeParams = {
  accessToken: string | null;
  roomId: string | null;
  onEvent: (event: TriviaRealtimeEvent) => void;
};

function parseRealtimeEvent(message: IMessage): TriviaRealtimeEvent | null {
  try {
    return JSON.parse(message.body) as TriviaRealtimeEvent;
  } catch {
    return null;
  }
}

export function useTriviaRealtime({
  accessToken,
  roomId,
  onEvent
}: UseTriviaRealtimeParams): TriviaRealtimeConnectionStatus {
  const [connectionStatus, setConnectionStatus] = useState<TriviaRealtimeConnectionStatus>("offline");

  useEffect(() => {
    if (!accessToken || !roomId) {
      setConnectionStatus("offline");
      return;
    }

    setConnectionStatus("connecting");
    let subscription: StompSubscription | undefined;

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
      subscription = client.subscribe(`/topic/games/trivia/${roomId}`, (message) => {
        const event = parseRealtimeEvent(message);
        if (!event) {
          return;
        }
        onEvent(event);
      });
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
      if (subscription) {
        subscription.unsubscribe();
      }
      client.deactivate();
      setConnectionStatus("offline");
    };
  }, [accessToken, roomId, onEvent]);

  return connectionStatus;
}
