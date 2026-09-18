import {useEffect, useMemo, useState} from "react";
import {Check, Search, Send, X} from "lucide-react";
import {useParams} from "react-router-dom";
import api from "../services/api";
import UserShell from "../components/UserShell";
import WorkspacePageHeader from "../components/WorkspacePageHeader";
import StatusPill from "../components/StatusPill";
import {ErrorState, LoadingState} from "../components/PageState";

const FILTERS = ["ALL", "INVITED", "FORM_SUBMITTED", "VERIFIED", "ACTIVE", "COMPLETED", "TERMINATED"];
const label = value => String(value || "").replaceAll("_", " ").replace(/\b\w/g, letter => letter.toUpperCase());
const sessionLabel = value => value === "SUBMITTED" ? "COMPLETED" : value;
const initials = name => String(name || "?").slice(0, 1).toUpperCase();

export default function AssessmentParticipants() {
  const {id} = useParams();
  const [assessment, setAssessment] = useState(null);
  const [participants, setParticipants] = useState(null);
  const [query, setQuery] = useState("");
  const [emailInput, setEmailInput] = useState("");
  const [results, setResults] = useState([]);
  const [selected, setSelected] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [inviting, setInviting] = useState(false);

  const load = () => {
    setError("");
    return Promise.all([
      api.get(`/assessments/${id}/participants`),
      api.get(`/assessments/${id}`),
    ]).then(([people, item]) => {
      setParticipants(people.data);
      setAssessment(item.data);
    }).catch(requestError => setError(requestError.response?.data?.message || "Participants could not be loaded."));
  };

  useEffect(() => { load(); }, [id]);
  useEffect(() => {
    if (query.trim().length < 2 || assessment?.visibility !== "PRIVATE") { setResults([]); return undefined; }
    const timer = setTimeout(() => api.get("/assessments/invitee-search", {params:{query:query.trim()}})
      .then(response => setResults(response.data || []))
      .catch(() => setResults([])), 250);
    return () => clearTimeout(timer);
  }, [query, assessment?.visibility]);

  const toggle = user => setSelected(current => current.some(item => item.id === user.id) ? current.filter(item => item.id !== user.id) : [...current, user]);
  const invite = async () => {
    const emails = emailInput.split(/[\s,;]+/).map(value => value.trim()).filter(Boolean);
    if ((!selected.length && !emails.length) || inviting) return;
    setInviting(true); setError(""); setMessage("");
    try {
      const response = await api.post(`/assessments/${id}/invitations`, {userIds:selected.map(user => user.id), emails});
      const invited = response.data?.invited ?? selected.length + emails.length;
      const already = response.data?.alreadyInvited ?? 0;
      setMessage([invited ? `${invited} participant${invited === 1 ? "" : "s"} invited` : "No new invitations sent", already ? `Already invited: ${already}` : ""].filter(Boolean).join(" · "));
      setSelected([]); setQuery(""); setEmailInput(""); setResults([]);
      await load();
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Invitations could not be sent. Please try again.");
    } finally { setInviting(false); }
  };
  const remove = async userId => {
    setError("");
    try { await api.delete(`/assessments/${id}/invitations/${userId}`); await load(); }
    catch (requestError) { setError(requestError.response?.data?.message || "This invitee cannot be removed safely."); }
  };

  const filtered = useMemo(() => (participants || []).filter(person => filter === "ALL" || (filter === "COMPLETED" ? person.status === "SUBMITTED" : filter === "ACTIVE" ? person.status === "ACTIVE" : person.status === filter)), [filter, participants]);
  const metrics = useMemo(() => {
    const all = participants || [];
    return {invited:all.filter(person => ["INVITED", "FORM_SUBMITTED", "VERIFIED", "ACTIVE", "SUBMITTED", "TERMINATED"].includes(person.status)).length, forms:all.filter(person => person.status !== "INVITED").length, verified:all.filter(person => ["VERIFIED", "ACTIVE", "SUBMITTED", "TERMINATED"].includes(person.status)).length, started:all.filter(person => ["ACTIVE", "SUBMITTED", "TERMINATED"].includes(person.status)).length, completed:all.filter(person => person.status === "SUBMITTED").length};
  }, [participants]);

  return <UserShell context="Assessment participants" headerless>
    <WorkspacePageHeader eyebrow="Assessment participants" title={assessment?.title || "Participants"} description="Manage private access and follow the participation journey without exposing a user directory." />
    {error && <ErrorState message={error}/>} {!assessment && !error && <LoadingState label="Loading participant workspace"/>}
    {assessment && <main className="vx-participant-workspace">
      <header className="vx-participant-heading"><div><span>Participants</span><h2>{assessment.title}</h2></div><div><StatusPill value={assessment.visibility}/><StatusPill value={assessment.status}/></div></header>
      <section className="vx-participant-metrics" aria-label="Participant summary"><div><span>Invited</span><strong>{metrics.invited}</strong></div><div><span>Forms received</span><strong>{metrics.forms}</strong></div><div><span>Verified</span><strong>{metrics.verified}</strong></div><div><span>Started</span><strong>{metrics.started}</strong></div><div><span>Completed</span><strong>{metrics.completed}</strong></div></section>
      {assessment.visibility === "PRIVATE" && <section className="vx-invitation-workbench"><header><div><span>Private access</span><h2>Invite participants</h2><p>Search existing users or enter external email addresses. Each person receives a unique secure access link.</p></div><span className="vx-selection-count">{selected.length} selected</span></header><label className="vx-search"><Search size={18}/><input value={query} onChange={event => setQuery(event.target.value)} placeholder="Search Verdixa users..." aria-label="Search Verdixa users"/></label><label className="vx-search"><Send size={18}/><input value={emailInput} onChange={event => setEmailInput(event.target.value)} placeholder="External email addresses, separated by commas" aria-label="External invitee emails"/></label>{results.length > 0 && <div className="vx-user-search-results">{results.map(user => <button type="button" className={selected.some(item => item.id === user.id) ? "selected" : ""} onClick={() => toggle(user)} key={user.id}><span className="vx-person-avatar">{initials(user.username)}</span><span><strong>{user.username}</strong><small>Verdixa user</small></span>{selected.some(item => item.id === user.id) && <Check size={17}/>}</button>)}</div>}{selected.length > 0 && <div className="vx-selected-tray">{selected.map(user => <button type="button" onClick={() => toggle(user)} key={user.id}><span className="vx-person-avatar">{initials(user.username)}</span>{user.username}<X size={14}/></button>)}</div>}{message && <p className="form-success" role="status">{message}</p>}<footer><small>Invitation links are hashed at rest. Existing invitations are never silently sent twice.</small><button type="button" className="vx-primary-action" disabled={(!selected.length && !emailInput.trim()) || inviting} onClick={invite}><Send size={15}/>{inviting ? "Inviting..." : "Send secure invitations"}</button></footer></section>}
      {participants && <section className="vx-participant-register"><header><div><span>Participation register</span><h2>{filtered.length} participant{filtered.length === 1 ? "" : "s"}</h2></div><nav aria-label="Filter participants">{FILTERS.map(value => <button type="button" onClick={() => setFilter(value)} className={filter === value ? "active" : ""} key={value}>{label(value)}</button>)}</nav></header><p className="vx-register-note">Submitted identity forms are visible only to this assessment’s host and remain separate from the public user directory.</p><div className="vx-participant-table" role="table"><div className="vx-participant-table-head" role="row"><span>Participant</span><span>Invitation</span><span>Email verification</span><span>Session</span><span>Solved</span><span>Score</span><span>Form details</span><span>Actions</span></div>{filtered.map(person => <div role="row" key={person.id}><span className="vx-table-person"><i>{initials(person.fullName || person.username)}</i><strong>{person.fullName || person.username}</strong><small>{person.email}</small>{person.fullName && person.fullName !== person.username && <small>@{person.username}</small>}</span><span>{person.status === "INVITED" ? <StatusPill value="INVITED"/> : "—"}</span><span>{person.status === "INVITED" ? "Pending" : person.status === "FORM_SUBMITTED" ? `Received ${person.formSubmittedAt ? new Date(person.formSubmittedAt).toLocaleString() : ""}` : person.verifiedAt ? new Date(person.verifiedAt).toLocaleString() : "Legacy access"}</span><span><StatusPill value={sessionLabel(person.status)}/></span><span>—</span><span>{person.score ?? 0}</span><span className="vx-participant-form-data">{person.participantReference && <strong>{person.participantReference}</strong>}{person.organization && <small>{person.organization}</small>}{!person.participantReference && !person.organization && "—"}</span><span>{assessment.visibility === "PRIVATE" && person.status === "INVITED" ? <button type="button" onClick={() => remove(person.id)}>Revoke</button> : "—"}</span></div>)}</div>{filtered.length === 0 && <p className="vx-participant-empty">No participants match this filter yet.</p>}</section>}
    </main>}
  </UserShell>;
}
