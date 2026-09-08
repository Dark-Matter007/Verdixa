import { BarChart3, History, LogOut, Trophy, CalendarDays, Map, Swords } from "lucide-react";
import { useNavigate } from "react-router-dom";
import ThemeToggle from "./ThemeToggle";
import BrandLogo from "./BrandLogo";

function UserNavigation({ active, compact = false }) {
  const navigate = useNavigate();
  const username = localStorage.getItem("algosphere_username") || "User";

  const logout = () => {
    localStorage.removeItem("algosphere_token");
    localStorage.removeItem("algosphere_username");
    localStorage.removeItem("algosphere_role");
    window.dispatchEvent(new Event("verdixa-signed-out"));
    navigate("/login");
  };

  return (
    <aside className={`sidebar vx-user-sidebar ${compact ? "compact" : ""}`} aria-label="User navigation">
      <div className="sidebar-brand"><BrandLogo compact /><div><strong>Verdixa</strong><span>Workspace</span></div></div>
      <nav className="sidebar-nav">
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
      <button className={active === "contests" ? "user-nav-button active" : "user-nav-button"} onClick={() => navigate("/contests")} title="Contests"><Swords size={17} /><span>Contests</span></button>
      <button className={active === "paths" ? "user-nav-button active" : "user-nav-button"} onClick={() => navigate("/learning-paths")} title="Learning paths"><Map size={17} /><span>Paths</span></button>
      <button className={active === "daily" ? "user-nav-button active" : "user-nav-button"} onClick={() => navigate("/daily-challenge")} title="Daily challenge"><CalendarDays size={17} /><span>Daily</span></button>
      </nav>
      <div className="vx-user-sidebar-footer"><div><strong>{username}</strong></div><ThemeToggle /></div>
      <button className="logout-button" onClick={logout} title="Logout">
        <LogOut size={17} />
        Sign out
      </button>
    </aside>
  );
}

export default UserNavigation;
