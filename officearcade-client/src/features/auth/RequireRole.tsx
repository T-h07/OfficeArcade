import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";
import type { AppRole } from "./auth.types";

type RequireRoleProps = {
  allowedRoles: AppRole[];
  children: React.ReactNode;
};

export function RequireRole({ allowedRoles, children }: RequireRoleProps) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (!allowedRoles.includes(user.role)) {
    return <Navigate to="/app/forbidden" replace state={{ from: location }} />;
  }

  return <>{children}</>;
}
