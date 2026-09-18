import { useRef, useState, useEffect } from "react";
import { Check, Copy, LockKeyhole, PanelLeftClose } from "lucide-react";
import EditorialPanel from "../EditorialPanel";
import { WorkspaceTabs } from "./WorkspaceControls";

function CopyExample({ text }) {
  const [message, setMessage] = useState("");
  return <button className="sw-copy" title="Copy example" aria-label="Copy example" onClick={async () => {
    try { await navigator.clipboard.writeText(text); setMessage("Copied"); } catch { setMessage("Copy unavailable"); }
  }}>{message === "Copied" ? <Check size={14}/> : <Copy size={14}/>}<span role="status">{message}</span></button>;
}

function HintConfirmation({ hint, onCancel, onReveal }) {
  const dialog = useRef(null);
  useEffect(() => { dialog.current?.showModal?.(); }, []);
  return <dialog ref={dialog} open={typeof HTMLDialogElement.prototype.showModal !== "function" || undefined} className="sw-confirm" aria-label="Confirm hint reveal" onCancel={(event) => { event.preventDefault(); onCancel(); }}>
    <LockKeyhole size={23}/><h2>Reveal this hint?</h2><p>Revealing this hint applies a {hint.penaltyPoints}-point penalty. Continue?</p>
    <div className="sw-actions"><button autoFocus onClick={onCancel}>Cancel</button><button className="sw-primary" onClick={() => onReveal(hint)}>Reveal hint</button></div>
  </dialog>;
}

export default function ProblemReadingPane({ problem, activePanel, setActivePanel, hintData, pendingHint, setPendingHint, revealHint, editorial, setEditorial, note, setNote, noteState, setNoteState, saveNote, deleteNote, onCollapse }) {
  const tags = Array.isArray(problem.tags) ? problem.tags : typeof problem.tags === "string" ? problem.tags.split(",") : [];
  const tabs = [{ value: "description", label: "Description" }, { value: "hints", label: `Hints${hintData.total ? ` (${hintData.unlocked}/${hintData.total})` : ""}` }, { value: "editorial", label: "Editorial" }, { value: "notes", label: "My Notes" }];
  return <section className="sw-reading" id="sw-problem" aria-label="Problem reading pane">
    <div className="sw-pane-bar"><WorkspaceTabs label="Problem information" items={tabs} value={activePanel} onChange={setActivePanel} idPrefix="reading"/><button className="sw-icon sw-collapse-problem" onClick={onCollapse} title="Collapse problem pane" aria-label="Collapse problem pane"><PanelLeftClose size={16}/></button></div>
    <div className="sw-reading-scroll" role="tabpanel" id={`reading-panel-${activePanel}`} aria-labelledby={`reading-${activePanel}`} tabIndex={0}>
      {activePanel === "description" && <article className="sw-article">
        <span className="sw-eyebrow">The challenge</span>
        {problem.description && <p className="sw-introduction">{problem.description}</p>}
        {[["constraints", "Constraints"], ["inputFormat", "Input Format"], ["outputFormat", "Output Format"]].map(([key, title]) => problem[key] && <section key={key}><h2>{title}</h2><p>{problem[key]}</p></section>)}
        {problem.examples && <section className="sw-examples"><div className="sw-section-heading"><h2>Examples</h2><CopyExample text={problem.examples}/></div><pre data-i18n-ignore translate="no">{problem.examples}</pre></section>}
        {tags.length > 0 && <section><h2>Topics</h2><div className="sw-tags">{tags.filter((tag) => tag.trim()).map((tag) => <span key={tag}>{tag.trim()}</span>)}</div></section>}
      </article>}
      {activePanel === "hints" && <div className="sw-hints"><span className="sw-eyebrow">A nudge in the right direction</span><p className="sw-muted">{hintData.total ? `${hintData.unlocked} of ${hintData.total} hints revealed · ${hintData.attempts} submission attempt${hintData.attempts === 1 ? "" : "s"} on this problem.` : "No hints are configured for this problem."}</p>
        {(hintData.hints || []).map((hint, index) => <article className="sw-hint" key={hint.id}><span className="sw-hint-number">{String(index + 1).padStart(2, "0")}</span><div><div className="sw-section-heading"><h2>{hint.revealed ? hint.title : `Hint ${index + 1}`}</h2>{hint.revealed ? <span className="sw-muted">Revealed</span> : <LockKeyhole size={14}/>}</div>
          {hint.revealed ? <><p>{hint.content}</p>{hint.penaltyPoints > 0 && <small>Penalty applied: {hint.penaltyPoints} points</small>}</> : hint.available ? <><p>Available to reveal{hint.penaltyPoints > 0 ? ` · ${hint.penaltyPoints}-point penalty` : ""}.</p><button onClick={() => hint.penaltyPoints > 0 ? setPendingHint(hint) : revealHint(hint)}>Reveal hint</button></> : <><p>{Math.min(hintData.attempts, hint.attemptsRequired)} of {hint.attemptsRequired} attempts completed</p><small>Available after {hint.attemptsRemaining} more attempt{hint.attemptsRemaining === 1 ? "" : "s"}.</small></>}
        </div></article>)}
        {pendingHint && <HintConfirmation hint={pendingHint} onCancel={() => setPendingHint(null)} onReveal={revealHint}/>}
      </div>}
      {activePanel === "editorial" && <article className="sw-article sw-editorial"><EditorialPanel key={problem.id} problemId={problem.id} access={editorial} onChange={setEditorial}/></article>}
      {activePanel === "notes" && <div className="sw-notes"><div className="sw-section-heading"><label htmlFor="private-note">Private note</label><span className="sw-muted">{note.length} characters</span></div><p className="sw-muted">Your approach, edge cases, and lessons learned.</p><textarea id="private-note" value={note} onChange={(event) => { setNote(event.target.value); setNoteState("idle"); }} placeholder="Start with an observation…"/><div className="sw-actions"><button className="sw-primary" onClick={saveNote} disabled={noteState === "saving"}>{noteState === "saving" ? "Saving…" : "Save Note"}</button>{note && <button onClick={deleteNote}>Delete Note</button>}</div><p role="status" className="sw-muted">{noteState === "saved" ? "Saved privately to your account." : noteState === "error" ? "Unable to save your note." : noteState === "deleted" ? "Note deleted." : "Changes are saved when you select Save Note."}</p></div>}
    </div>
  </section>;
}
