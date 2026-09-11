import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Expand, ShieldCheck, Timer } from "lucide-react";
import api from "../services/api";
import { ErrorState, LoadingState } from "../components/PageState";

export default function ContestLaunch() {
  const { id } = useParams(); const navigate = useNavigate();
  const [contest, setContest] = useState(null); const [problems, setProblems] = useState([]); const [error, setError] = useState("");
  const load = async () => { try { setError(""); const contestResponse = await api.get(`/contests/${id}`); setContest(contestResponse.data); if (!contestResponse.data.registered || contestResponse.data.status !== "LIVE") return; const problemsResponse = await api.get(`/contests/${id}/problems`); setProblems(problemsResponse.data || []); } catch (reason) { setError(reason.response?.data?.message || "The contest workspace could not be prepared."); } };
  useEffect(() => { load(); }, [id]);
  const start = async () => { try { if (document.documentElement.requestFullscreen) await document.documentElement.requestFullscreen(); } catch { /* Browsers can decline fullscreen; the workspace remains usable. */ } navigate(`/problems/${problems[0].problemId}?contest=${id}&workspace=1`); };
  if (!contest && !error) return <main className="contest-launch"><LoadingState label="Preparing secure workspace"/></main>;
  if (error) return <main className="contest-launch"><ErrorState message={error} onRetry={load}/></main>;
  if (contest.status !== "LIVE") return <main className="contest-launch"><section className="contest-launch-card"><Timer size={27}/><p className="vx-eyebrow">Contest workspace</p><h1>{contest.status === "ENDED" ? "This contest has finished" : "The workspace is not open yet"}</h1><p>{contest.status === "ENDED" ? "Your scored submissions and final standing are available from the contest results." : `The workspace opens at ${new Date(contest.startAt).toLocaleString()}.`}</p><button className="secondary-button" onClick={() => navigate(`/contests/${id}`)}>Back to contest</button></section></main>;
  return <main className="contest-launch"><section className="contest-launch-card"><ShieldCheck size={28}/><p className="vx-eyebrow">Contest focus mode</p><h1>Ready when you are.</h1><p>Fullscreen focus mode keeps the contest timer, problem tabs, and submit history in one workspace. Timing and scoring remain enforced by the server.</p><ul><li><Expand size={15}/> Fullscreen workspace</li><li><Timer size={15}/> Server-timed contest window</li><li><ShieldCheck size={15}/> No hints, editorials, or personal notes during the contest</li></ul><button className="primary-button" disabled={!problems.length} onClick={start}>{problems.length ? "Start contest" : "No problems available"}</button><button className="contest-launch-back" onClick={() => navigate(`/contests/${id}`)}>Return to contest</button></section></main>;
}
