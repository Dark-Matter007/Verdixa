import { Command, LogOut } from "lucide-react";
import { NavLink, useNavigate } from "react-router-dom";
import BrandLogo from "./BrandLogo";
import Avatar from "./Avatar";

const links = [
  ["/problems", "Practice"], ["/contests", "Compete"], ["/paths", "Paths"],
  ["/daily", "Daily"], ["/history", "Activity"], ["/leaderboard", "Ranks"], ["/profile", "Profile"],
];

export default function UserShell({ children, context }) {
  const navigate = useNavigate();
  const username = localStorage.getItem("algosphere_username") || "User";
  const logout = () => {
    ["algosphere_token", "algosphere_username", "algosphere_role"].forEach((key) => localStorage.removeItem(key));
    navigate("/login");
  };
  return <div className="vx-shell">
    <header className="vx-topbar">
      <NavLink className="vx-wordmark" to="/dashboard"><BrandLogo compact /><span>Verdixa</span></NavLink>
      <nav className="vx-primary-nav" aria-label="Primary navigation">
        {links.map(([to, label]) => <NavLink key={to} to={to} className={({ isActive }) => isActive ? "active" : ""}>{label}</NavLink>)}
      </nav>
      <div className="vx-identity"><Command size={14} aria-hidden="true" /><span>{context || "Workspace"}</span><Avatar name={username} /><strong>{username}</strong><button onClick={logout} aria-label="Sign out"><LogOut size={15} /></button></div>
    </header>
    <main className="vx-main"><NavLink className="sr-only" to="/dashboard">Back to Problems</NavLink>{children}</main>
  </div>;
}
