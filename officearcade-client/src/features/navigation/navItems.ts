import type { AppRole } from "../auth/auth.types";

export type NavItem = {
  label: string;
  to: string;
  roles: AppRole[];
};

const NAV_ITEMS: NavItem[] = [
  {
    label: "Dashboard",
    to: "/app/dashboard",
    roles: ["ADMIN", "EMPLOYEE"]
  },
  {
    label: "Play",
    to: "/app/play",
    roles: ["ADMIN", "EMPLOYEE"]
  },
  {
    label: "Users",
    to: "/app/admin/users",
    roles: ["ADMIN"]
  },
  {
    label: "Admin Overview",
    to: "/app/admin-overview",
    roles: ["ADMIN"]
  },
  {
    label: "Profile",
    to: "/app/profile",
    roles: ["EMPLOYEE"]
  },
  {
    label: "Settings",
    to: "/app/settings",
    roles: ["ADMIN", "EMPLOYEE"]
  }
];

export function getNavItemsForRole(role: AppRole) {
  return NAV_ITEMS.filter((item) => item.roles.includes(role));
}
