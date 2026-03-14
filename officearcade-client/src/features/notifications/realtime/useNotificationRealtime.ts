import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import { useEffect, useState } from "react";
import { appConfig } from "../../../lib/config";
import type { NotificationRealtimeEvent } from "../types/notifications.types";

type NotificationRealtimeConnectionStatus = "offline" | "connecting" | "connected" | "degraded";

type UseNotificationRealtimeParams = {
  accessToken: string | null;
  userId: string | null;
  onEvent: (event: NotificationRealtimeEvent) => void;
};

function parseRealtimeEvent(message: IMessage): NotificationRealtimeEvent | null {
  try {
    return JSON.parse(message.body) as NotificationRealtimeEvent;
  } catch {
    return null;
  }
}

export function useNotificationRealtime({
  accessToken,
  userId,
  onEvent
}: UseNotificationRealtimeParams): NotificationRealtimeConnectionStatus {
  const [connectionStatus, setConnectionStatus] = useState<NotificationRealtimeConnectionStatus>("offline");

  useEffect(() => {
    if (!accessToken || !userId) {
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
      subscription = client.subscribe(`/topic/notifications/${userId}`, (message) => {
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
  }, [accessToken, onEvent, userId]);

  return connectionStatus;
}
