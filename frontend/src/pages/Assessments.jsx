import {useEffect, useMemo, useState} from "react";
import {ArrowRight, Building2, CalendarClock, ClipboardCheck, Clock3, Lock, Plus, Users} from "lucide-react";
import {Link} from "react-router-dom";
import api from "../services/api";
import UserShell from "../components/UserShell";
import WorkspacePageHeader from "../components/WorkspacePageHeader";
import StatusPill from "../components/StatusPill";
import {ErrorState, LoadingState} from "../components/PageState";

const excerpt = value => value ? value.length > 170 ? `${value.slice(0, 167)}…` : value : "A hosted coding assessment from the Verdixa workspace.";
const duration = assessment => assessment.startAt && assessment.endAt ? Math.max(0, Math.round((new Date(assessment.endAt) - new Date(assessment.startAt)) / 60000)) : null;

export default function Assessments() {
  const [items, setItems] = useState(null);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState("ALL");
  useEffect(() => { api.get("/assessments").then(response => setItems(response.data)).catch(requestError => setError(requestError.response?.data?.message || "Assessments could not be loaded.")); }, []);
  const visible = useMemo(() => (items || []).filter(item => filter === "ALL" || item.status === filter || (filter === "PRIVATE" && item.visibility === "PRIVATE")), [filter, items]);
  return <UserShell context="Assessments" headerless>
    <WorkspacePageHeader eyebrow="Hosted coding evaluations" title="Assessments" description="Discover scheduled coding evaluations and private invitations from your Verdixa workspace." actions={<Link className="vx-primary-action" to="/assessments/studio"><Plus size={16}/><span>Assessment Studio</span><ArrowRight size={16}/></Link>} />
    {error && <ErrorState message={error}/>} {!items && !error && <LoadingState label="Loading assessments"/>}
    {items && <main className="vx-assessment-index">
      <section className="vx-assessment-index-head"><div><span>Assessment board</span><h2>{items.length} available evaluation{items.length === 1 ? "" : "s"}</h2></div><nav aria-label="Filter assessments">{[["ALL", "All"], ["PRIVATE", "Private"], ["UPCOMING", "Upcoming"], ["LIVE", "Live"]].map(([value, copy]) => <button type="button" className={filter === value ? "active" : ""} onClick={() => setFilter(value)} key={value}>{copy}</button>)}</nav></section>
      {visible.length > 0 ? <div className="vx-assessment-index-list">{visible.map((assessment, index) => <Link className="vx-assessment-index-item" to={`/assessments/${assessment.id}`} key={assessment.id}><span className="vx-assessment-index-number">{String(index + 1).padStart(2, "0")}</span><div className="vx-assessment-index-summary"><header><div><StatusPill value={assessment.status}/>{assessment.visibility === "PRIVATE" && <span className="vx-private-marker"><Lock size={12}/> Private invitation</span>}</div><ArrowRight size={18}/></header><h2>{assessment.title}</h2><p>{excerpt(assessment.description)}</p></div><dl><div><dt><Building2 size={13}/> Organization</dt><dd>{assessment.organization}</dd></div><div><dt><CalendarClock size={13}/> Starts</dt><dd>{assessment.startAt ? new Date(assessment.startAt).toLocaleString() : "Not scheduled"}</dd></div><div><dt><Clock3 size={13}/> Duration</dt><dd>{duration(assessment) ? `${duration(assessment)} min` : "—"}</dd></div><div><dt><Users size={13}/> Participation</dt><dd>{assessment.participantCount || 0} registered</dd></div></dl><footer><span>Hosted by <b>{assessment.host}</b></span><span>Open assessment <ArrowRight size={14}/></span></footer></Link>)}</div> : <section className="vx-assessment-empty"><ClipboardCheck size={30}/><span>Nothing scheduled yet</span><h2>{filter === "ALL" ? "No assessments available" : "No assessments match this view"}</h2><p>Public assessments and private invitations will appear here as they become available.</p><Link className="vx-primary-action" to="/assessments/studio"><Plus size={16}/><span>Open Assessment Studio</span><ArrowRight size={16}/></Link></section>}
    </main>}
  </UserShell>;
}
