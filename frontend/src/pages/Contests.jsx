import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import api from "../services/api";
import UserShell from "../components/UserShell";
import StatusPill from "../components/StatusPill";
import { EmptyState, ErrorState, LoadingState } from "../components/PageState";

export default function Contests() {
  const [items, setItems] = useState([]); const [tab, setTab] = useState("LIVE"); const [loading, setLoading] = useState(true); const [error, setError] = useState("");
  const load = useCallback(() => { setLoading(true); setError(""); api.get("/contests").then((response) => setItems(response.data || [])).catch(() => setError("The competition schedule is temporarily unavailable.")).finally(() => setLoading(false)); }, []);
  useEffect(() => { load(); }, [load]);
  const rows = useMemo(() => items.filter((contest) => tab === "PAST" ? ["ENDED", "CANCELLED"].includes(contest.status) : contest.status === tab), [items, tab]);
  return <UserShell context="Competition">
    <section className="vx-competition-head"><div><p className="vx-eyebrow">Timed environments</p><h1 className="vx-display">Think clearly.<br/><em>Move deliberately.</em></h1><p className="vx-deck">Server-timed competitions with a shared judge, transparent scoring, and serious standings.</p></div><div className="vx-contest-switcher" role="tablist" aria-label="Contest state">{[["LIVE","Live now"],["UPCOMING","Upcoming"],["PAST","Past"]].map(([key,label]) => <button role="tab" aria-selected={tab === key} className={tab === key ? "active" : ""} key={key} onClick={() => setTab(key)}>{label}</button>)}</div></section>
    {error && <ErrorState message={error} onRetry={load}/>} {loading && <LoadingState label="Loading competition schedule"/>}
    {!loading && !error && !rows.length && <EmptyState title={`No ${tab.toLowerCase()} contests`}>The next persisted event will appear in this field.</EmptyState>}
    {!loading && !error && rows.length > 0 && <section className="vx-contest-list" aria-label={`${tab.toLowerCase()} contests`}>{rows.map((contest) => <article className="vx-contest-entry" key={contest.id}><StatusPill value={contest.status}/><h2>{contest.title}</h2><p>{contest.description || "Rules and problem access are available from the contest workspace."}</p><div className="vx-contest-meta"><span>{new Date(contest.startAt).toLocaleString()}</span><span>→ {new Date(contest.endAt).toLocaleString()}</span><span>{contest.registrationCount} registered</span><span>{contest.visibility}</span></div><Link className="vx-text-link" to={`/contests/${contest.id}`}>{contest.status === "LIVE" ? "Enter arena" : contest.status === "UPCOMING" ? "View schedule" : "Review results"}</Link></article>)}</section>}
  </UserShell>;
}
