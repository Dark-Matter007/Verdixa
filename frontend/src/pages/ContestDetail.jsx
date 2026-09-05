import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api from "../services/api";
import UserShell from "../components/UserShell";
import StatusPill from "../components/StatusPill";
import { ErrorState, LoadingState } from "../components/PageState";

export default function ContestDetail() {
  const { id } = useParams(); const username = localStorage.getItem("algosphere_username");
  const [contest, setContest] = useState(null); const [problems, setProblems] = useState([]); const [standings, setStandings] = useState([]); const [error, setError] = useState("");
  const load = useCallback(async () => { setError(""); try { const response = await api.get(`/contests/${id}`); setContest(response.data); if (response.data.registered && response.data.status !== "UPCOMING") { const [problemResponse, standingResponse] = await Promise.all([api.get(`/contests/${id}/problems`), api.get(`/contests/${id}/leaderboard`)]); setProblems(problemResponse.data || []); setStandings(standingResponse.data || []); } else { api.get(`/contests/${id}/leaderboard`).then((value) => setStandings(value.data || [])).catch(() => setStandings([])); } } catch (reason) { setError(reason.response?.data?.message || "This contest workspace is unavailable."); } }, [id]);
  useEffect(() => { load(); const timer = setInterval(load, 30000); return () => clearInterval(timer); }, [load]);
  const register = async () => { try { await api.post(`/contests/${id}/register`, {}); load(); } catch (reason) { setError(reason.response?.data?.message || "Registration could not be completed."); } };
  if (!contest && !error) return <UserShell context="Contest arena"><LoadingState label="Opening contest arena"/></UserShell>;
  return <UserShell context="Contest arena">{error && <ErrorState message={error} onRetry={load}/>} {contest && <>
    <header className="vx-arena-head"><div><StatusPill value={contest.status}/><p className="vx-eyebrow">Contest / {contest.visibility}</p><h1>{contest.title}</h1><p>{contest.description}</p></div><aside><span>Window</span><strong>{new Date(contest.startAt).toLocaleString()}</strong><small>until {new Date(contest.endAt).toLocaleString()}</small>{!contest.registered && contest.status !== "ENDED" && <button className="primary-button" onClick={register}>Register</button>}</aside></header>
    <div className="vx-arena-grid"><section><div className="vx-library-header"><div><span className="vx-section-meta">Contest set</span><h2 className="vx-section-title">Problems</h2></div><span className="vx-section-meta">{problems.length} visible</span></div>{contest.registered ? <div className="vx-arena-problems">{problems.map((problem, index) => <Link key={problem.id} to={`/problems/${problem.problemId}?contest=${id}`}><b>{String.fromCharCode(65 + (problem.displayOrder || index + 1) - 1)}</b><span><strong>{problem.title}</strong><small>{problem.points} points</small></span><StatusPill value={problem.status || "UNOPENED"}/></Link>)}</div> : <div className="empty-state"><h3>Registration required</h3><p>The problem set remains sealed until registration and contest start.</p></div>}</section>
      <aside className="vx-standings-rail"><span className="vx-section-meta">Live field</span><h2>Standings</h2>{standings.length ? standings.slice(0,10).map((entry) => <div className={entry.username === username ? "is-self" : ""} key={entry.userId}><span>#{entry.rank}</span><strong>{entry.username}</strong><small>{entry.solved} solved · {entry.penalty} penalty</small></div>) : <p>No scored submissions yet.</p>}</aside></div>
  </>}</UserShell>;
}
