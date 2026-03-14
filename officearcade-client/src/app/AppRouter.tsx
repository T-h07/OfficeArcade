import { Navigate, Route, Routes } from "react-router-dom";
import { RequireAuth } from "../features/auth/RequireAuth";
import { PublicOnlyRoute } from "../features/auth/PublicOnlyRoute";
import { RequireRole } from "../features/auth/RequireRole";
import { AdminUsersPage } from "../features/admin-users/pages/AdminUsersPage";
import { AppShell } from "../features/layout/AppShell";
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
            <AppShell />
          </RequireAuth>
        }
      >
        <Route index element={<Navigate to="/app/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="play" element={<PlayPage />} />
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
