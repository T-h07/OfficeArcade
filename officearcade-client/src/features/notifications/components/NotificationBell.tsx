import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  getMyUnreadNotificationCount,
  listMyNotifications,
  markAllNotificationsAsRead,
  markNotificationAsRead,
  NotificationsApiError
} from "../api/notificationsApi";
import { useNotificationRealtime } from "../realtime/useNotificationRealtime";
import type { UserNotification } from "../types/notifications.types";

type NotificationBellProps = {
  accessToken: string | null;
  userId: string | null;
  onUnauthorized: () => void;
};

function formatRelativeTime(value: string) {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  const deltaSeconds = Math.floor((Date.now() - parsed.getTime()) / 1000);
  if (deltaSeconds < 60) {
    return `${Math.max(deltaSeconds, 0)}s ago`;
  }
  const deltaMinutes = Math.floor(deltaSeconds / 60);
  if (deltaMinutes < 60) {
    return `${deltaMinutes}m ago`;
  }
  const deltaHours = Math.floor(deltaMinutes / 60);
  if (deltaHours < 24) {
    return `${deltaHours}h ago`;
  }
  const deltaDays = Math.floor(deltaHours / 24);
  if (deltaDays < 7) {
    return `${deltaDays}d ago`;
  }
  return parsed.toLocaleDateString();
}

function typeBadge(type: UserNotification["type"]) {
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

function typeLabel(type: UserNotification["type"]) {
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
    return "Respect";
  }
  if (type === "KARMA_APPLIED") {
    return "Karma";
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
    return "Report";
  }
  return "Game Result";
}

export function NotificationBell({ accessToken, userId, onUnauthorized }: NotificationBellProps) {
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState<UserNotification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isLoadingList, setIsLoadingList] = useState(false);
  const [isMutating, setIsMutating] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const loadUnreadCount = useCallback(async () => {
    if (!accessToken) {
      setUnreadCount(0);
      return;
    }

    try {
      const response = await getMyUnreadNotificationCount(accessToken);
      setUnreadCount(response.unreadCount);
    } catch (error) {
      if (error instanceof NotificationsApiError && error.status === 401) {
        onUnauthorized();
      }
    }
  }, [accessToken, onUnauthorized]);

  const loadRecentNotifications = useCallback(async () => {
    if (!accessToken) {
      setNotifications([]);
      return;
    }

    setIsLoadingList(true);
    setErrorMessage(null);
    try {
      const response = await listMyNotifications(accessToken, 8);
      setNotifications(response.notifications);
      setUnreadCount(response.unreadCount);
    } catch (error) {
      if (error instanceof NotificationsApiError && error.status === 401) {
        onUnauthorized();
        return;
      }
      setErrorMessage(error instanceof Error ? error.message : "Unable to load notifications.");
    } finally {
      setIsLoadingList(false);
    }
  }, [accessToken, onUnauthorized]);

  useEffect(() => {
    void loadUnreadCount();
  }, [loadUnreadCount]);

  useEffect(() => {
    if (isOpen) {
      void loadRecentNotifications();
    }
  }, [isOpen, loadRecentNotifications]);

  useNotificationRealtime({
    accessToken,
    userId,
    onEvent: (event) => {
      setUnreadCount(event.unreadCount);
      if (isOpen) {
        void loadRecentNotifications();
      }
    }
  });

  const unreadNotifications = useMemo(() => notifications.filter((notification) => notification.unread).length, [notifications]);

  async function handleMarkRead(notificationId: string) {
    if (!accessToken) {
      return;
    }
    setIsMutating(true);
    setErrorMessage(null);
    try {
      const updated = await markNotificationAsRead(accessToken, notificationId);
      setNotifications((previous) => previous.map((item) => (item.id === updated.id ? updated : item)));
      setUnreadCount((previous) => Math.max(previous - 1, 0));
    } catch (error) {
      if (error instanceof NotificationsApiError && error.status === 401) {
        onUnauthorized();
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
        onUnauthorized();
        return;
      }
      setErrorMessage(error instanceof Error ? error.message : "Unable to mark all notifications as read.");
    } finally {
      setIsMutating(false);
    }
  }

  function handleOpenNotification(notification: UserNotification) {
    if (notification.unread) {
      void handleMarkRead(notification.id);
    }
    if (notification.navigationPath) {
      navigate(notification.navigationPath);
      setIsOpen(false);
    }
  }

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setIsOpen((previous) => !previous)}
        className="relative rounded-lg border border-oa-border bg-oa-surface px-3 py-2 text-sm text-oa-text transition-colors hover:border-oa-accent/45"
        aria-label="Open notifications"
      >
        <span className="sr-only">Notifications</span>
        <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
          <path
            d="M9.5 18h5M4 17.5h16c-1.1-1.2-1.8-2.7-1.8-4.4V10a6.2 6.2 0 1 0-12.4 0v3.1c0 1.7-.7 3.2-1.8 4.4Z"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
        {unreadCount > 0 ? (
          <span className="absolute -right-1 -top-1 min-w-5 rounded-full border border-oa-danger/50 bg-oa-danger px-1.5 py-0.5 text-[10px] font-semibold leading-none text-white">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        ) : null}
      </button>

      {isOpen ? (
        <section className="absolute right-0 z-50 mt-2 w-[min(92vw,360px)] rounded-xl border border-oa-border bg-oa-surface/95 p-3 shadow-glow">
          <div className="flex items-center justify-between gap-2">
            <h3 className="text-sm font-semibold text-oa-text">Notifications</h3>
            <div className="flex items-center gap-2">
              <span className="rounded-full border border-oa-border bg-black/20 px-2 py-0.5 text-[11px] text-oa-text">
                Unread: {unreadCount}
              </span>
              <button
                type="button"
                onClick={() => {
                  void handleMarkAllRead();
                }}
                className="rounded-md border border-oa-border bg-black/20 px-2 py-1 text-[11px] text-oa-text transition-colors hover:border-oa-accent/45 disabled:cursor-not-allowed disabled:opacity-65"
                disabled={isMutating || unreadNotifications === 0}
              >
                Mark all read
              </button>
            </div>
          </div>

          {errorMessage ? (
            <p className="mt-2 rounded-lg border border-oa-danger/45 bg-oa-danger/10 px-2.5 py-2 text-xs text-oa-danger">
              {errorMessage}
            </p>
          ) : null}

          <div className="mt-2 max-h-80 space-y-2 overflow-y-auto pr-1">
            {isLoadingList ? (
              <p className="rounded-lg border border-oa-border bg-black/20 px-3 py-3 text-xs text-oa-muted">
                Loading notifications...
              </p>
            ) : notifications.length === 0 ? (
              <p className="rounded-lg border border-oa-border bg-black/20 px-3 py-3 text-xs text-oa-muted">
                No notifications yet.
              </p>
            ) : (
              notifications.map((notification) => (
                <article
                  key={notification.id}
                  className={`rounded-lg border px-3 py-2 ${
                    notification.unread
                      ? "border-oa-accent/45 bg-oa-accent/10"
                      : "border-oa-border bg-black/20"
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <button
                      type="button"
                      onClick={() => handleOpenNotification(notification)}
                      className="cursor-pointer text-left"
                    >
                      <p className="text-sm font-medium text-oa-text">{notification.title}</p>
                      <p className="mt-0.5 text-xs text-oa-muted">{notification.message}</p>
                    </button>
                    {notification.unread ? (
                      <button
                        type="button"
                        onClick={() => {
                          void handleMarkRead(notification.id);
                        }}
                        className="rounded-md border border-oa-border bg-black/20 px-2 py-1 text-[11px] text-oa-text transition-colors hover:border-oa-accent/45 disabled:cursor-not-allowed disabled:opacity-65"
                        disabled={isMutating}
                      >
                        Read
                      </button>
                    ) : null}
                  </div>
                  <div className="mt-1 flex items-center justify-between">
                    <span className={`rounded-full border px-2 py-0.5 text-[10px] ${typeBadge(notification.type)}`}>
                      {typeLabel(notification.type)}
                    </span>
                    <span className="text-[10px] text-oa-muted">{formatRelativeTime(notification.createdAt)}</span>
                  </div>
                </article>
              ))
            )}
          </div>

          <div className="mt-3 border-t border-oa-border pt-2 text-right">
            <Link
              to="/app/notifications"
              className="rounded-md border border-oa-border bg-black/20 px-2.5 py-1 text-xs text-oa-text transition-colors hover:border-oa-accent/45"
              onClick={() => setIsOpen(false)}
            >
              Open Notifications Center
            </Link>
          </div>
        </section>
      ) : null}
    </div>
  );
}
