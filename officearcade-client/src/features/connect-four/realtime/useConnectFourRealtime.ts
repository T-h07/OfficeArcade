import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import { useEffect, useState } from "react";
import { appConfig } from "../../../lib/config";
import type {
  ConnectFourRealtimeConnectionStatus,
  ConnectFourRealtimeEvent
} from "../types/connectFour.types";

type UseConnectFourRealtimeParams = {
  accessToken: string | null;
  roomId: string | null;
  onEvent: (event: ConnectFourRealtimeEvent) => void;
};

function parseRealtimeEvent(message: IMessage): ConnectFourRealtimeEvent | null {
  try {
    return JSON.parse(message.body) as ConnectFourRealtimeEvent;
  } catch {
    return null;
  }
}

export function useConnectFourRealtime({
  accessToken,
  roomId,
  onEvent
}: UseConnectFourRealtimeParams): ConnectFourRealtimeConnectionStatus {
  const [connectionStatus, setConnectionStatus] = useState<ConnectFourRealtimeConnectionStatus>("offline");

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

      subscription = client.subscribe(`/topic/games/connect-four/${roomId}`, (message) => {
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
