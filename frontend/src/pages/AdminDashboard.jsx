import { useCallback, useEffect, useState } from "react";
import { ArrowUpRight, Plus } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import api from "../services/api";
import AdminShell from "../components/AdminShell";
import StatusPill from "../components/StatusPill";
import { EmptyState, ErrorState, LoadingState } from "../components/PageState";

export default function AdminDashboard() {
  const [certificateSummary,setCertificateSummary]=useState(null);
  useEffect(()=>{api.get("/admin/certificates/summary").then(r=>setCertificateSummary(r.data)).catch(()=>{});},[]);
  const navigate = useNavigate(); const [problems, setProblems] = useState([]); const [statistics, setStatistics] = useState(null); const [loading, setLoading] = useState(true); const [error, setError] = useState("");
  const load = useCallback(async () => { setLoading(true); setError(""); try { const [problemResponse, statisticsResponse] = await Promise.all([api.get("/problems/admin/all"), api.get("/admin/analytics")]); setProblems(problemResponse.data || []); setStatistics(statisticsResponse.data); } catch { setError("Administrative telemetry could not be retrieved."); } finally { setLoading(false); } }, []);
  useEffect(() => { if (localStorage.getItem("algosphere_role") !== "ADMIN") { navigate("/dashboard"); return; } load(); }, [load, navigate]);
  const metrics = statistics ? [["Published Problems",statistics.publishedProblems],["Total Users",statistics.totalUsers],["Active / 30d",statistics.activeUsers],["Submissions",statistics.totalSubmissions],["Accepted",statistics.acceptedSubmissions],["Today",statistics.submissionsToday],["7 days",statistics.submissionsLast7Days],["Acceptance",`${statistics.acceptanceRate}%`]] : [];
  return <AdminShell eyebrow="Control room / overview" title="Platform operations" description="Content integrity, user activity, and judge outcomes from persisted platform data." actions={<button className="primary-button" onClick={() => navigate("/admin/problems/new")}><Plus size={16}/> New problem</button>}>
    {error && <ErrorState message={error} onRetry={load}/>} {loading && <LoadingState label="Loading platform telemetry"/>}
    {!loading && statistics && <section className="vx-admin-metrics" aria-label="Platform statistics">{metrics.map(([label,value]) => <div key={label}><span>{label}</span><strong>{value}</strong></div>)}</section>}
    {certificateSummary && <section className="certificate-admin-summary"><h2>Certificates Issued</h2><dl>{[50,100,150].map(t=><div key={t}><dt>{t} Problem Certificates</dt><dd>{certificateSummary[t]}</dd></div>)}<div><dt>Total Certificates Issued</dt><dd>{certificateSummary.total}</dd></div></dl></section>}
    <section className="vx-admin-section"><div className="vx-library-header"><div><span className="vx-section-meta">Content integrity</span><h2 className="vx-section-title">Recently managed problems</h2></div><Link className="vx-text-link" to="/admin/problems">Full inventory</Link></div>
      {!loading && !problems.length && <EmptyState title="No problems created">Create the first problem to establish the content inventory.</EmptyState>}
      {!loading && problems.length > 0 && <div className="vx-admin-table"><div className="vx-admin-table-head"><span>Problem</span><span>Mode</span><span>Difficulty</span><span>Publication</span><span>Actions</span></div>{problems.slice(0,8).map((problem) => { const validFunction = problem.executionMode !== "FUNCTION" || Boolean(problem.functionSignature?.functionName && problem.functionSignature?.returnType && Array.isArray(problem.functionSignature?.parameters)); return <div className="vx-admin-row" key={problem.id}><strong>{problem.title}<small>Problem #{problem.id} · {problem.active ? "Active" : "Inactive"}</small></strong><span>{validFunction ? `Valid ${problem.executionMode}` : "Invalid FUNCTION metadata"}</span><StatusPill value={problem.difficulty}/><StatusPill value={problem.active ? "Published" : "Draft"}/><span className="vx-admin-actions"><button onClick={() => navigate(`/admin/problems/${problem.id}/analytics`)}>Analytics</button><button onClick={() => navigate(`/admin/problems/${problem.id}/testcases`)}>Test Cases</button><Link to={`/admin/problems/edit/${problem.id}`} aria-label={`Edit ${problem.title}`}><ArrowUpRight size={15}/></Link></span></div>; })}</div>}
    </section>
  </AdminShell>;
}
