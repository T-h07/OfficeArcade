export type UnoGameStatus = "WAITING" | "ACTIVE" | "FINISHED";
export type UnoRealtimeConnectionStatus = "offline" | "connecting" | "connected" | "degraded";

export type UnoRealtimeEventType =
  | "GAME_STARTED"
  | "CARD_PLAYED"
  | "CARD_DRAWN"
  | "GAME_FINISHED"
  | "GAME_ABORTED";

export type UnoCardColor = "RED" | "YELLOW" | "GREEN" | "BLUE";
export type UnoCardType = "NUMBER" | "SKIP" | "REVERSE" | "DRAW_TWO";

export type UnoCard = {
  token: string;
  color: UnoCardColor;
  type: UnoCardType;
  label: string;
};

export type UnoPlayer = {
  userId: string;
  displayName: string;
  handCount: number;
  currentTurn: boolean;
};

export type UnoGameState = {
  roomId: string;
  gameSessionId: string | null;
  status: UnoGameStatus;
  players: UnoPlayer[];
  currentTurnUserId: string | null;
  winnerUserId: string | null;
  direction: "CLOCKWISE" | "COUNTERCLOCKWISE";
  currentColor: UnoCardColor | null;
  topDiscardCard: UnoCard | null;
  myHand: UnoCard[];
  playableCardTokens: string[];
  drawPileCount: number;
  discardPileCount: number;
  moveCount: number;
  canStart: boolean;
  canPlay: boolean;
  canDraw: boolean;
  myTurn: boolean;
  startedAt: string | null;
  endedAt: string | null;
  updatedAt: string;
};

export type UnoPlayCardRequest = {
  cardToken: string;
};

export type UnoRealtimeEvent = {
  eventType: UnoRealtimeEventType;
  roomId: string;
  gameSessionId: string | null;
  triggeredByUserId: string | null;
  occurredAt: string;
};
