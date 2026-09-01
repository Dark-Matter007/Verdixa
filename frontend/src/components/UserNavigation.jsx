import { BarChart3, History, LogOut, Trophy, CalendarDays, Map } from "lucide-react";
import { useNavigate } from "react-router-dom";

function UserNavigation({ active, compact = false }) {
  const navigate = useNavigate();
  const username = localStorage.getItem("algosphere_username") || "User";

  const logout = () => {
    localStorage.removeItem("algosphere_token");
    localStorage.removeItem("algosphere_username");
    localStorage.removeItem("algosphere_role");
    navigate("/login");
  };

  return (
    <div className={`user-navigation ${compact ? "compact" : ""}`}>
      {!compact && (
        <div className="user-info">
          <span className="user-label">SIGNED IN AS</span>
          <strong>{username}</strong>
        </div>
      )}
      <button className={active === "dashboard" ? "user-nav-button active" : "user-nav-button"} onClick={() => navigate("/dashboard")} title="Problem library">
        <BarChart3 size={17} />
        <span>Problems</span>
      </button>
      <button className={active === "history" ? "user-nav-button active" : "user-nav-button"} onClick={() => navigate("/history")} title="Submission history">
        <History size={17} />
        <span>History</span>
      </button>
      <button className={active === "profile" ? "user-nav-button active" : "user-nav-button"} onClick={() => navigate("/profile")} title="Your progress">
        <BarChart3 size={17} />
        <span>Progress</span>
      </button>
      <button className={active === "leaderboard" ? "user-nav-button active" : "user-nav-button"} onClick={() => navigate("/leaderboard")} title="Leaderboard">
        <Trophy size={17} />
        <span>Ranks</span>
      </button>
      <button className={active === "paths" ? "user-nav-button active" : "user-nav-button"} onClick={() => navigate("/learning-paths")} title="Learning paths"><Map size={17} /><span>Paths</span></button>
      <button className={active === "daily" ? "user-nav-button active" : "user-nav-button"} onClick={() => navigate("/daily-challenge")} title="Daily challenge"><CalendarDays size={17} /><span>Daily</span></button>
      <button className="logout-button" onClick={logout} title="Logout">
        <LogOut size={17} />
        {!compact && "Logout"}
      </button>
    </div>
  );
}

export default UserNavigation;
