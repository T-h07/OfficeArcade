import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { FullScreenStatus } from "../../components/FullScreenStatus";

type RequireAuthProps = {
  children: React.ReactNode;
};

export function RequireAuth({ children }: RequireAuthProps) {
  const { status } = useAuth();
  const location = useLocation();

  if (status === "checking") {
    return <FullScreenStatus title="Restoring session" message="Checking saved authentication state..." />;
  }

  if (status === "unauthenticated") {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <>{children}</>;
}
