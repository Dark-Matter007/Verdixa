import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../services/api";
import UserShell from "../components/UserShell";
import { EmptyState, ErrorState, LoadingState } from "../components/PageState";

export default function DailyChallenge() {
  const [today, setToday] = useState(null); const [history, setHistory] = useState([]); const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true); const [error, setError] = useState("");
  const load = useCallback(() => { setLoading(true); setError(""); Promise.allSettled([api.get("/daily-challenges/today"), api.get("/daily-challenges"), api.get("/daily-challenges/summary")]).then(([current, previous, stats]) => { if (current.status !== "fulfilled") { setError("Today's challenge could not be retrieved."); return; } setToday(current.value.data); setHistory(previous.status === "fulfilled" ? previous.value.data || [] : []); setSummary(stats.status === "fulfilled" ? stats.value.data : null); }).finally(() => setLoading(false)); }, []);
  useEffect(() => { load(); }, [load]);
  return <UserShell context="Daily focus"><header className="vx-page-intro"><div><p className="vx-eyebrow">Daily discipline</p><h1>One problem.<br/>Full attention.</h1><p>A single persisted challenge anchors today’s practice without turning progress into a game.</p></div>{summary && <aside className="vx-intelligence"><div className="vx-metric"><span>Current</span><strong>{summary.currentStreak}d</strong></div><div className="vx-metric"><span>Longest</span><strong>{summary.longestStreak}d</strong></div></aside>}</header>
    {error && <ErrorState message={error} onRetry={load}/>} {loading && <LoadingState label="Loading daily challenge"/>}
    {!loading && !error && (today?.problem ? <section className="vx-focus"><div><span className="vx-section-meta">{today.challengeDate}</span><h2>{today.problem.title}</h2><p className="vx-deck">{today.problem.description || "Open the specification and work through the challenge in the Verdixa solver."}</p></div><div className="vx-focus-meta"><span className="vx-status">{today.completed ? "Completed" : "Ready"}</span><p>{today.problem.difficulty} · {today.problem.tags || "General"}</p><Link className="primary-button" to={`/problems/${today.problem.id}`}>{today.completed ? "Review Challenge" : "Open Challenge"}</Link></div></section> : <EmptyState title="No challenge scheduled today">The full problem field remains available for deliberate practice.</EmptyState>)}
    {!loading && !error && <section className="vx-report-section"><span className="vx-section-meta">Previous challenges</span><h2>Recent cadence</h2>{history.length ? history.map((item) => <div className="vx-ledger-row" key={item.id}><time>{item.challengeDate}</time><Link to={`/problems/${item.problem.id}`}>{item.problem.title}</Link><span>{item.completed ? "Completed" : "Open"}</span></div>) : <p className="admin-empty-copy">No previous challenges yet.</p>}</section>}
  </UserShell>;
}
