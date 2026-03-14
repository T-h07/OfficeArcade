export type RoomStatus = "OPEN" | "FULL" | "CLOSED";
export type RoomMemberRole = "HOST" | "MEMBER";
export type LobbyRealtimeEventType = "ROOM_CREATED" | "ROOM_JOINED" | "ROOM_LEFT" | "ROOM_CLOSED";
export type LobbyRealtimeConnectionStatus = "offline" | "connecting" | "connected" | "degraded";

export type LobbyGameType = {
  id: string;
  code: string;
  displayName: string;
};

export type LobbyRoomMember = {
  userId: string;
  displayName: string;
  role: RoomMemberRole;
  joinedAt: string;
};

export type LobbyRoomSummary = {
  id: string;
  roomName: string;
  hostUserId: string;
  hostDisplayName: string;
  gameTypeCode: string;
  gameTypeDisplayName: string;
  isPrivate: boolean;
  currentPlayers: number;
  maxPlayers: number;
  rounds: number;
  status: RoomStatus;
  createdAt: string;
  updatedAt: string;
};

export type LobbyRoomDetail = LobbyRoomSummary & {
  members: LobbyRoomMember[];
};

export type LobbyRoomListResponse = {
  rooms: LobbyRoomSummary[];
  total: number;
};

export type MyLobbyRoomResponse = {
  room: LobbyRoomDetail | null;
};

export type LobbyRoomActionResponse = {
  status: string;
  message: string;
  room: LobbyRoomDetail | null;
};

export type CreateLobbyRoomRequest = {
  roomName: string;
  gameTypeCode: string;
  maxPlayers: number;
  rounds: number;
  isPrivate: boolean;
  password?: string;
};

export type JoinLobbyRoomRequest = {
  password?: string;
};

export type LobbyRealtimeEvent = {
  eventType: LobbyRealtimeEventType;
  roomId: string;
  triggeredByUserId: string;
  occurredAt: string;
};
