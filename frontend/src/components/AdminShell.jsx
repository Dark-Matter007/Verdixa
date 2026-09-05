import Sidebar from "./Sidebar";

export default function AdminShell({ eyebrow = "Administration", title, description, actions, children }) {
  return <div className="dashboard-layout vx-admin-shell">
    <Sidebar />
    <main className="dashboard-main vx-admin-main">
      <header className="vx-admin-head">
        <div>
          <p className="vx-eyebrow">{eyebrow}</p>
          <h1>{title}</h1>
          {description && <p>{description}</p>}
        </div>
        {actions && <div className="vx-admin-head-actions">{actions}</div>}
      </header>
      {children}
    </main>
  </div>;
}
