import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import { useEffect, useState } from "react";
import { appConfig } from "../../../lib/config";
import type { UnoRealtimeConnectionStatus, UnoRealtimeEvent } from "../types/uno.types";

type UseUnoRealtimeParams = {
  accessToken: string | null;
  roomId: string | null;
  onEvent: (event: UnoRealtimeEvent) => void;
};

function parseRealtimeEvent(message: IMessage): UnoRealtimeEvent | null {
  try {
    return JSON.parse(message.body) as UnoRealtimeEvent;
  } catch {
    return null;
  }
}

export function useUnoRealtime({ accessToken, roomId, onEvent }: UseUnoRealtimeParams): UnoRealtimeConnectionStatus {
  const [connectionStatus, setConnectionStatus] = useState<UnoRealtimeConnectionStatus>("offline");

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
      subscription = client.subscribe(`/topic/games/uno/${roomId}`, (message) => {
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
