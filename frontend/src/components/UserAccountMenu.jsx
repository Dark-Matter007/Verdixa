import { useEffect, useRef, useState } from "react";
import { ChevronDown, LayoutDashboard, LogOut, ShieldCheck, UserRound } from "lucide-react";
import { useNavigate } from "react-router-dom";
import Avatar from "./Avatar";
import ThemeToggle from "./ThemeToggle";

/** Compact shared account control for authenticated workspaces. */
export default function UserAccountMenu() {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const root = useRef(null);
  const username = localStorage.getItem("algosphere_username") || "User";
  const role = localStorage.getItem("algosphere_role");

  useEffect(() => {
    const close = (event) => { if (!root.current?.contains(event.target)) setOpen(false); };
    window.addEventListener("pointerdown", close);
    return () => window.removeEventListener("pointerdown", close);
  }, []);
  const logout = () => {
    ["algosphere_token", "algosphere_username", "algosphere_role"].forEach((key) => localStorage.removeItem(key));
    window.dispatchEvent(new Event("verdixa-signed-out"));
    navigate("/login");
  };
  const go = (path) => { setOpen(false); navigate(path); };
  return <div className="vx-account-control" ref={root}>
    <ThemeToggle />
    <button className="vx-account-trigger" type="button" aria-expanded={open} aria-haspopup="menu" onClick={() => setOpen((value) => !value)}>
      <Avatar name={username} /><span>{username}</span><ChevronDown size={14} aria-hidden="true" />
    </button>
    {open && <div className="vx-account-menu" role="menu" aria-label="Account menu">
      {role === "ADMIN" ? <button role="menuitem" onClick={() => go("/admin")}><ShieldCheck size={15} />Admin workspace</button> : <>
        <button role="menuitem" onClick={() => go("/dashboard")}><LayoutDashboard size={15} />Dashboard</button>
        <button role="menuitem" onClick={() => go("/profile")}><UserRound size={15} />Profile</button>
      </>}
      <button role="menuitem" onClick={() => go(role === "ADMIN" ? "/admin/profile/edit" : "/profile/edit")}><UserRound size={15} />Edit profile</button>
      <button role="menuitem" className="vx-account-signout" onClick={logout}><LogOut size={15} />Sign out</button>
    </div>}
  </div>;
}
