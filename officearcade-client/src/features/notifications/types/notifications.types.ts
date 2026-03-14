export type NotificationType =
  | "CHALLENGE_CREATED"
  | "CHALLENGE_CONFIRMED"
  | "CHALLENGE_REJECTED"
  | "CHALLENGE_DISPUTE_RESOLVED"
  | "RESPECT_GAINED"
  | "KARMA_APPLIED"
  | "STORE_PURCHASE_SUCCESS"
  | "ITEM_EQUIPPED"
  | "MODERATION_STATUS_UPDATE"
  | "REPORT_STATUS_UPDATE"
  | "GAME_RESULT";

export type UserNotification = {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  unread: boolean;
  navigationPath: string | null;
  sourceRoomId: string | null;
  sourceGameSessionId: string | null;
  sourceChallengeId: string | null;
  sourceStoreItemId: string | null;
  sourceReportId: string | null;
  createdAt: string;
  readAt: string | null;
};

export type NotificationListResponse = {
  total: number;
  unreadCount: number;
  notifications: UserNotification[];
};

export type NotificationUnreadCountResponse = {
  unreadCount: number;
};

export type NotificationReadAllResponse = {
  status: string;
  message: string;
  markedCount: number;
};

export type NotificationRealtimeEventType = "CREATED" | "READ" | "READ_ALL";

export type NotificationRealtimeEvent = {
  eventType: NotificationRealtimeEventType;
  userId: string;
  notificationId: string | null;
  unreadCount: number;
  occurredAt: string;
};
