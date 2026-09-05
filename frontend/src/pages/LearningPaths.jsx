import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../services/api";
import UserShell from "../components/UserShell";
import StatusPill from "../components/StatusPill";
import { EmptyState, ErrorState, LoadingState } from "../components/PageState";

export default function LearningPaths() {
  const [paths, setPaths] = useState([]); const [loading, setLoading] = useState(true); const [error, setError] = useState("");
  const load = useCallback(() => { setLoading(true); setError(""); api.get("/learning-paths").then((response) => setPaths(response.data || [])).catch(() => setError("The learning curriculum could not be loaded.")).finally(() => setLoading(false)); }, []);
  useEffect(() => { load(); }, [load]);
  return <UserShell context="Curriculum"><header className="vx-page-intro"><div><p className="vx-eyebrow">Structured mastery</p><h1>Learning,<br/>with direction.</h1><p>Published curricula organize real Verdixa problems into deliberate sequences. Progress follows your accepted solutions.</p></div><aside className="vx-page-aside">Ordered milestones<br/>Topic progression<br/>Real completion state</aside></header>
    {error && <ErrorState message={error} onRetry={load}/>} {loading && <LoadingState label="Loading learning paths"/>} {!loading && !error && !paths.length && <EmptyState title="No paths published yet">New curricula will appear here when they are ready.</EmptyState>}
    {!loading && !error && !!paths.length && <div className="vx-timeline">{paths.map((path) => <article className="vx-track" key={path.id}><span className="vx-section-meta">{(path.sections || []).length} modules</span><h2>{path.title}</h2><p>{path.description}</p>{(path.sections || []).map((section, sectionIndex) => <section className="vx-track-section" key={section.id}><h3>{String(sectionIndex + 1).padStart(2,"0")} / {section.title}</h3><ol>{(section.items || []).map((item) => <li key={item.id}><Link to={`/problems/${item.problem.id}`}>{item.problem.title}</Link><StatusPill value={item.problem.difficulty}/></li>)}</ol></section>)}</article>)}</div>}
  </UserShell>;
}
