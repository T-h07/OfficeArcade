export type RouteAccent =
  | "dashboard"
  | "play"
  | "challenges"
  | "leaderboards"
  | "notifications"
  | "identity"
  | "admin";

export type ShellMode = "player" | "admin";

export type ShellRouteMeta = {
  accent: RouteAccent;
  mode: ShellMode;
  label: string;
  hint: string;
};

const ROUTE_META: Array<{ pathPrefix: string; meta: ShellRouteMeta }> = [
  {
    pathPrefix: "/app/play",
    meta: { accent: "play", mode: "player", label: "Play Arena", hint: "Live room sessions and active games" }
  },
  {
    pathPrefix: "/app/challenges",
    meta: { accent: "challenges", mode: "player", label: "Challenges", hint: "Respect and Karma challenge flow" }
  },
  {
    pathPrefix: "/app/leaderboards",
    meta: { accent: "leaderboards", mode: "player", label: "Leaderboards", hint: "Competitive standings and rank momentum" }
  },
  {
    pathPrefix: "/app/notifications",
    meta: { accent: "notifications", mode: "player", label: "Notifications", hint: "Recent updates and social feedback" }
  },
  {
    pathPrefix: "/app/store",
    meta: { accent: "identity", mode: "player", label: "Store", hint: "Respect rewards and cosmetic unlocks" }
  },
  {
    pathPrefix: "/app/inventory",
    meta: { accent: "identity", mode: "player", label: "Inventory", hint: "Owned cosmetics and active loadout" }
  },
  {
    pathPrefix: "/app/profile",
    meta: { accent: "identity", mode: "player", label: "Profile", hint: "Identity, loadout, and account summary" }
  },
  {
    pathPrefix: "/app/admin",
    meta: { accent: "admin", mode: "admin", label: "Admin Operations", hint: "Company controls and operational views" }
  },
  {
    pathPrefix: "/app/admin-overview",
    meta: { accent: "admin", mode: "admin", label: "Admin Overview", hint: "Administration status and key controls" }
  },
  {
    pathPrefix: "/app/settings",
    meta: { accent: "admin", mode: "admin", label: "Settings", hint: "Environment-level preferences" }
  }
];

const DEFAULT_META: ShellRouteMeta = {
  accent: "dashboard",
  mode: "player",
  label: "Dashboard",
  hint: "Progress, status, and next actions"
};

export function getShellRouteMeta(pathname: string): ShellRouteMeta {
  const matched = ROUTE_META.find((entry) => pathname.startsWith(entry.pathPrefix));
  if (matched) {
    return matched.meta;
  }
  return DEFAULT_META;
}
