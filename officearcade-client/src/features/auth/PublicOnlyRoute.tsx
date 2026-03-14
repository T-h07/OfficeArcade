import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { FullScreenStatus } from "../../components/FullScreenStatus";

type PublicOnlyRouteProps = {
  children: React.ReactNode;
};

export function PublicOnlyRoute({ children }: PublicOnlyRouteProps) {
  const { status } = useAuth();

  if (status === "checking") {
    return <FullScreenStatus title="Loading OfficeArcade" message="Preparing authentication context..." />;
  }

  if (status === "authenticated") {
    return <Navigate to="/app/dashboard" replace />;
  }

  return <>{children}</>;
}
