import { Navigate, useLocation } from "react-router-dom";
import VerdixaAssistant from "./VerdixaAssistant";

function ProtectedRoute({ children, allowedRoles }) {
  const location = useLocation();
  const token = localStorage.getItem("algosphere_token");
  const role = localStorage.getItem("algosphere_role");

  if (!token || !role) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (allowedRoles && !allowedRoles.includes(role)) {
    return <Navigate to={role === "ADMIN" ? "/admin" : "/dashboard"} replace />;
  }

  const assessmentFocusRoute = /^\/assessments\/\d+\/take$/.test(location.pathname) || /^\/assessment\/\d+\/preflight$/.test(location.pathname);
  return <>{children}{role === "USER" && !assessmentFocusRoute && <VerdixaAssistant />}</>;
}

export default ProtectedRoute;
