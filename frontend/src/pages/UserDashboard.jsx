import { useNavigate } from "react-router-dom";
import { Code2, BookOpen, LogOut } from "lucide-react";

function UserDashboard() {
  const navigate = useNavigate();

  const username =
    localStorage.getItem("algosphere_username") || "User";

  const logout = () => {
    localStorage.removeItem("algosphere_token");
    localStorage.removeItem("algosphere_username");
    localStorage.removeItem("algosphere_role");

    navigate("/login");
  };

  return (
    <div className="dashboard-layout">

      <aside className="sidebar">

        <div className="sidebar-brand">
          <div className="sidebar-logo">
            <Code2 size={24} />
          </div>

          <div>
            <strong>Verdixa</strong>
            <span>USER</span>
          </div>
        </div>

        <nav className="sidebar-nav">

          <button className="sidebar-item active">
            <BookOpen size={20} />
            Problems
          </button>

        </nav>

        <button className="logout-button" onClick={logout}>
          <LogOut size={20} />
          Logout
        </button>

      </aside>

      <main className="dashboard-main">

        <header className="dashboard-header">
          <div>
            <h1>Welcome, {username}</h1>
            <p>Practice algorithms and improve your coding skills.</p>
          </div>
        </header>

        <section className="dashboard-section">

          <div className="section-header">
            <div>
              <h2>Start Coding</h2>
              <p>Choose a problem and begin solving.</p>
            </div>
          </div>

          <div className="empty-state">
            <BookOpen size={40} />

            <h3>Problems</h3>

            <p>
              Your coding problems will appear here.
            </p>

            <button
              className="primary-button"
              onClick={() => navigate("/problems")}
            >
              Browse Problems
            </button>
          </div>

        </section>

      </main>

    </div>
  );
}

export default UserDashboard;
