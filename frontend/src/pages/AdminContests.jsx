import { useEffect, useState } from "react";
import api from "../services/api";
import AdminShell from "../components/AdminShell";
import { EmptyState, LoadingState } from "../components/PageState";
import StatusPill from "../components/StatusPill";

const empty = { title:"", slug:"", description:"", rulesText:"", startAt:"", endAt:"", visibility:"PUBLIC", accessCode:"", status:"UPCOMING", accent:"#a92d43" };

export default function AdminContests() {
  const [items, setItems] = useState([]);
  const [problems, setProblems] = useState([]);
  const [selection, setSelection] = useState({});
  const [form, setForm] = useState(empty);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setError("");
    const [contestResult, problemResult] = await Promise.allSettled([
      api.get("/contests"),
      api.get("/problems/admin/page", { params:{ page:0, size:100 } }),
    ]);
    if (contestResult.status === "fulfilled") setItems(contestResult.value.data || []);
    else setError("Unable to load the contest registry.");
    if (problemResult.status === "fulfilled") setProblems(problemResult.value.data?.content || []);
    else setError((current) => current || "Unable to load problems for contest selection.");
    setLoading(false);
  };

  useEffect(() => { load(); }, []);

  const toggleProblem = (problemId) => setSelection((current) => {
    const next = {...current};
    if (next[problemId]) delete next[problemId];
    else next[problemId] = 100;
    return next;
  });

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const created = await api.post("/contests", form);
      const contestId = created.data.id;
      // Preserve the selected order. The API assigns the next display order when
      // each problem is added, so concurrent requests can otherwise collide.
      for (const [problemId, points] of Object.entries(selection)) {
        await api.post(`/contests/${contestId}/problems`, { problemId:Number(problemId), points:Number(points) || 100 });
      }
      setForm(empty);
      setSelection({});
      await load();
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Unable to create the contest and attach its selected problems.");
    } finally {
      setSaving(false);
    }
  };

  const selectedCount = Object.keys(selection).length;

  return <AdminShell title="Contests" description="Build timed competitions, select their problem set, and control access from one workspace.">
    {error && <div className="dashboard-error" role="alert">{error}</div>}
    <form className="vx-admin-contest-form" onSubmit={submit}>
      <div className="vx-admin-form-heading"><span>New contest</span><small>Timing and access are enforced by the server</small></div>
      {[["title","Title"],["slug","URL slug"],["startAt","Start time"],["endAt","End time"]].map(([name,label]) => <label key={name}>{label}<input required type={name.endsWith("At") ? "datetime-local" : "text"} value={form[name]} onChange={(event) => setForm({...form,[name]:event.target.value})}/></label>)}
      <label className="vx-form-span">Description<textarea value={form.description} onChange={(event) => setForm({...form,description:event.target.value})}/></label>
      <label>Visibility<select value={form.visibility} onChange={(event) => setForm({...form,visibility:event.target.value})}><option>PUBLIC</option><option>PRIVATE</option></select></label>
      {form.visibility === "PRIVATE" && <label>Access code<input required value={form.accessCode} onChange={(event) => setForm({...form,accessCode:event.target.value})}/></label>}
      <fieldset className="vx-problem-picker">
        <legend>Contest problem set <span>{selectedCount} selected</span></legend>
        <p>Select published problems and assign the points available for each one.</p>
        {problems.length === 0 ? <div className="vx-guidance-empty">No problems are available to add.</div> : <div className="vx-problem-picker-list">
          {problems.map((problem, index) => {
            const selected = Object.prototype.hasOwnProperty.call(selection, problem.id);
            return <div className={selected ? "selected" : ""} key={problem.id}>
              <label><input type="checkbox" checked={selected} onChange={() => toggleProblem(problem.id)}/><span className="vx-problem-letter">{String.fromCharCode(65 + index)}</span><span><strong>{problem.title}</strong><small>{problem.difficulty}</small></span></label>
              {selected && <label className="vx-points-input"><span>Points</span><input type="number" min="1" value={selection[problem.id]} onChange={(event) => setSelection((current) => ({...current,[problem.id]:event.target.value}))}/></label>}
            </div>;
          })}
        </div>}
      </fieldset>
      <button className="primary-button" disabled={saving}>{saving ? "Creating contest…" : `Create contest${selectedCount ? ` with ${selectedCount} problem${selectedCount === 1 ? "" : "s"}` : ""}`}</button>
    </form>
    <section className="vx-admin-section">
      <div className="vx-section-heading"><div><p className="vx-eyebrow">Competition registry</p><h2>Scheduled contests</h2></div></div>
      {loading ? <LoadingState label="Loading contests"/> : items.length === 0 ? <EmptyState title="No contests">Create the first competition using the control panel above.</EmptyState> : <div className="vx-contest-registry">{items.map((contest) => <div key={contest.id}><strong>{contest.title}<small>{contest.visibility}</small></strong><StatusPill value={contest.status}/><time>{new Date(contest.startAt).toLocaleString()}</time><span className="vx-section-meta">{contest.registrationCount} registered</span></div>)}</div>}
    </section>
  </AdminShell>;
}
