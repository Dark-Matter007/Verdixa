import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Code2, Users, LogOut, Plus, List, ShieldCheck } from "lucide-react";
import api from "../services/api";
import BrandLogo from "../components/BrandLogo";

function AdminDashboard() {
  const navigate = useNavigate();
  const [problems, setProblems] = useState([]); const [statistics, setStatistics] = useState(null);
  const [loading, setLoading] = useState(true); const [error, setError] = useState("");
  const loadDashboardData = async () => { setLoading(true); setError(""); try {
    const [problemsResponse, statisticsResponse] = await Promise.all([api.get("/problems/admin/all"), api.get("/admin/analytics")]);
    setProblems(problemsResponse.data); setStatistics(statisticsResponse.data);
  } catch (err) { console.error("Failed to load dashboard data:", err); setError("Unable to load administrative statistics."); } finally { setLoading(false); } };
  useEffect(() => { if (localStorage.getItem("algosphere_role") !== "ADMIN") { navigate("/dashboard"); return; } loadDashboardData(); }, [navigate]);
  const logout = () => { ["algosphere_token", "algosphere_username", "algosphere_role"].forEach((key) => localStorage.removeItem(key)); navigate("/login"); };
  const cards = statistics ? [["Published Problems", statistics.publishedProblems], ["Total Users", statistics.totalUsers], ["Active Users (30d)", statistics.activeUsers], ["Submissions", statistics.totalSubmissions], ["Accepted", statistics.acceptedSubmissions], ["Today", statistics.submissionsToday], ["Last 7 days", statistics.submissionsLast7Days], ["Last 30 days", statistics.submissionsLast30Days], ["Acceptance Rate", `${statistics.acceptanceRate}%`]] : [];
  const integrityLabel = (problem) => {
    if (!problem.active) return "Draft / inactive";
    if (problem.executionMode === "STDIN") return "Valid STDIN";
    const signature = problem.functionSignature;
    return signature?.functionName && signature?.returnType && Array.isArray(signature.parameters)
      ? "Valid FUNCTION"
      : "Invalid FUNCTION metadata";
  };
  return <div className="dashboard-layout"><aside className="sidebar"><button className="sidebar-brand" onClick={() => navigate("/admin")} aria-label="Verdixa admin dashboard"><BrandLogo compact /><div><strong>Verdixa</strong><span>Admin</span></div></button><nav className="sidebar-nav"><button className="sidebar-item active" onClick={() => navigate("/admin")}><ShieldCheck size={18}/>Dashboard</button><button className="sidebar-item" onClick={() => navigate("/admin/problems")}><List size={18}/>Problems</button><button className="sidebar-item" onClick={() => navigate("/admin/users")}><Users size={18}/>Users</button><button className="sidebar-item" onClick={() => navigate("/admin/collections")}><List size={18}/>Collections</button><button className="sidebar-item" onClick={() => navigate("/admin/learning-paths")}><List size={18}/>Learning Paths</button><button className="sidebar-item" onClick={() => navigate("/admin/daily-challenges")}><List size={18}/>Daily Challenges</button></nav><button className="sidebar-logout" onClick={logout}><LogOut size={18}/>Logout</button></aside>
    <main className="dashboard-main"><header className="dashboard-header"><div><div className="admin-badge"><ShieldCheck size={18}/>Administrator</div><h1>Admin Dashboard</h1><p>Database-derived platform health and management tools.</p></div><button className="primary-button" onClick={() => navigate("/admin/problems/new")}><Plus size={18}/>Add Problem</button></header>
      {error && <div className="error-message">{error}</div>}
      <section className="stats-grid">{loading ? <div className="loading">Loading platform statistics...</div> : cards.map(([label, value]) => <div className="stat-card" key={label} onClick={label === "Total Users" ? () => navigate("/admin/users") : undefined}><div className="stat-icon"><Code2 size={22}/></div><div><span>{label}</span><strong>{value}</strong></div></div>)}</section>
      <section className="dashboard-section"><div className="section-header"><div><h2>Problem Management</h2><p>Create, edit, deactivate, and manage judge test cases.</p></div><button className="secondary-button" onClick={() => navigate("/admin/problems")}>View All</button></div>{loading ? <div className="loading">Loading problems...</div> : problems.length === 0 ? <div className="empty-state"><Code2 size={45}/><h3>No problems yet</h3><p>Create your first coding problem.</p></div> : <div className="problem-preview">{problems.slice(0, 5).map((problem) => <div className="problem-preview-row" key={problem.id}><div className="problem-preview-info"><strong>{problem.title}</strong><small>Problem #{problem.id} · {problem.active ? "Active" : "Inactive"}</small><span className={integrityLabel(problem).startsWith("Invalid") ? "integrity-warning" : "integrity-ok"}>{integrityLabel(problem)}</span></div><span className={`difficulty ${problem.difficulty?.toLowerCase()}`}>{problem.difficulty}</span><div className="problem-preview-actions"><button className="secondary-button" onClick={() => navigate(`/admin/problems/edit/${problem.id}`)}>Edit</button><button className="secondary-button" onClick={() => navigate(`/admin/problems/${problem.id}/analytics`)}>Analytics</button><button className="secondary-button" onClick={() => navigate(`/admin/problems/${problem.id}/testcases`)}>Test Cases</button></div></div>)}</div>}</section>
    </main></div>;
}
export default AdminDashboard;
