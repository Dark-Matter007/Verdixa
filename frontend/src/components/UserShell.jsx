import { BookOpen, CalendarDays, History, LogOut, Map, Swords, Trophy, UserRound } from "lucide-react";
import { NavLink, useNavigate } from "react-router-dom";
import BrandLogo from "./BrandLogo";
import Avatar from "./Avatar";
import UserAccountMenu from "./UserAccountMenu";

const links = [
  ["/problems", "Practice", BookOpen], ["/contests", "Compete", Swords], ["/paths", "Paths", Map],
  ["/daily", "Daily", CalendarDays], ["/history", "Activity", History], ["/leaderboard", "Ranks", Trophy], ["/profile", "Profile", UserRound],
];

export default function UserShell({ children, context }) {
  const navigate = useNavigate();
  const username = localStorage.getItem("algosphere_username") || "User";
  const logout = () => {
    ["algosphere_token", "algosphere_username", "algosphere_role"].forEach((key) => localStorage.removeItem(key)); window.dispatchEvent(new Event("verdixa-signed-out"));
    navigate("/login");
  };
  return <div className="dashboard-layout vx-user-shell">
    <aside className="sidebar vx-user-sidebar" aria-label="User navigation">
      <NavLink className="sidebar-brand" to="/dashboard"><BrandLogo compact /><div><strong>Verdixa</strong><span>User workspace</span></div></NavLink>
      <nav className="sidebar-nav">
        {links.map(([to, label, Icon]) => <NavLink key={to} to={to} className={({ isActive }) => `sidebar-item ${isActive ? "active" : ""}`}><Icon size={17} aria-hidden="true"/><span>{label}</span></NavLink>)}
      </nav>
      <div className="vx-user-sidebar-footer"><div><Avatar name={username}/><strong>{username}</strong></div></div>
      <button className="sidebar-logout" onClick={logout}><LogOut size={17} aria-hidden="true"/>Sign out</button>
    </aside>
    <main className="dashboard-main vx-main"><header className="vx-workspace-header"><span>{context || "Workspace"}</span><UserAccountMenu /></header><NavLink className="sr-only" to="/dashboard">Back to Problems</NavLink>{children}</main>
  </div>;
}
