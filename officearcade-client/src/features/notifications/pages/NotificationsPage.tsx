import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";
import {
  listMyNotifications,
  markAllNotificationsAsRead,
  markNotificationAsRead,
  NotificationsApiError
} from "../api/notificationsApi";
import { useNotificationRealtime } from "../realtime/useNotificationRealtime";
import type { UserNotification } from "../types/notifications.types";

type NotificationFilter = "ALL" | "UNREAD" | "READ";

function formatDateTime(value: string) {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleString();
}

function notificationTypeLabel(type: UserNotification["type"]) {
  if (type === "CHALLENGE_CREATED") {
    return "Challenge Created";
  }
  if (type === "CHALLENGE_CONFIRMED") {
    return "Challenge Confirmed";
  }
  if (type === "CHALLENGE_REJECTED") {
    return "Challenge Rejected";
  }
  if (type === "CHALLENGE_DISPUTE_RESOLVED") {
    return "Dispute Resolved";
  }
  if (type === "RESPECT_GAINED") {
    return "Respect Gained";
  }
  if (type === "KARMA_APPLIED") {
    return "Karma Applied";
  }
  if (type === "STORE_PURCHASE_SUCCESS") {
    return "Purchase";
  }
  if (type === "ITEM_EQUIPPED") {
    return "Item Equipped";
  }
  if (type === "MODERATION_STATUS_UPDATE") {
    return "Moderation";
  }
  if (type === "REPORT_STATUS_UPDATE") {
    return "Report Update";
  }
  return "Game Result";
}

function typeBadgeClass(type: UserNotification["type"]) {
  if (type === "RESPECT_GAINED") {
    return "border-oa-accent/45 bg-oa-accent/15 text-oa-text";
  }
  if (type === "KARMA_APPLIED") {
    return "border-oa-danger/45 bg-oa-danger/15 text-oa-danger";
  }
  if (type === "MODERATION_STATUS_UPDATE") {
    return "border-amber-300/45 bg-amber-300/15 text-amber-100";
  }
  return "border-oa-border bg-black/25 text-oa-muted";
}

export function NotificationsPage() {
  const navigate = useNavigate();
  const { accessToken, logout, user } = useAuth();

  const [notifications, setNotifications] = useState<UserNotification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [isMutating, setIsMutating] = useState(false);
  const [filter, setFilter] = useState<NotificationFilter>("ALL");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const loadNotifications = useCallback(async () => {
    if (!accessToken) {
      setNotifications([]);
      setUnreadCount(0);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setErrorMessage(null);
    try {
      const response = await listMyNotifications(accessToken, 100);
      setNotifications(response.notifications);
      setUnreadCount(response.unreadCount);
    } catch (error) {
      if (error instanceof NotificationsApiError && error.status === 401) {
        logout();
        return;
      }
      setErrorMessage(error instanceof Error ? error.message : "Unable to load notifications.");
    } finally {
      setIsLoading(false);
    }
  }, [accessToken, logout]);

  useEffect(() => {
    void loadNotifications();
  }, [loadNotifications]);

  useNotificationRealtime({
    accessToken,
    userId: user?.id ?? null,
    onEvent: (event) => {
      setUnreadCount(event.unreadCount);
      void loadNotifications();
    }
  });

  const visibleNotifications = useMemo(() => {
    if (filter === "UNREAD") {
      return notifications.filter((notification) => notification.unread);
    }
    if (filter === "READ") {
      return notifications.filter((notification) => !notification.unread);
    }
    return notifications;
  }, [filter, notifications]);

  async function handleMarkRead(notification: UserNotification) {
    if (!accessToken || !notification.unread) {
      return;
    }
    setIsMutating(true);
    setErrorMessage(null);
    try {
      const updated = await markNotificationAsRead(accessToken, notification.id);
      setNotifications((previous) => previous.map((item) => (item.id === updated.id ? updated : item)));
      setUnreadCount((previous) => Math.max(previous - 1, 0));
    } catch (error) {
      if (error instanceof NotificationsApiError && error.status === 401) {
        logout();
        return;
      }
      setErrorMessage(error instanceof Error ? error.message : "Unable to mark notification as read.");
    } finally {
      setIsMutating(false);
    }
  }

  async function handleMarkAllRead() {
    if (!accessToken) {
      return;
    }
    setIsMutating(true);
    setErrorMessage(null);
    try {
      await markAllNotificationsAsRead(accessToken);
      setNotifications((previous) =>
        previous.map((item) => ({
          ...item,
          unread: false,
          readAt: item.readAt ?? new Date().toISOString()
        }))
      );
      setUnreadCount(0);
    } catch (error) {
      if (error instanceof NotificationsApiError && error.status === 401) {
        logout();
        return;
      }
      setErrorMessage(error instanceof Error ? error.message : "Unable to mark all notifications as read.");
    } finally {
      setIsMutating(false);
    }
  }

  function handleOpenNotification(notification: UserNotification) {
    if (notification.unread) {
      void handleMarkRead(notification);
    }
    if (notification.navigationPath) {
      navigate(notification.navigationPath);
    }
  }

  return (
    <section className="space-y-5">
      <header className="rounded-2xl border border-oa-border bg-oa-surface/85 p-5">
        <p className="text-xs uppercase tracking-[0.18em] text-oa-muted">Social Feedback</p>
        <h1 className="mt-2 text-2xl font-semibold text-oa-text">Notifications Center</h1>
        <p className="mt-2 text-sm text-oa-muted">
          Track challenge outcomes, social feedback, moderation status updates, and important in-app activity.
        </p>
      </header>

      <section className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-oa-border bg-oa-surface/70 p-4">
        <div className="flex flex-wrap items-center gap-2 text-xs">
          <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-oa-text">
            Total: {notifications.length}
          </span>
          <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-oa-text">
            Unread: {unreadCount}
          </span>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <select
            value={filter}
            onChange={(event) => setFilter(event.target.value as NotificationFilter)}
            className="rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
          >
            <option value="ALL">All</option>
            <option value="UNREAD">Unread</option>
            <option value="READ">Read</option>
          </select>
          <button
            type="button"
            onClick={() => {
              void handleMarkAllRead();
            }}
            className="rounded-lg border border-oa-border bg-black/20 px-3 py-2 text-sm text-oa-text transition-colors hover:border-oa-accent/45 disabled:cursor-not-allowed disabled:opacity-65"
            disabled={isMutating || unreadCount === 0}
          >
            Mark all as read
          </button>
          <button
            type="button"
            onClick={() => {
              void loadNotifications();
            }}
            className="rounded-lg border border-oa-border bg-black/20 px-3 py-2 text-sm text-oa-text transition-colors hover:border-oa-accent/45 disabled:cursor-not-allowed disabled:opacity-65"
            disabled={isLoading}
          >
            {isLoading ? "Refreshing..." : "Refresh"}
          </button>
        </div>
      </section>

      {errorMessage ? (
        <div className="rounded-xl border border-oa-danger/45 bg-oa-danger/10 px-4 py-3 text-sm text-oa-danger">{errorMessage}</div>
      ) : null}

      <section className="rounded-2xl border border-oa-border bg-oa-surface/80 p-4">
        {isLoading ? (
          <p className="rounded-lg border border-oa-border bg-black/20 px-3 py-4 text-sm text-oa-muted">
            Loading notifications...
          </p>
        ) : visibleNotifications.length === 0 ? (
          <p className="rounded-lg border border-oa-border bg-black/20 px-3 py-4 text-sm text-oa-muted">
            No notifications to show for this filter.
          </p>
        ) : (
          <div className="space-y-3">
            {visibleNotifications.map((notification) => (
              <article
                key={notification.id}
                className={`rounded-xl border p-4 ${
                  notification.unread
                    ? "border-oa-accent/45 bg-oa-accent/10"
                    : "border-oa-border bg-black/20"
                }`}
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <button
                    type="button"
                    onClick={() => handleOpenNotification(notification)}
                    className="cursor-pointer text-left"
                  >
                    <h3 className="text-base font-semibold text-oa-text">{notification.title}</h3>
                    <p className="mt-1 text-sm text-oa-muted">{notification.message}</p>
                  </button>
                  {notification.unread ? (
                    <button
                      type="button"
                      onClick={() => {
                        void handleMarkRead(notification);
                      }}
                      className="rounded-md border border-oa-border bg-black/25 px-2.5 py-1 text-xs text-oa-text transition-colors hover:border-oa-accent/45 disabled:cursor-not-allowed disabled:opacity-65"
                      disabled={isMutating}
                    >
                      Mark read
                    </button>
                  ) : (
                    <span className="rounded-md border border-oa-border bg-black/25 px-2.5 py-1 text-xs text-oa-muted">Read</span>
                  )}
                </div>
                <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-xs">
                  <span className={`rounded-full border px-2 py-0.5 ${typeBadgeClass(notification.type)}`}>
                    {notificationTypeLabel(notification.type)}
                  </span>
                  <span className="text-oa-muted">Created: {formatDateTime(notification.createdAt)}</span>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </section>
  );
}
