import { useCallback, useEffect, useMemo, useState } from "react";
import { Search } from "lucide-react";
import { useNavigate } from "react-router-dom";
import api from "../services/api";
import UserShell from "../components/UserShell";
import StatusPill from "../components/StatusPill";
import Pagination from "../components/Pagination";
import { EmptyState, ErrorState, LoadingState } from "../components/PageState";

export default function SubmissionHistory() {
  const navigate = useNavigate(); const [items, setItems] = useState([]); const [page, setPage] = useState(0); const [pagination, setPagination] = useState(null);
  const [query, setQuery] = useState(""); const [verdict, setVerdict] = useState("ALL"); const [language, setLanguage] = useState("ALL"); const [loading, setLoading] = useState(true); const [error, setError] = useState("");
  const load = useCallback(async () => { setLoading(true); setError(""); try { const user = await api.get("/users/me"); const response = await api.get(`/submissions/user/${user.data.id}/page`, { params: { page, size: 20 } }); setItems(response.data.content || []); setPagination(response.data); } catch { setError("We couldn't retrieve your submission record."); } finally { setLoading(false); } }, [page]);
  useEffect(() => { load(); }, [load]);
  const languages = useMemo(() => [...new Set(items.map((item) => item.language).filter(Boolean))], [items]);
  const visible = items.filter((item) => (item.problemTitle || "").toLowerCase().includes(query.toLowerCase()) && (verdict === "ALL" || item.status === verdict) && (language === "ALL" || item.language === language));
  return <UserShell context="Activity"><header className="vx-page-intro"><div><p className="vx-eyebrow">Execution record</p><h1>Every attempt,<br/>in sequence.</h1><p>A precise record of practice and contest submissions, verdicts, runtimes, and test coverage.</p></div><aside className="vx-page-aside">Chronological ledger<br/>Persisted judge results<br/>Replayable source</aside></header>
    <div className="vx-filter-deck"><label className="vx-search"><Search size={15}/><span className="sr-only">Search submissions</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Find a problem"/></label><select aria-label="Verdict" value={verdict} onChange={(event) => setVerdict(event.target.value)}><option value="ALL">All verdicts</option><option value="ACCEPTED">Accepted</option><option value="WRONG_ANSWER">Wrong answer</option><option value="COMPILATION_ERROR">Compilation error</option><option value="RUNTIME_ERROR">Runtime error</option><option value="TIME_LIMIT_EXCEEDED">Timeout</option></select><select aria-label="Language" value={language} onChange={(event) => setLanguage(event.target.value)}><option value="ALL">All languages</option>{languages.map((item) => <option key={item}>{item}</option>)}</select></div>
    {error && <ErrorState message={error} onRetry={load}/>} {loading && <LoadingState label="Loading submission record"/>} {!loading && !error && !visible.length && <EmptyState title="No submissions in this view">Your next judge result will appear here.</EmptyState>}
    {!loading && !error && visible.length > 0 && <div className="vx-data-table"><div className="vx-history-header"><span>Verdict</span><span>Problem / context</span><span>Language</span><span>Runtime</span><span>Tests</span><span>Submitted</span></div>{visible.map((item) => <article className="vx-history-row" key={item.id} tabIndex="0" onClick={() => navigate(`/submissions/${item.id}`)} onKeyDown={(event) => event.key === "Enter" && navigate(`/submissions/${item.id}`)}><StatusPill value={item.status}/><strong>{item.problemTitle}<small>{item.contestId ? "Contest" : "Practice"}</small></strong><span>{item.language?.toUpperCase()}</span><span>{item.executionTimeMs ?? 0} ms</span><span>{item.passedTestCases}/{item.totalTestCases}</span><time>{new Date(item.submittedAt).toLocaleString()}</time></article>)}</div>}
    {!loading && !error && <Pagination data={pagination} onChange={setPage}/>}</UserShell>;
}
