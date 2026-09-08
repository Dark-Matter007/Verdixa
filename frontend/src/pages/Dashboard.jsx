import CertificateProgress from "../components/CertificateProgress";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Bookmark, Check, ChevronRight, Search } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import api from "../services/api";
import UserShell from "../components/UserShell";
import StatusPill from "../components/StatusPill";
import { EmptyState, ErrorState, LoadingState } from "../components/PageState";

export default function Dashboard() {
  const navigate = useNavigate();
  const username = localStorage.getItem("algosphere_username") || "Developer";
  const [problems, setProblems] = useState([]); const [progress, setProgress] = useState(null);
  const [bookmarks, setBookmarks] = useState(new Set()); const [total, setTotal] = useState(null);
  const [search, setSearch] = useState(""); const [difficulty, setDifficulty] = useState("ALL");
  const [topic, setTopic] = useState("ALL"); const [status, setStatus] = useState("ALL"); const [sort, setSort] = useState("title");
  const [bookmarkedOnly, setBookmarkedOnly] = useState(false); const [loading, setLoading] = useState(true); const [error, setError] = useState(""); const [bookmarkError, setBookmarkError] = useState("");

  const load = useCallback(async () => {
    setLoading(true); setError(""); setBookmarkError("");
    const results = await Promise.allSettled([
      api.get("/problems/library", { params: { search, difficulty, topic, status, bookmarked: bookmarkedOnly, sort, size: 100 } }),
      api.get("/users/me/progress"), api.get("/bookmarks"),
    ]);
    const authFailure = results.find((result) => result.status === "rejected" && [401, 403].includes(result.reason?.response?.status));
    if (authFailure) { localStorage.clear(); navigate("/login"); return; }
    if (results[0].status === "fulfilled") {
      const page = results[0].value.data; const content = Array.isArray(page?.content) ? page.content : [];
      setProblems(content.map((entry) => ({ ...entry.problem, userStatus: entry.status, problemStats: entry.statistics })));
      setTotal(Number.isFinite(page?.totalElements) ? page.totalElements : content.length);
    } else { setProblems([]); setError("We couldn't retrieve the problem library."); }
    setProgress(results[1].status === "fulfilled" ? results[1].value.data : null);
    setBookmarks(new Set(results[2].status === "fulfilled" ? (results[2].value.data || []).map((problem) => problem.id) : []));
    if (results[2].status === "rejected") setBookmarkError("Unable to load bookmarks. The problem field is still available.");
    setLoading(false);
  }, [bookmarkedOnly, difficulty, navigate, search, sort, status, topic]);
  useEffect(() => { load(); }, [load]);

  const topics = useMemo(() => [...new Set(problems.flatMap((problem) => (problem.tags || "").split(",").map((tag) => tag.trim()).filter(Boolean)))].sort(), [problems]);
  const visible = useMemo(() => problems.filter((problem) => {
    const match = `${problem.title || ""} ${problem.tags || ""}`.toLowerCase().includes(search.toLowerCase());
    return match && (difficulty === "ALL" || problem.difficulty?.toUpperCase() === difficulty) && (topic === "ALL" || (problem.tags || "").split(",").map((tag) => tag.trim()).includes(topic)) && (!bookmarkedOnly || bookmarks.has(problem.id));
  }), [bookmarkedOnly, bookmarks, difficulty, problems, search, topic]);
  const toggleBookmark = async (event, id) => { event.stopPropagation(); const exists = bookmarks.has(id); try { await api({ method: exists ? "delete" : "post", url: `/bookmarks/${id}` }); setBookmarks((current) => { const next = new Set(current); if (exists) next.delete(id); else next.add(id); return next; }); } catch { setError("The bookmark could not be updated."); } };
  const firstOpen = visible.find((problem) => problem.userStatus === "ATTEMPTED") || visible.find((problem) => problem.userStatus !== "SOLVED") || visible[0];

  return <UserShell context="Practice">
    <section className="vx-dashboard-head">
      <div className="vx-command"><p className="vx-eyebrow">Personal workspace / ready</p><h1 className="vx-display">Good to see you,<br/><em>{username}.</em></h1><p className="vx-deck">Your practice queue is ready. Continue a thread of thought or choose a new problem from the field below.</p></div>
      <div className="vx-intelligence" aria-label="Your progress summary">
        <div className="vx-metric"><span>Total Problems</span><strong>{total ?? "—"}</strong></div>
        <div className="vx-metric"><span>Solved</span><strong>{progress?.solvedProblems ?? "—"}</strong></div>
        <div className="vx-metric"><span>Acceptance</span><strong>{progress ? `${progress.acceptanceRate}%` : "—"}</strong></div>
        <div className="vx-metric"><span>Current Streak</span><strong>{progress ? `${progress.currentStreak} days` : "—"}</strong></div>
        <div className="vx-metric"><span>Completion</span><strong>{progress ? `${progress.completionPercentage}%` : "—"}</strong></div>
        <div className="vx-completion-bar" role="progressbar" aria-label="Problem completion" aria-valuemin="0" aria-valuemax="100" aria-valuenow={progress?.completionPercentage ?? 0}><i style={{width:`${Math.min(100, Math.max(0, progress?.completionPercentage || 0))}%`}} /></div>
      </div>
    </section>
    <CertificateProgress compact/>
    <div className="vx-dashboard-grid">
      <section className="vx-library" aria-labelledby="problem-library-title">
        <div className="vx-library-header"><div><span className="vx-section-meta">Problem field · {total ?? "—"} available</span><h2 className="vx-section-title" id="problem-library-title">Choose your next problem</h2><p>Search, narrow, and move directly into the workspace.</p></div><span className="vx-section-meta">{visible.length} results</span></div>
        <div className="vx-filter-deck"><label className="vx-search"><Search size={16}/><span className="sr-only">Search problems</span><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search titles or topics" /></label><select aria-label="Topic" value={topic} onChange={(event) => setTopic(event.target.value)}><option value="ALL">All topics</option>{topics.map((item) => <option key={item}>{item}</option>)}</select><select aria-label="Sort problems" value={sort} onChange={(event) => setSort(event.target.value)}><option value="title">Title</option><option value="difficulty">Difficulty</option><option value="acceptanceRate">Acceptance</option></select></div>
        <div className="vx-difficulty-tabs" role="group" aria-label="Difficulty filter">{["ALL","EASY","MEDIUM","HARD"].map((level) => <button className={difficulty === level ? "active" : ""} key={level} onClick={() => setDifficulty(level)}>{level === "ALL" ? "All levels" : level[0] + level.slice(1).toLowerCase()}</button>)}<button className={bookmarkedOnly ? "active" : ""} onClick={() => setBookmarkedOnly((value) => !value)}>Bookmarked</button><select className="vx-filter-button" aria-label="Status" value={status} onChange={(event) => setStatus(event.target.value)}><option value="ALL">Any status</option><option value="SOLVED">Solved</option><option value="ATTEMPTED">Attempted</option><option value="UNATTEMPTED">Unopened</option></select></div>
        {error && <ErrorState message={error} onRetry={load}/>} {!error && bookmarkError && <ErrorState message={bookmarkError} onRetry={load}/>} {loading && <LoadingState label="Loading problem field"/>}
        {!loading && !error && !visible.length && <EmptyState title="No problems in this view">Try clearing a filter or searching another topic.</EmptyState>}
        {!loading && !error && !!visible.length && <div className="vx-problem-list">{visible.map((problem, index) => { const solved = problem.userStatus === "SOLVED"; const tags = (problem.tags || "").split(",").filter(Boolean).slice(0,2); return <article className="vx-problem-row" key={problem.id} tabIndex="0" onClick={() => navigate(`/problems/${problem.id}`)} onKeyDown={(event) => { if (event.key === "Enter") navigate(`/problems/${problem.id}`); }}><span className={`vx-problem-index ${solved ? "solved" : ""}`}>{solved ? <Check size={15}/> : String(index + 1).padStart(3,"0")}</span><div className="vx-problem-copy"><strong>{problem.title}</strong><small>{problem.description || "Open the problem specification"}</small></div><div className="vx-problem-tags">{tags.map((tag) => <span key={tag}>{tag.trim()}</span>)}</div><StatusPill value={problem.difficulty}/><span className="vx-acceptance">{problem.problemStats?.acceptanceRate == null ? "—" : `${problem.problemStats.acceptanceRate}%`}</span><button className={`vx-icon-button ${bookmarks.has(problem.id) ? "active" : ""}`} aria-label={bookmarks.has(problem.id) ? "Remove bookmark" : "Bookmark problem"} onClick={(event) => toggleBookmark(event, problem.id)}><Bookmark size={15} fill={bookmarks.has(problem.id) ? "currentColor" : "none"}/></button></article>; })}</div>}
      </section>
      <aside className="vx-side-rail" aria-label="Your next actions">
        <section className="vx-rail-section"><span className="vx-section-meta">Resume</span><h3>{firstOpen ? "Continue unresolved work" : "Choose a problem"}</h3><p>{firstOpen ? `Return to a ${firstOpen.difficulty?.toLowerCase()} problem from your current field.` : "Published problems will appear here when available."}</p>{firstOpen && <Link className="vx-rail-link" to={`/problems/${firstOpen.id}`}>Open workspace <ChevronRight size={14}/></Link>}</section>
        <section className="vx-rail-section"><span className="vx-section-meta">Daily focus</span><h3>One deliberate problem</h3><p>Keep your practice cadence grounded in today’s persisted challenge.</p><Link className="vx-rail-link" to="/daily-challenge">View daily challenge <ChevronRight size={14}/></Link></section>
        <section className="vx-rail-section"><span className="vx-section-meta">Competition</span><h3>Timed environments</h3><p>Registration, active contests, and standings live in a dedicated arena.</p><Link className="vx-rail-link" to="/contests">Enter competitions <ChevronRight size={14}/></Link></section>
      </aside>
    </div>
  </UserShell>;
}
