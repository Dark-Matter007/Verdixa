import { BarChart3, BookOpen, CalendarDays, ClipboardList, FolderKanban, LogOut, Swords, Users } from "lucide-react";
import { NavLink, useNavigate } from "react-router-dom";
import BrandLogo from "./BrandLogo";

const links = [
  ["/admin", "Overview", BarChart3, true],
  ["/admin/problems", "Problems", BookOpen],
  ["/admin/contests", "Contests", Swords],
  ["/admin/users", "Users", Users],
  ["/admin/collections", "Collections", FolderKanban],
  ["/admin/learning-paths", "Learning paths", ClipboardList],
  ["/admin/daily-challenges", "Daily challenges", CalendarDays],
];

/** Shared enterprise navigation for every administration surface. */
export default function Sidebar() {
  const navigate = useNavigate();
  const logout = () => {
    ["algosphere_token", "algosphere_username", "algosphere_role"].forEach((key) => localStorage.removeItem(key));
    navigate("/login");
  };

  return <aside className="sidebar admin-sidebar" aria-label="Administration navigation">
    <NavLink className="sidebar-brand" to="/admin" aria-label="Verdixa administration">
      <BrandLogo compact />
      <div><strong>Verdixa</strong><span>Control room</span></div>
    </NavLink>
    <nav className="sidebar-nav">
      {links.map(([to, label, Icon, end]) => <NavLink end={end} className={({ isActive }) => `sidebar-item ${isActive ? "active" : ""}`} to={to} key={to}>
        <Icon size={17} aria-hidden="true" /><span>{label}</span>
      </NavLink>)}
    </nav>
    <button className="sidebar-logout" onClick={logout}><LogOut size={17} aria-hidden="true" />Sign out</button>
  </aside>;
}
