import type { AppRole } from "../auth/auth.types";

export type NavSection = "CORE" | "SOCIAL" | "IDENTITY" | "OPERATIONS" | "SYSTEM";

export type NavItem = {
  label: string;
  to: string;
  roles: AppRole[];
  section: NavSection;
};

const NAV_ITEMS: NavItem[] = [
  {
    label: "Dashboard",
    to: "/app/dashboard",
    roles: ["ADMIN", "EMPLOYEE"],
    section: "CORE"
  },
  {
    label: "Play",
    to: "/app/play",
    roles: ["ADMIN", "EMPLOYEE"],
    section: "CORE"
  },
  {
    label: "Challenges",
    to: "/app/challenges",
    roles: ["ADMIN", "EMPLOYEE"],
    section: "CORE"
  },
  {
    label: "Leaderboards",
    to: "/app/leaderboards",
    roles: ["ADMIN", "EMPLOYEE"],
    section: "SOCIAL"
  },
  {
    label: "Notifications",
    to: "/app/notifications",
    roles: ["ADMIN", "EMPLOYEE"],
    section: "SOCIAL"
  },
  {
    label: "Store",
    to: "/app/store",
    roles: ["ADMIN", "EMPLOYEE"],
    section: "IDENTITY"
  },
  {
    label: "Inventory",
    to: "/app/inventory",
    roles: ["ADMIN", "EMPLOYEE"],
    section: "IDENTITY"
  },
  {
    label: "Analytics",
    to: "/app/admin/analytics",
    roles: ["ADMIN"],
    section: "OPERATIONS"
  },
  {
    label: "Users",
    to: "/app/admin/users",
    roles: ["ADMIN"],
    section: "OPERATIONS"
  },
  {
    label: "Departments",
    to: "/app/admin/departments",
    roles: ["ADMIN"],
    section: "OPERATIONS"
  },
  {
    label: "Moderation",
    to: "/app/admin/moderation",
    roles: ["ADMIN"],
    section: "OPERATIONS"
  },
  {
    label: "Admin Overview",
    to: "/app/admin-overview",
    roles: ["ADMIN"],
    section: "OPERATIONS"
  },
  {
    label: "Profile",
    to: "/app/profile",
    roles: ["EMPLOYEE"],
    section: "IDENTITY"
  },
  {
    label: "Settings",
    to: "/app/settings",
    roles: ["ADMIN", "EMPLOYEE"],
    section: "SYSTEM"
  }
];

export function getNavItemsForRole(role: AppRole) {
  return NAV_ITEMS.filter((item) => item.roles.includes(role));
}
