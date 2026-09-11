import Sidebar from "./Sidebar";
import UserAccountMenu from "./UserAccountMenu";
import VerdixaAssistant from "./VerdixaAssistant";

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
        <div className="vx-admin-head-actions">{actions}<UserAccountMenu /></div>
      </header>
      {children}
    </main>
  <VerdixaAssistant mode="admin" /></div>;
}
