import {useCallback, useEffect, useRef, useState} from "react";
import {Activity, Award, RefreshCw, ShieldCheck, Signal, UsersRound} from "lucide-react";
import {useParams} from "react-router-dom";
import api from "../services/api";
import UserShell from "../components/UserShell";
import WorkspacePageHeader from "../components/WorkspacePageHeader";
import {ErrorState, LoadingState} from "../components/PageState";

const participantMetrics = [
  ["Invited", "invited"], ["Verified", "verified"], ["Started", "started"], ["Active", "active"], ["Completed", "completed"],
];
const outcomeMetrics = [
  ["Terminated", "terminated"], ["Absent", "absent"], ["Average score", "averageScore"], ["Highest score", "highestScore"], ["Proctor events", "proctorEvents"],
];

function MetricStrip({items, data, className = ""}) {
  return <div className={`vx-assessment-metric-strip ${className}`}>{items.map(([label, key]) => <div key={key}><span>{label}</span><strong>{data[key] ?? 0}</strong></div>)}</div>;
}

export default function AssessmentAnalytics() {
  const {id} = useParams();
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [updated, setUpdated] = useState(null);
  const running = useRef(false);
  const load = useCallback(async () => {
    if (running.current) return;
    running.current = true;
    try {
      const response = await api.get(`/assessments/${id}/analytics`);
      setData(response.data);
      setUpdated(new Date());
      setError("");
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Assessment analytics could not be loaded.");
    } finally {
      running.current = false;
    }
  }, [id]);

  useEffect(() => {
    load();
    const timer = setInterval(() => !document.hidden && load(), 15000);
    return () => clearInterval(timer);
  }, [load]);

  const leaderboard = data?.leaderboard || [];
  return <UserShell context="Assessment analytics" headerless>
    <WorkspacePageHeader eyebrow="Creator intelligence" title="Assessment analytics" description="Live participation, scoring, and proctor activity derived from persisted assessment records." actions={<button type="button" className="vx-secondary-action" onClick={load}><RefreshCw size={15}/>Refresh</button>}/>
    {error && <ErrorState message={error} onRetry={load}/>} {!data && !error && <LoadingState label="Aggregating assessment records"/>}
    {data && <main className="vx-assessment-analytics-editorial">
      <div className="vx-assessment-live-state"><span><Signal size={15}/>Live assessment state</span><small>Last updated {updated?.toLocaleTimeString()}</small></div>
      <section className="vx-assessment-analytics-section"><header><div><span>Participation flow</span><h2>From invitation to completion</h2></div><UsersRound size={20}/></header><MetricStrip data={data} items={participantMetrics}/></section>
      <section className="vx-assessment-analytics-overview"><div className="vx-assessment-score-story"><span>Completion signal</span><strong>{data.completed ?? 0}</strong><p>completed submission{data.completed === 1 ? "" : "s"}</p><small>Live completion is calculated from active assessment sessions.</small></div><div className="vx-assessment-analytics-side"><div><Activity size={18}/><span>Currently active</span><strong>{data.active ?? 0}</strong><small>participant{data.active === 1 ? "" : "s"} in session</small></div><div><ShieldCheck size={18}/><span>Proctor events</span><strong>{data.proctorEvents ?? 0}</strong><small>recorded policy signals</small></div></div></section>
      <section className="vx-assessment-analytics-section"><header><div><span>Outcomes & oversight</span><h2>Score and session health</h2></div><Award size={20}/></header><MetricStrip data={data} items={outcomeMetrics} className="vx-assessment-outcome-strip"/></section>
      <section className="vx-assessment-leaderboard-editorial"><header><div><span>Participant ranking</span><h2>Leaderboard</h2><p>Scores update as completed work is persisted.</p></div><span>{leaderboard.length} ranked</span></header>{leaderboard.length ? <div className="vx-assessment-leaderboard-table"><div className="vx-assessment-leaderboard-head"><span>Rank</span><span>Participant</span><span>Score</span><span>Status</span></div>{leaderboard.map((row, index) => <div className="vx-assessment-leaderboard-row" key={row.id}><strong>{String(index + 1).padStart(2, "0")}</strong><span>{row.participant}</span><b>{row.score}</b><i>{row.status}</i></div>)}</div> : <div className="vx-assessment-leaderboard-empty"><Award size={22}/><div><strong>No participant has started yet</strong><p>Rankings will appear here as participants begin and submit their assessment work.</p></div></div>}</section>
    </main>}
  </UserShell>;
}
