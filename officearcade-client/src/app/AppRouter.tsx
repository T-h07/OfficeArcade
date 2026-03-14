import { Navigate, Route, Routes } from "react-router-dom";
import { RequireAuth } from "../features/auth/RequireAuth";
import { PublicOnlyRoute } from "../features/auth/PublicOnlyRoute";
import { RequireRole } from "../features/auth/RequireRole";
import { SuspendedAccessGate } from "../features/auth/SuspendedAccessGate";
import { AdminUsersPage } from "../features/admin-users/pages/AdminUsersPage";
import { ChallengesPage } from "../features/challenges/pages/ChallengesPage";
import { AppShell } from "../features/layout/AppShell";
import { LeaderboardsPage } from "../features/leaderboards/pages/LeaderboardsPage";
import { AdminModerationPage } from "../features/moderation/pages/AdminModerationPage";
import { InventoryPage } from "../features/store/pages/InventoryPage";
import { StorePage } from "../features/store/pages/StorePage";
import { AdminOverviewPage } from "../pages/AdminOverviewPage";
import { DashboardPage } from "../pages/DashboardPage";
import { ForbiddenPage } from "../pages/ForbiddenPage";
import { LoginPage } from "../pages/LoginPage";
import { PlayPage } from "../pages/PlayPage";
import { ProfilePage } from "../pages/ProfilePage";
import { SettingsPage } from "../pages/SettingsPage";

export function AppRouter() {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <PublicOnlyRoute>
            <LoginPage />
          </PublicOnlyRoute>
        }
      />

      <Route
        path="/app"
        element={
          <RequireAuth>
            <SuspendedAccessGate>
              <AppShell />
            </SuspendedAccessGate>
          </RequireAuth>
        }
      >
        <Route index element={<Navigate to="/app/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="play" element={<PlayPage />} />
        <Route path="challenges" element={<ChallengesPage />} />
        <Route path="leaderboards" element={<LeaderboardsPage />} />
        <Route path="store" element={<StorePage />} />
        <Route path="inventory" element={<InventoryPage />} />
        <Route
          path="admin-overview"
          element={
            <RequireRole allowedRoles={["ADMIN"]}>
              <AdminOverviewPage />
            </RequireRole>
          }
        />
        <Route
          path="admin/users"
          element={
            <RequireRole allowedRoles={["ADMIN"]}>
              <AdminUsersPage />
            </RequireRole>
          }
        />
        <Route
          path="admin/moderation"
          element={
            <RequireRole allowedRoles={["ADMIN"]}>
              <AdminModerationPage />
            </RequireRole>
          }
        />
        <Route
          path="profile"
          element={
            <RequireRole allowedRoles={["EMPLOYEE"]}>
              <ProfilePage />
            </RequireRole>
          }
        />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="forbidden" element={<ForbiddenPage />} />
      </Route>

      <Route path="/" element={<Navigate to="/app/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/app/dashboard" replace />} />
    </Routes>
  );
}
