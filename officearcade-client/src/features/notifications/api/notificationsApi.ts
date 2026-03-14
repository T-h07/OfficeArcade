import { appConfig } from "../../../lib/config";
import type {
  NotificationListResponse,
  NotificationReadAllResponse,
  NotificationUnreadCountResponse,
  UserNotification
} from "../types/notifications.types";

export class NotificationsApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "NotificationsApiError";
  }
}

function resolveUrl(path: string) {
  return `${appConfig.apiBaseUrl}${path}`;
}

function authHeaders(token: string): HeadersInit {
  return {
    Accept: "application/json",
    Authorization: `Bearer ${token}`
  };
}

async function parseError(response: Response, fallbackMessage: string): Promise<never> {
  let message = fallbackMessage;

  try {
    const payload = (await response.json()) as Record<string, unknown>;
    if (typeof payload.detail === "string" && payload.detail.trim().length > 0) {
      message = payload.detail;
    } else if (typeof payload.message === "string" && payload.message.trim().length > 0) {
      message = payload.message;
    } else if (typeof payload.error === "string" && payload.error.trim().length > 0) {
      message = payload.error;
    }
  } catch {
    // Keep fallback message.
  }

  throw new NotificationsApiError(response.status, message);
}

export async function listMyNotifications(token: string, limit = 50): Promise<NotificationListResponse> {
  const query = new URLSearchParams();
  query.set("limit", String(Math.max(1, Math.min(limit, 100))));

  const response = await fetch(resolveUrl(`/api/notifications/me?${query.toString()}`), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load notifications.");
  }

  return (await response.json()) as NotificationListResponse;
}

export async function getMyUnreadNotificationCount(token: string): Promise<NotificationUnreadCountResponse> {
  const response = await fetch(resolveUrl("/api/notifications/me/unread-count"), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load unread notification count.");
  }

  return (await response.json()) as NotificationUnreadCountResponse;
}

export async function markNotificationAsRead(token: string, notificationId: string): Promise<UserNotification> {
  const response = await fetch(resolveUrl(`/api/notifications/${notificationId}/read`), {
    method: "POST",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to mark notification as read.");
  }

  return (await response.json()) as UserNotification;
}

export async function markAllNotificationsAsRead(token: string): Promise<NotificationReadAllResponse> {
  const response = await fetch(resolveUrl("/api/notifications/me/read-all"), {
    method: "POST",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to mark all notifications as read.");
  }

  return (await response.json()) as NotificationReadAllResponse;
}
