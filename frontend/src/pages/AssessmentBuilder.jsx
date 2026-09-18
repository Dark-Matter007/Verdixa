import {useEffect, useMemo, useRef, useState} from "react";
import {ArrowRight, Check, Clock3, LibraryBig, LockKeyhole, Search, ShieldCheck, X} from "lucide-react";
import {useNavigate, useParams} from "react-router-dom";
import api from "../services/api";
import UserShell from "../components/UserShell";
import WorkspacePageHeader from "../components/WorkspacePageHeader";

const initial = {title:"", organization:"", description:"", instructions:"", visibility:"PUBLIC", startAt:"", endAt:"", maxParticipants:100, fullscreenRequired:true, microphoneRequired:true, strictProctoring:true, problems:[], mcqs:[]};
const difficulties = ["ALL", "EASY", "MEDIUM", "HARD"];

function problemFromLibraryEntry(entry) {
  return entry?.problem || entry;
}

export default function AssessmentBuilder() {
  const {id} = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState(initial);
  const [problems, setProblems] = useState([]);
  const [query, setQuery] = useState("");
  const [difficulty, setDifficulty] = useState("ALL");
  const [catalogOpen, setCatalogOpen] = useState(false);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [catalogError, setCatalogError] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [busy, setBusy] = useState(false);
  const [inviteQuery, setInviteQuery] = useState("");
  const [inviteResults, setInviteResults] = useState([]);
  const [invitees, setInvitees] = useState([]);
  const [originalInviteeIds, setOriginalInviteeIds] = useState([]);
  const initializedAssessmentId = useRef(null);

  useEffect(() => {
    let mounted = true;
    async function loadCatalog() {
      setCatalogLoading(true);
      setCatalogError("");
      try {
        let page = 0;
        let pageCount = 1;
        const allProblems = [];
        do {
          const response = await api.get("/problems/library", {params:{page, size:100, sort:"title"}});
          const content = Array.isArray(response.data) ? response.data : response.data?.content || [];
          allProblems.push(...content.map(problemFromLibraryEntry).filter(Boolean));
          pageCount = Array.isArray(response.data) ? 1 : response.data?.totalPages || 1;
          page += 1;
        } while (page < pageCount);
        if (mounted) setProblems(allProblems);
      } catch (requestError) {
        if (mounted) setCatalogError(requestError.response?.data?.message || "The visible problem library could not be loaded.");
      } finally {
        if (mounted) setCatalogLoading(false);
      }
    }
    loadCatalog();
    return () => { mounted = false; };
  }, []);

  useEffect(() => {
    if (!id) return undefined;
    let mounted = true;
    api.get(`/assessments/${id}`).then(response => {
      if (!mounted) return;
      const assessment = response.data;
      if (initializedAssessmentId.current === id) return;
      initializedAssessmentId.current = id;
      setForm({...assessment, startAt:assessment.startAt?.slice(0, 16) || "", endAt:assessment.endAt?.slice(0, 16) || "", problems:(assessment.problems || []).map(problem => ({problemId:problem.id, points:problem.points})), mcqs:assessment.mcqs || []});
    }).catch(requestError => {
      if (mounted) setError(requestError.response?.data?.message || "Assessment could not be loaded.");
    });
    return () => { mounted = false; };
  }, [id]);

  useEffect(() => {
    if (!id) return;
    api.get(`/assessments/${id}/participants`).then(response => { const people=response.data.filter(person => ["INVITED", "VERIFIED", "ACTIVE", "SUBMITTED", "TERMINATED"].includes(person.status)).map(person => ({id:person.id, username:person.username})); setInvitees(people); setOriginalInviteeIds(people.map(person => person.id)); }).catch(() => {});
  }, [id]);
  useEffect(() => {
    if (form.visibility !== "PRIVATE" || inviteQuery.trim().length < 2) { setInviteResults([]); return; }
    const timer = setTimeout(() => api.get("/assessments/invitee-search", {params:{query:inviteQuery.trim()}}).then(response => setInviteResults(response.data)).catch(() => setInviteResults([])), 250);
    return () => clearTimeout(timer);
  }, [form.visibility, inviteQuery]);

  const visible = useMemo(() => problems.filter(problem => {
    const searchText = `${problem.title || ""} ${problem.tags || ""}`.toLowerCase();
    return searchText.includes(query.toLowerCase()) && (difficulty === "ALL" || problem.difficulty?.toUpperCase() === difficulty);
  }), [difficulty, problems, query]);
  const selected = problemId => form.problems.some(problem => problem.problemId === problemId);
  const selectedProblems = useMemo(() => form.problems.map(selection => problems.find(problem => problem.id === selection.problemId)).filter(Boolean), [form.problems, problems]);
  const toggle = problem => setForm(current => {
    const exists = current.problems.some(item => item.problemId === problem.id);
    return {...current, problems:exists ? current.problems.filter(item => item.problemId !== problem.id) : [...current.problems, {problemId:problem.id, points:100}]};
  });
  const addMcq=()=>setForm(current=>({...current,mcqs:[...(current.mcqs||[]),{question:"",options:["","","",""],correctOption:0,points:100}]}));
  const updateMcq=(index,field,value)=>setForm(current=>({...(current),mcqs:current.mcqs.map((question,questionIndex)=>questionIndex===index?{...question,[field]:value}:question)}));
  const updateOption=(questionIndex,optionIndex,value)=>setForm(current=>({...current,mcqs:current.mcqs.map((question,index)=>index===questionIndex?{...question,options:question.options.map((option,choiceIndex)=>choiceIndex===optionIndex?value:option)}:question)}));
  const removeMcq=index=>setForm(current=>({...current,mcqs:current.mcqs.filter((_,questionIndex)=>questionIndex!==index)}));
  const change = event => {
    const value = event.target.type === "checkbox" ? event.target.checked : event.target.value;
    if (event.target.name === "visibility" && form.visibility === "PRIVATE" && value === "PUBLIC" && invitees.length && !window.confirm("This assessment will become discoverable to all eligible Verdixa users. Your selected invitees will be retained, but private-only access ends when you publish.")) return;
    setForm(current => ({...current, [event.target.name]:value}));
  };
  const save = async event => {
    event.preventDefault();
    setBusy(true);
    setError("");
    setSuccess("");
    try {
      const body = {...form, startAt:form.startAt, endAt:form.endAt, registrationDeadline:null, maxParticipants:Number(form.maxParticipants), inviteeIds:form.visibility === "PRIVATE" ? invitees.map(person => person.id) : null};
      const result = id ? await api.put(`/assessments/${id}`, body) : await api.post("/assessments", body);
      if (id && form.visibility === "PRIVATE") await Promise.all(originalInviteeIds.filter(userId => !invitees.some(person => person.id === userId)).map(userId => api.delete(`/assessments/${result.data.id}/invitations/${userId}`)));
      if (!id && form.visibility === "PRIVATE" && invitees.length) await api.post(`/assessments/${result.data.id}/invitations`, {userIds:invitees.map(person => person.id)});
      setOriginalInviteeIds(invitees.map(person => person.id));
      setSuccess(id ? "Saved successfully. Your changes are now reflected in this assessment." : "Draft saved successfully. You can publish it from Assessment Studio when ready.");
      if (!id) navigate(`/assessments/studio/${result.data.id}/edit`, {replace:true});
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Assessment could not be saved.");
    } finally {
      setBusy(false);
    }
  };

  return <UserShell context="Assessment builder" headerless>
    <WorkspacePageHeader eyebrow={id ? "Assessment draft" : "Organization workspace"} title={id ? "Edit assessment" : "Create an assessment"} description="Set the brief, schedule secure participation, and choose judge-ready problems in one measured workflow."/>
    {error && <div className="form-error" role="alert">{error}</div>}
    {success && <div className="form-success" role="status">{success}</div>}
    <form className="vx-builder-workspace" onSubmit={save}>
      <aside className="vx-builder-rail"><div><span>Build flow</span><ol><li className="active"><b>01</b><div><strong>Assessment brief</strong><small>Define the evaluation.</small></div></li><li><b>02</b><div><strong>Schedule & access</strong><small>Set timing and capacity.</small></div></li><li><b>03</b><div><strong>Safeguards</strong><small>Choose participant rules.</small></div></li><li><b>04</b><div><strong>Participants</strong><small>Private access, if needed.</small></div></li><li><b>05</b><div><strong>Problem set</strong><small>Select the challenges.</small></div></li><li><b>06</b><div><strong>Review</strong><small>Verify before saving.</small></div></li></ol></div><div className="vx-builder-rail-note"><ShieldCheck size={19}/><strong>Server-validated</strong><p>Times, access, and selected problems are checked before an assessment is saved.</p></div></aside>
      <div className="vx-builder-form-stack">
        <section><header><span>01 / Assessment brief</span><h2>What are you evaluating?</h2><p>Give participants enough context before they enter a focused coding session.</p></header><div className="vx-form-grid"><label>Assessment title<input name="title" value={form.title} onChange={change} maxLength="180" required/></label><label>Organization<input name="organization" value={form.organization} onChange={change} maxLength="180" required/></label><label className="wide">Description<textarea name="description" value={form.description || ""} onChange={change} rows="4"/></label><label className="wide">Participant instructions<textarea name="instructions" value={form.instructions || ""} onChange={change} rows="4"/></label></div></section>
        <section><header><span>02 / Schedule & access</span><h2>Make participation deliberate</h2><p>Use a clear time window and choose who can discover the assessment.</p></header><div className="vx-form-grid"><div className="wide vx-visibility-choice" role="radiogroup" aria-label="Assessment visibility"><span>Visibility</span><label><input type="radio" name="visibility" value="PUBLIC" checked={form.visibility === "PUBLIC"} onChange={change}/><b>Public</b><small>Anyone with the link can verify their email and participate.</small></label><label><input type="radio" name="visibility" value="PRIVATE" checked={form.visibility === "PRIVATE"} onChange={change}/><b>Private</b><small>Only email addresses you invite can verify and participate.</small></label></div><label>Maximum participants<input name="maxParticipants" type="number" min="1" max="10000" value={form.maxParticipants} onChange={change}/></label><label>Starts<input name="startAt" type="datetime-local" value={form.startAt} onChange={change} required/></label><label>Ends<input name="endAt" type="datetime-local" value={form.endAt} onChange={change} required/></label></div>{form.visibility === "PRIVATE" && <div className="vx-invite-selector"><header><span>Invite participants</span><b>{invitees.length} selected</b></header><label className="vx-search"><Search size={16}/><input value={inviteQuery} onChange={event => setInviteQuery(event.target.value)} placeholder="Search users by username"/></label><div className="vx-invite-results">{inviteResults.filter(person => !invitees.some(selected => selected.id === person.id)).map(person => <button type="button" key={person.id} onClick={() => setInvitees(current => [...current, person])}>Add {person.username}</button>)}</div><div className="vx-invite-selected">{invitees.map(person => <button type="button" key={person.id} onClick={() => setInvitees(current => current.filter(selected => selected.id !== person.id))}>{person.username}<X size={14}/></button>)}</div><small>Private invitations are kept when you switch visibility until you explicitly remove them.</small></div>}</section>
        <section><header><span>03 / Safeguards</span><h2>Focused participation rules</h2><p>These checks are visible to participants before they begin.</p></header><div className="vx-builder-check-list"><label><input type="checkbox" name="fullscreenRequired" checked={form.fullscreenRequired} onChange={change}/><span><LockKeyhole size={17}/><strong>Require fullscreen</strong><small>Keep the assessment workspace in focus.</small></span></label><label><input type="checkbox" name="microphoneRequired" checked={form.microphoneRequired} onChange={change}/><span><Clock3 size={17}/><strong>Require microphone presence</strong><small>Checks an active stream; audio is never recorded.</small></span></label><label><input type="checkbox" name="strictProctoring" checked={form.strictProctoring} onChange={change}/><span><ShieldCheck size={17}/><strong>Use strict proctoring</strong><small>End a session on tab switch, fullscreen exit, or microphone loss.</small></span></label></div></section>
        <section className="vx-builder-problem-section"><header><span>04 / Problem set</span><h2>Build your challenge set</h2><p>Browse every active, judge-ready problem, select what fits, then return here with the set preserved.</p></header><div className="vx-problem-set-summary"><div><strong>{form.problems.length}</strong><span>selected problem{form.problems.length === 1 ? "" : "s"}</span></div><button type="button" className="vx-secondary-action" onClick={() => setCatalogOpen(true)}><LibraryBig size={16}/> Browse visible problems <ArrowRight size={15}/></button></div>{selectedProblems.length > 0 ? <div className="vx-builder-selected-list">{selectedProblems.map(problem => <button type="button" onClick={() => toggle(problem)} key={problem.id}><span><strong>{problem.title}</strong><small>{problem.difficulty}{problem.tags ? ` · ${problem.tags}` : ""}</small></span><X size={14} aria-label={`Remove ${problem.title}`}/></button>)}</div> : <div className="vx-builder-empty-selection"><LibraryBig size={21}/><div><strong>No problems selected yet</strong><p>Open the visible library to review, filter, and add challenges to this assessment.</p></div></div>}</section>
        <section className="vx-builder-problem-section"><header><span>05 / Multiple choice</span><h2>Add knowledge-check questions</h2><p>Mix coding challenges with scored MCQs. Correct answers remain server-side.</p></header><button type="button" className="vx-secondary-action" onClick={addMcq}>Add MCQ</button>{form.mcqs?.map((mcq,index)=><article className="vx-mcq-author" key={index}><header><strong>Question {index+1}</strong><button type="button" onClick={()=>removeMcq(index)}>Remove</button></header><label>Question<textarea value={mcq.question} onChange={event=>updateMcq(index,"question",event.target.value)} rows="3" required/></label><div className="vx-form-grid">{mcq.options.map((option,optionIndex)=><label key={optionIndex}>Option {String.fromCharCode(65+optionIndex)}<input value={option} onChange={event=>updateOption(index,optionIndex,event.target.value)} required/></label>)}<label>Correct answer<select value={mcq.correctOption} onChange={event=>updateMcq(index,"correctOption",Number(event.target.value))}>{mcq.options.map((_,optionIndex)=><option value={optionIndex} key={optionIndex}>Option {String.fromCharCode(65+optionIndex)}</option>)}</select></label><label>Points<input type="number" min="1" value={mcq.points} onChange={event=>updateMcq(index,"points",Number(event.target.value))} required/></label></div></article>)}</section>
        <section className="vx-builder-review"><header><span>06 / Review</span><h2>Ready for a deliberate assessment?</h2><p>Review the operational details before saving this {id ? "assessment update" : "draft"}.</p></header><dl><div><dt>Title</dt><dd>{form.title || "Untitled assessment"}</dd></div><div><dt>Visibility</dt><dd>{form.visibility === "PRIVATE" ? `${invitees.length} invited participant${invitees.length === 1 ? "" : "s"}` : "Discoverable by eligible users"}</dd></div><div><dt>Schedule</dt><dd>{form.startAt ? new Date(form.startAt).toLocaleString() : "Start time not set"}</dd></div><div><dt>Duration</dt><dd>{form.startAt && form.endAt ? `${Math.max(0, Math.round((new Date(form.endAt) - new Date(form.startAt)) / 60000))} minutes` : "Not set"}</dd></div><div><dt>Problem set</dt><dd>{form.problems.length} coding · {form.mcqs?.length||0} MCQ</dd></div><div><dt>Safeguards</dt><dd>{[form.fullscreenRequired && "Fullscreen", form.microphoneRequired && "Microphone", form.strictProctoring && "Strict proctoring"].filter(Boolean).join(" · ") || "None"}</dd></div></dl></section>
        <footer><p><ShieldCheck size={15}/> Your assessment remains a draft until you publish it from the studio.</p><button className="vx-primary-action" disabled={busy || !form.problems.length}>{busy ? "Saving…" : id ? "Save changes" : "Save draft"}</button></footer>
      </div>
    </form>
    {catalogOpen && <div className="vx-problem-catalog-layer" role="presentation"><section className="vx-problem-catalog" role="dialog" aria-modal="true" aria-labelledby="problem-catalog-title"><header><div><span>Visible problem library</span><h2 id="problem-catalog-title">Choose assessment problems</h2><p>Only active problems available to your users appear here. Your selections stay in the draft when you return.</p></div><button type="button" className="vx-catalog-close" onClick={() => setCatalogOpen(false)} aria-label="Close problem library"><X size={18}/></button></header><div className="vx-catalog-tools"><label className="vx-search"><Search size={16}/><input autoFocus value={query} onChange={event => setQuery(event.target.value)} placeholder="Search titles or tags"/></label><div className="vx-catalog-filters" aria-label="Filter problems by difficulty">{difficulties.map(value => <button type="button" className={difficulty === value ? "active" : ""} onClick={() => setDifficulty(value)} key={value}>{value === "ALL" ? "All" : `${value[0]}${value.slice(1).toLowerCase()}`}</button>)}</div></div><div className="vx-catalog-count" aria-live="polite">{catalogLoading ? "Loading visible problems…" : `${visible.length} visible problem${visible.length === 1 ? "" : "s"}`}</div><div className="vx-catalog-list">{catalogLoading ? <div className="vx-catalog-message">Loading the problem library…</div> : catalogError ? <div className="vx-catalog-message error">{catalogError}</div> : visible.length ? visible.map(problem => <button type="button" className={selected(problem.id) ? "selected" : ""} onClick={() => toggle(problem)} key={problem.id}><span className="vx-catalog-check">{selected(problem.id) && <Check size={15}/>}</span><span className="vx-catalog-problem-copy"><strong>{problem.title}</strong><small>{problem.difficulty}{problem.tags ? ` · ${problem.tags}` : ""}</small></span><span className={`vx-difficulty-badge ${String(problem.difficulty || "").toLowerCase()}`}>{problem.difficulty || "Unrated"}</span></button>) : <div className="vx-catalog-message">No active problems match this search or filter.</div>}</div><footer><p><strong>{form.problems.length}</strong> problem{form.problems.length === 1 ? "" : "s"} ready for this assessment</p><button type="button" className="vx-primary-action" onClick={() => setCatalogOpen(false)}>Done selecting <Check size={16}/></button></footer></section></div>}
  </UserShell>;
}
