import { useCallback, useEffect, useMemo, useState } from "react";
import { Search, Users } from "lucide-react";
import api from "../services/api";
import Avatar from "../components/Avatar";
import UserShell from "../components/UserShell";
import Pagination from "../components/Pagination";
import { EmptyState, ErrorState, LoadingState } from "../components/PageState";

export default function Leaderboard() {
  const username = localStorage.getItem("algosphere_username");
  const [entries, setEntries] = useState([]); const [pagination, setPagination] = useState(null);
  const [page, setPage] = useState(0); const [query, setQuery] = useState(""); const [loading, setLoading] = useState(true); const [error, setError] = useState("");
  const load = useCallback(() => { setLoading(true); setError(""); api.get("/users/leaderboard/page", { params: { page, size: 20 } }).then((response) => { setEntries(response.data.content || []); setPagination(response.data); }).catch((reason) => setError(reason.response?.status === 401 ? "Your session expired. Sign in again to view current rankings." : "We couldn't retrieve current rankings." )).finally(() => setLoading(false)); }, [page]);
  useEffect(() => { load(); }, [load]);
  const visible = useMemo(() => entries.filter((entry) => entry.username?.toLowerCase().includes(query.toLowerCase())), [entries, query]);
  const self = entries.find((entry) => entry.username === username); const top = entries.filter((entry) => entry.rank <= 3).slice(0,3);
  return <UserShell context="Community">
    <header className="vx-page-intro"><div><p className="vx-eyebrow">Community performance</p><h1>Ranks built on<br/>accepted work.</h1><p>Distinct solved problems determine position; repeated accepted submissions never inflate the solved count.</p></div><aside className="vx-page-aside">Global field<br/>Persisted submissions<br/>Deterministic ranking</aside></header>
    {!loading && !error && entries.length > 0 && <section className="vx-leader-feature" aria-label="Leaderboard summary">
      <div className="vx-self-rank"><span>Your position</span><strong>{self ? `#${self.rank}` : "—"}</strong><p>{self ? `${self.solvedProblems} distinct problems solved with ${self.acceptanceRate}% acceptance.` : "Your ranking may be on another page."}</p></div>
      <div className="vx-top-three">{top.map((entry) => <article className="vx-top-performer" key={entry.userId}><span className={`vx-place vx-place--${entry.rank}`}>POSITION {String(entry.rank).padStart(2,"0")}</span><Avatar name={entry.username}/><b>{entry.username}</b><small>{entry.solvedProblems} solved · {entry.acceptanceRate}% accepted</small></article>)}</div>
    </section>}
    <section aria-labelledby="ranking-field-title"><div className="vx-library-header"><div><span className="vx-section-meta">Ranking field</span><h2 className="vx-section-title" id="ranking-field-title">All competitors</h2></div><label className="vx-search"><Search size={15}/><span className="sr-only">Search this ranking page</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Find a competitor"/></label></div>
      {error && <ErrorState message={error} onRetry={load}/>} {loading && <LoadingState label="Loading ranking field"/>}
      {!loading && !error && !visible.length && <EmptyState title={query ? "No matching competitor" : "No ranked users yet"} icon={Users}>{query ? "Try another name." : "Accepted solutions will establish the first ranking."}</EmptyState>}
      {!loading && !error && visible.length > 0 && <div className="vx-data-table" role="table" aria-label="Global leaderboard"><div className="vx-data-header" role="row"><span>Rank</span><span>Competitor</span><span>Solved</span><span>Accepted</span><span>Attempts</span><span>Rate</span></div>{visible.map((entry) => <div className={`vx-data-row ${entry.username === username ? "is-self" : ""}`} role="row" key={entry.userId}><span className="vx-rank-number">#{entry.rank}</span><span className="vx-data-user"><Avatar name={entry.username}/><strong>{entry.username}</strong>{entry.username === username && <i className="you-badge">You</i>}</span><span>{entry.solvedProblems}</span><span>{entry.acceptedSubmissionCount}</span><span>{entry.submissionCount}</span><span>{entry.acceptanceRate}%</span></div>)}</div>}
      {!loading && !error && <Pagination data={pagination} onChange={setPage}/>}
    </section>
  </UserShell>;
}
