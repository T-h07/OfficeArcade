export type TriviaGameStatus = "WAITING" | "ACTIVE" | "FINISHED";
export type TriviaRealtimeConnectionStatus = "offline" | "connecting" | "connected" | "degraded";

export type TriviaRealtimeEventType =
  | "GAME_STARTED"
  | "ANSWER_SUBMITTED"
  | "ROUND_RESOLVED"
  | "GAME_FINISHED"
  | "GAME_ABORTED";

export type TriviaAnswerOption = {
  index: number;
  label: string;
};

export type TriviaQuestion = {
  questionId: string;
  prompt: string;
  category: string;
  difficulty: string;
  options: TriviaAnswerOption[];
};

export type TriviaPlayerScore = {
  userId: string;
  displayName: string;
  score: number;
  answeredCurrentRound: boolean;
};

export type TriviaPlayerRoundAnswer = {
  userId: string;
  displayName: string;
  selectedOptionIndex: number;
  correct: boolean;
};

export type TriviaRoundOutcome = {
  roundNumber: number;
  questionId: string;
  questionPrompt: string;
  correctOptionIndex: number;
  playerAnswers: TriviaPlayerRoundAnswer[];
  resolvedAt: string;
};

export type TriviaGameState = {
  roomId: string;
  gameSessionId: string | null;
  status: TriviaGameStatus;
  currentRound: number;
  totalRounds: number;
  currentQuestion: TriviaQuestion | null;
  players: TriviaPlayerScore[];
  submittedUserIds: string[];
  winnerUserId: string | null;
  draw: boolean;
  canStart: boolean;
  canAnswer: boolean;
  answeredByCurrentUser: boolean;
  waitingForOpponent: boolean;
  lastRoundOutcome: TriviaRoundOutcome | null;
  startedAt: string | null;
  endedAt: string | null;
  updatedAt: string;
};

export type TriviaAnswerRequest = {
  selectedOptionIndex: number;
};

export type TriviaRealtimeEvent = {
  eventType: TriviaRealtimeEventType;
  roomId: string;
  gameSessionId: string | null;
  triggeredByUserId: string | null;
  occurredAt: string;
};
