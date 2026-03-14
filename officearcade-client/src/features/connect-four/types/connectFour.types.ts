export type ConnectFourGameStatus = "WAITING" | "ACTIVE" | "FINISHED";
export type ConnectFourRealtimeConnectionStatus = "offline" | "connecting" | "connected" | "degraded";

export type ConnectFourRealtimeEventType = "GAME_STARTED" | "MOVE_PLAYED" | "GAME_FINISHED" | "GAME_ABORTED";

export type ConnectFourPlayer = {
  userId: string;
  displayName: string;
};

export type ConnectFourGameState = {
  roomId: string;
  gameSessionId: string | null;
  status: ConnectFourGameStatus;
  rows: number;
  columns: number;
  board: number[][];
  players: ConnectFourPlayer[];
  playerOneUserId: string | null;
  playerTwoUserId: string | null;
  currentTurnUserId: string | null;
  winnerUserId: string | null;
  draw: boolean;
  moveCount: number;
  canStart: boolean;
  canMove: boolean;
  myTurn: boolean;
  startedAt: string | null;
  endedAt: string | null;
  updatedAt: string;
};

export type ConnectFourMoveRequest = {
  column: number;
};

export type ConnectFourRealtimeEvent = {
  eventType: ConnectFourRealtimeEventType;
  roomId: string;
  gameSessionId: string | null;
  triggeredByUserId: string | null;
  occurredAt: string;
};
