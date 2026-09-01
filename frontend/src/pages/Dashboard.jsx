import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Bookmark, BookOpen, CheckCircle2, ChevronRight, Code2, Filter, Flame, Search } from "lucide-react";
import api from "../services/api";
import UserNavigation from "../components/UserNavigation";
import BrandLogo from "../components/BrandLogo";

function Dashboard() {
  const navigate = useNavigate();
  const username = localStorage.getItem("algosphere_username") || "User";
  const [problems, setProblems] = useState([]);
  const [progress, setProgress] = useState(null);
  const [search, setSearch] = useState("");
  const [difficulty, setDifficulty] = useState("ALL");
  const [topic, setTopic] = useState("ALL");
  const [status, setStatus] = useState("ALL");
  const [sort, setSort] = useState("title");
  const [showBookmarked, setShowBookmarked] = useState(false);
  const [bookmarkedProblemIds, setBookmarkedProblemIds] = useState(new Set());
  const [totalProblems, setTotalProblems] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [progressError, setProgressError] = useState("");
  const [bookmarkError, setBookmarkError] = useState("");

  const fetchDashboard = useCallback(async () => {
    try {
      setLoading(true);
      setError("");
      setProgressError("");
      setBookmarkError("");
      const [problemsResult, progressResult, bookmarksResult] = await Promise.allSettled([
        api.get("/problems/library", { params: { search, difficulty, topic, status, bookmarked: showBookmarked, sort, size: 100 } }),
        api.get("/users/me/progress"),
        api.get("/bookmarks"),
      ]);

      const failure = [problemsResult, progressResult, bookmarksResult]
        .find((result) => result.status === "rejected" && [401, 403].includes(result.reason?.response?.status));
      if (failure) {
        localStorage.clear();
        navigate("/login");
        return;
      }

      if (problemsResult.status === "fulfilled") {
        const page = problemsResult.value.data;
        const content = Array.isArray(page?.content) ? page.content : [];
        setProblems(content.map((entry) => ({ ...entry.problem, userStatus: entry.status, problemStats: entry.statistics })));
        setTotalProblems(Number.isFinite(page?.totalElements) ? page.totalElements : content.length);
      } else {
        setProblems([]);
        setTotalProblems(null);
        setError("Unable to load dashboard data.");
      }

      if (progressResult.status === "fulfilled") setProgress(progressResult.value.data);
      else { setProgress(null); setProgressError("Unable to load progress statistics."); }

      if (bookmarksResult.status === "fulfilled") {
        setBookmarkedProblemIds(new Set((bookmarksResult.value.data || []).map((problem) => problem.id)));
      } else {
        setBookmarkedProblemIds(new Set());
        setBookmarkError("Unable to load bookmarks.");
      }
    } finally {
      setLoading(false);
    }
  }, [navigate, search, difficulty, topic, status, sort, showBookmarked]);

  useEffect(() => { fetchDashboard(); }, [fetchDashboard]);

  const topics = useMemo(() => [...new Set(problems.flatMap((problem) =>
    (problem.tags || "").split(",").map((tag) => tag.trim()).filter(Boolean)))].sort(), [problems]);
  const solvedProblemIds = useMemo(() => new Set(problems.filter((problem) => problem.userStatus === "SOLVED").map((problem) => problem.id)), [problems]);
  const filteredProblems = useMemo(() => problems.filter((problem) => {
    const text = `${problem.title || ""} ${problem.description || ""} ${problem.tags || ""}`.toLowerCase();
    return text.includes(search.toLowerCase())
      && (difficulty === "ALL" || problem.difficulty?.toUpperCase() === difficulty)
      && (topic === "ALL" || (problem.tags || "").split(",").map((tag) => tag.trim()).includes(topic))
      && (!showBookmarked || bookmarkedProblemIds.has(problem.id));
  }), [problems, search, difficulty, topic, showBookmarked, bookmarkedProblemIds]);
  const toggleBookmark = async (event, problemId) => {
    event.stopPropagation();
    const bookmarked = bookmarkedProblemIds.has(problemId);
    try {
      await api({ method: bookmarked ? "delete" : "post", url: `/bookmarks/${problemId}` });
      setBookmarkedProblemIds((previous) => {
        const next = new Set(previous);
        if (bookmarked) next.delete(problemId);
        else next.add(problemId);
        return next;
      });
    } catch { setError("Unable to update this bookmark."); }
  };
  const counts = ["EASY", "MEDIUM", "HARD"].reduce((result, level) => ({ ...result, [level]: problems.filter((problem) => problem.difficulty?.toUpperCase() === level).length }), {});

  return <div className="dashboard-page">
    <header className="dashboard-navbar">
      <button className="dashboard-brand" onClick={() => navigate("/dashboard")} aria-label="Verdixa dashboard"><BrandLogo compact /><div><h2>Verdixa</h2><span>Code. Execute. Evolve.</span></div></button>
      <UserNavigation active="dashboard" />
    </header>
    <main className="dashboard-container">
      <section className="dashboard-hero"><div><p className="hero-eyebrow">Welcome back</p><h1>Keep building,<span> {username}.</span></h1><p className="hero-description">Sharpen your problem-solving skills and master algorithms one challenge at a time.</p></div><div className="hero-icon"><Code2 size={76} strokeWidth={1.3} /></div></section>
      <section className="stats-grid">
        <div className="stat-card"><div className="stat-icon"><BookOpen size={22} /></div><div><span>Total Problems</span><strong>{loading || totalProblems === null ? "—" : totalProblems}</strong></div></div>
        <div className="stat-card"><div className="stat-icon"><CheckCircle2 size={22} /></div><div><span>Solved</span><strong>{loading || !progress ? "—" : progress.solvedProblems}</strong></div></div>
        <div className="stat-card"><div className="stat-icon"><Flame size={22} /></div><div><span>Current Streak</span><strong>{loading || !progress ? "—" : `${progress.currentStreak} days`}</strong></div></div>
        <div className="stat-card"><div className="stat-icon"><Filter size={22} /></div><div><span>Completion</span><strong>{loading || !progress ? "—" : `${progress.completionPercentage}%`}</strong></div></div>
      </section>
      <section className="problems-section"><div className="section-heading"><div><p className="section-eyebrow">PRACTICE</p><h2>Problem Library</h2><p>Choose a problem and start coding.</p></div><button className="refresh-button" onClick={fetchDashboard} disabled={loading}>{loading ? "Loading..." : "Refresh"}</button></div>
        <div className="filter-bar"><div className="search-box"><Search size={19} /><input type="text" placeholder="Search problems or tags..." value={search} onChange={(event) => setSearch(event.target.value)} /></div><div className="difficulty-filter"><Filter size={18} />{["ALL", "EASY", "MEDIUM", "HARD"].map((level) => <button key={level} className={`${difficulty === level ? "active" : ""} ${level.toLowerCase()}`} onClick={() => setDifficulty(level)}>{level === "ALL" ? "All" : level[0] + level.slice(1).toLowerCase()}</button>)}</div><button className={`bookmark-filter ${showBookmarked ? "active" : ""}`} onClick={() => setShowBookmarked((value) => !value)}><Bookmark size={16} /> Bookmarked</button><select className="topic-filter" value={topic} onChange={(event) => setTopic(event.target.value)}><option value="ALL">All topics</option>{topics.map((item) => <option value={item} key={item}>{item}</option>)}</select><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="ALL">All statuses</option><option value="SOLVED">Solved</option><option value="ATTEMPTED">Attempted</option><option value="UNATTEMPTED">Unattempted</option></select><select value={sort} onChange={(event) => setSort(event.target.value)}><option value="title">Title</option><option value="difficulty">Difficulty</option><option value="acceptanceRate">Acceptance rate</option></select></div>
        <div className="difficulty-summary"><span><b>{counts.EASY || 0}</b> Easy</span><span><b>{counts.MEDIUM || 0}</b> Medium</span><span><b>{counts.HARD || 0}</b> Hard</span><span className="results-count">{filteredProblems.length} result{filteredProblems.length !== 1 ? "s" : ""}</span></div>
        {error && <div className="dashboard-error"><p>{error}</p><button className="secondary-button" onClick={fetchDashboard} disabled={loading}>Retry</button></div>}
        {!error && progressError && <div className="dashboard-error"><p>{progressError} Problem data is still available.</p><button className="secondary-button" onClick={fetchDashboard} disabled={loading}>Retry</button></div>}
        {!error && bookmarkError && <div className="dashboard-error"><p>{bookmarkError} Problem data is still available.</p><button className="secondary-button" onClick={fetchDashboard} disabled={loading}>Retry</button></div>}
        {loading && <div className="empty-state"><div className="loading-spinner" /><p>Loading your dashboard...</p></div>}
        {!loading && !error && filteredProblems.length === 0 && <div className="empty-state"><BookOpen size={42} /><h3>No problems found</h3><p>Try changing your search, difficulty, or topic filter.</p></div>}
        {!loading && !error && filteredProblems.length > 0 && <div className="problem-list">{filteredProblems.map((problem, index) => { const solved = solvedProblemIds.has(problem.id); const bookmarked = bookmarkedProblemIds.has(problem.id); return <div className={solved ? "problem-card solved" : "problem-card"} key={problem.id} onClick={() => navigate(`/problems/${problem.id}`)}><div className="problem-number">{solved ? <CheckCircle2 size={20} /> : String(index + 1).padStart(2, "0")}</div><div className="problem-main"><div className="problem-title-row"><h3>{problem.title}</h3>{solved && <span className="solved-badge">Solved</span>}<span className={`difficulty-badge ${problem.difficulty?.toLowerCase()}`}>{problem.difficulty}</span></div><p>{problem.description}</p>{problem.tags && <div className="problem-tags">{problem.tags.split(",").slice(0, 4).map((tag) => <span key={tag}>{tag.trim()}</span>)}</div>}</div><button className={`problem-bookmark ${bookmarked ? "active" : ""}`} onClick={(event) => toggleBookmark(event, problem.id)} aria-label={bookmarked ? "Remove bookmark" : "Bookmark problem"}><Bookmark size={18} fill={bookmarked ? "currentColor" : "none"} /></button><div className="problem-arrow"><ChevronRight size={22} /></div></div>; })}</div>}
      </section>
    </main>
    <footer className="dashboard-footer"><span>© 2026 Verdixa</span><span>Practice • Learn • Master</span></footer>
  </div>;
}

export default Dashboard;
