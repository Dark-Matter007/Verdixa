import { useState } from "react";
import { LockKeyhole, BookOpen } from "lucide-react";
import api from "../services/api";
import EditorialContent from "./EditorialContent";
export default function EditorialPanel({ problemId, access, onChange }) {
  const [busy,setBusy]=useState(false); const [error,setError]=useState("");
  const reveal=async()=>{setBusy(true);setError("");try{const response=await api.post(`/problems/${problemId}/editorial/reveal`);onChange(response.data);}catch(e){setError(e.response?.data?.message||"Unable to reveal editorial. Please retry.");}finally{setBusy(false);}};
  if (!access) return <p>Editorial status is unavailable. Refresh to retry.</p>;
  if (!access.published) return <p>No editorial has been published yet.</p>;
  if (access.status === "REVEALED") return <EditorialContent editorial={access.editorial}/>;
  return <section className="editorial-access" aria-live="polite">
    {access.status === "AVAILABLE" ? <><BookOpen size={26}/><h2>Editorial Available</h2><p>Spoiler warning: revealing the editorial will show the full approach and reference solutions.</p><button className="submit-button" disabled={busy} onClick={reveal}>{busy ? "Revealing…" : "Reveal Editorial"}</button></> : <><LockKeyhole size={26}/><h2>Editorial Locked</h2><p>{access.failedSubmissionCount} / {access.requiredFailedSubmissions} failed submissions</p><progress aria-label="Editorial unlock progress" max={access.requiredFailedSubmissions} value={Math.min(access.failedSubmissionCount,access.requiredFailedSubmissions)}/><p>Available after an Accepted solution or 3 qualifying failed official submissions for this problem. Run Code does not count. Compilation errors count, consistent with hints.</p></>}
    {error && <p role="alert">{error}</p>}
  </section>;
}
