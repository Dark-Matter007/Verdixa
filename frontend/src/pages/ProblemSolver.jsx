import EditorialPanel from "../components/EditorialPanel";
import SubmissionMilestone from "../components/SubmissionMilestone";
import { useCallback, useContext, useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  Play,
  Send,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Clock3,
  Maximize2,
  Minimize2,
  RotateCcw,
  Bookmark,
  ShieldCheck,
  TriangleAlert,
} from "lucide-react";
import Editor from "@monaco-editor/react";
import api from "../services/api";
import BrandLogo from "../components/BrandLogo";
import UserAccountMenu from "../components/UserAccountMenu";
import { ThemeContext } from "../components/ThemeProvider";

const EMPTY_HINTS = { total: 0, unlocked: 0, attempts: 0, unlockAt: 3, available: false, hints: [] };

function ProblemSolver() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const contestId = new URLSearchParams(location.search).get("contest");
  const contestWorkspace = new URLSearchParams(location.search).get("workspace") === "1";
  const { theme } = useContext(ThemeContext);

  const [problem, setProblem] = useState(null);
  const [sourceCode, setSourceCode] = useState("");
  const [language, setLanguage] = useState("java");
  const [customInput, setCustomInput] = useState("");
  const [customCases, setCustomCases] = useState([{ arguments: [], expected: "" }]);
  const [languageSources, setLanguageSources] = useState({});
  const [starterTemplates, setStarterTemplates] = useState({});
  const [editorExpanded, setEditorExpanded] = useState(false);
  const [bookmarked, setBookmarked] = useState(false);
  const [editorial, setEditorial] = useState(null);
  const [hintData, setHintData] = useState(EMPTY_HINTS);
  const [pendingHint, setPendingHint] = useState(null);
  const [note, setNote] = useState("");
  const [noteState, setNoteState] = useState("idle");
  const [activePanel, setActivePanel] = useState("description");
  const [personalLists, setPersonalLists] = useState([]);
  const [navigation, setNavigation] = useState({});
  const [contest, setContest] = useState(null);
  const [contestProblems, setContestProblems] = useState([]);
  const [focusWarning, setFocusWarning] = useState("");
  const [remainingSeconds, setRemainingSeconds] = useState(null);

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const [submissions, setSubmissions] = useState([]);
  const [error, setError] = useState("");

  const loadContestWorkspace = useCallback(async () => {
    if (!contestId) return;
    try {
      const [contestResponse, problemsResponse] = await Promise.all([api.get(`/contests/${contestId}`), api.get(`/contests/${contestId}/problems`)]);
      setContest(contestResponse.data); setContestProblems(problemsResponse.data || []);
    } catch (reason) { setFocusWarning(reason.response?.data?.message || "Contest status could not be refreshed."); }
  }, [contestId]);

  const fetchSubmissionHistory = useCallback(async () => {
    try {
      const userResponse = await api.get("/users/me");
      const response = await api.get(
        `/submissions/user/${userResponse.data.id}/problem/${id}`
      );
      setSubmissions(response.data || []);
    } catch (err) {
      console.error("Failed to load problem submissions:", err);
    }
  }, [id]);

  const fetchProblem = useCallback(async () => {
    try {
      setLoading(true);
      setError("");

      const response = await api.get(contestId ? `/contests/${contestId}/problems/${id}` : `/problems/${id}`);

      const data = response.data;

      setProblem(data);
      if (!contestWorkspace) {
        api.get(`/problems/${id}/navigation`).then((value) => setNavigation(value.data || {})).catch(() => setNavigation({}));
        api.get("/lists").then((value) => setPersonalLists(value.data || [])).catch(() => setPersonalLists([]));
        api.get(`/problems/${id}/editorial`).then((value) => setEditorial(value.data)).catch(() => setEditorial(null));
        api.get(`/problems/${id}/hints`).then((value) => setHintData(Array.isArray(value.data) ? EMPTY_HINTS : value.data || EMPTY_HINTS)).catch(() => setHintData(EMPTY_HINTS));
        api.get(`/problems/${id}/note`).then((value) => setNote(value.data.content || "")).catch(() => setNote(""));
        const bookmarkResponse = await api.get(`/bookmarks/${id}`);
        setBookmarked(Boolean(bookmarkResponse.data));
      }

      const defaults = {
        java: `public class Solution {
    public static void main(String[] args) {

    }
}`,
        cpp: `#include <iostream>\nusing namespace std;\n\nint main() {\n    // Write your solution here\n    return 0;\n}`,
        python: `# Write your solution here\ndef main():\n    pass\n\nif __name__ == "__main__":\n    main()`
      };
      if (data.executionMode === "FUNCTION" && data.functionSignature) {
        const signature=data.functionSignature, params=signature.parameters||[];
        const cppType=(type)=>({Integer:"int",String:"string","int[]":"vector<int>","long[]":"vector<long long>","double[]":"vector<double>","String[]":"vector<string>",long:"long long",boolean:"bool"}[type]||type);
        const pythonName=signature.functionName.replace(/([a-z0-9])([A-Z])/g,"$1_$2").toLowerCase();
        defaults.java=`public ${signature.returnType} ${signature.functionName}(${params.map((p)=>`${p.type} ${p.name}`).join(", ")}) {\n    // Write your solution\n}`;
        defaults.cpp=`${cppType(signature.returnType)} ${signature.functionName}(${params.map((p)=>`${cppType(p.type)} ${p.name}`).join(", ")}) {\n    // Write your solution\n}`;
        defaults.python=`def ${pythonName}(${params.map((p)=>p.name).join(", ")}):\n    # Write your solution\n    pass`;
        setCustomCases([{arguments:params.map(()=>""),expected:""}]);
      }
      let starterByLanguage = {};
      if (data.starterCode) {
        try {
          const parsed = JSON.parse(data.starterCode);
          if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) starterByLanguage = parsed;
        } catch {
          starterByLanguage = { java: data.starterCode };
        }
      }
      const initialSources = {
        java: starterByLanguage.java || defaults.java,
        cpp: starterByLanguage.cpp || defaults.cpp,
        python: starterByLanguage.python || defaults.python,
      };
      setLanguageSources(initialSources);
      setStarterTemplates(initialSources);
      setSourceCode(initialSources.java);
      if (location.state?.replay) {
        const replay=location.state.replay, replayLanguage=replay.language==="c++"?"cpp":replay.language;
        setLanguage(replayLanguage); setSourceCode(replay.sourceCode); setLanguageSources({...initialSources,[replayLanguage]:replay.sourceCode});
        window.history.replaceState({},document.title);
      }
    } catch (err) {
      console.error("Failed to load problem:", err);

      if (err.response?.status === 401) {
        localStorage.clear();
        navigate("/login");
        return;
      }
      if (err.response?.status === 403) setError("You do not have access to this problem.");
      else if (err.response?.status === 404) setError("Problem not found or no longer published.");
      else setError("Unable to load this problem. Please retry.");
    } finally {
      setLoading(false);
    }
  }, [id, navigate, location.state, contestId, contestWorkspace]);

  const runCode = async (caseIndex = null) => {
    if (!sourceCode.trim()) {
      setError("Please enter some code.");
      return;
    }

    try {
      setSubmitting(true);
      setError("");
      setResult(null);

      const request = {
        problemId: Number(id),
        language,
        sourceCode,
        input: customInput,
      };
      const casesToRun = caseIndex === null ? customCases : [customCases[caseIndex]];
      if (problem.executionMode === "FUNCTION") request.testCases = casesToRun.map((item) => ({
        arguments: item.arguments.map((value) => JSON.parse(value)),
        ...(item.expected.trim() ? { expected: JSON.parse(item.expected) } : {})
      }));
      const response = await api.post("/execution-test/run", request);

      setResult(response.data);
    } catch (err) {
      console.error("Submission failed:", err);

      if (err instanceof SyntaxError) {
        setError("Check that each custom testcase value is valid JSON.");
      } else if (err.response?.data) {
        setResult(err.response.data);
      } else {
        setError("Unable to run code. Please retry.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const changeLanguage = (nextLanguage) => {
    setLanguageSources((previous) => ({ ...previous, [language]: sourceCode }));
    setLanguage(nextLanguage);
    if (languageSources[nextLanguage] !== undefined) setSourceCode(languageSources[nextLanguage]);
  };

  const resetCurrentLanguage = () => {
    const template = starterTemplates[language] ?? "";
    if (sourceCode !== template && !window.confirm(`Reset your ${language === "cpp" ? "C++17" : language} solution to the starter code?`)) return;
    setSourceCode(template);
    setLanguageSources((previous) => ({ ...previous, [language]: template }));
    setResult(null);
    setError("");
  };

  const toggleBookmark = async () => {
    try {
      await api({ method: bookmarked ? "delete" : "post", url: `/bookmarks/${id}` });
      setBookmarked((value) => !value);
    } catch { setError("Unable to update this bookmark."); }
  };

  const saveNote = async () => {
    setNoteState("saving");
    try { await api.put(`/problems/${id}/note`, { content: note }); setNoteState("saved"); }
    catch { setNoteState("error"); }
  };
  const deleteNote = async () => {
    try { await api.delete(`/problems/${id}/note`); setNote(""); setNoteState("deleted"); }
    catch { setNoteState("error"); }
  };
  const addToList = async (listId) => { if (!listId) return; try { await api.post(`/lists/${listId}/problems/${id}`); setError(""); } catch (err) { setError(err.response?.data?.message || "Unable to add this problem to the list."); } };
  const revealHint = async (hint) => {
    try {
      await api.post(`/problems/${id}/hints/${hint.id}/reveal`);
      const response = await api.get(`/problems/${id}/hints`);
      setHintData(response.data || EMPTY_HINTS);
      setPendingHint(null);
    } catch (err) { setError(err.response?.data?.message || "Unable to reveal this hint."); }
  };

  useEffect(() => {
    if (!localStorage.getItem("algosphere_token")) {
      navigate("/login");
      return;
    }

    fetchProblem();
    fetchSubmissionHistory();
  }, [fetchProblem, fetchSubmissionHistory, navigate]);

  useEffect(() => { loadContestWorkspace(); }, [loadContestWorkspace]);
  useEffect(() => {
    if (!contestWorkspace || !contest?.endAt) return;
    const refresh = () => setRemainingSeconds(Math.max(0, Math.ceil((new Date(contest.endAt).getTime() - Date.now()) / 1000)));
    refresh(); const timer = window.setInterval(refresh, 1000);
    const onFullscreenChange = () => { if (!document.fullscreenElement) setFocusWarning("Focus mode was exited. You can continue, but re-enter fullscreen to reduce distractions."); };
    const onVisibility = () => { if (document.hidden) setFocusWarning("Focus mode detected that this tab lost visibility. Contest timing continues on the server."); };
    document.addEventListener("fullscreenchange", onFullscreenChange); document.addEventListener("visibilitychange", onVisibility);
    return () => { window.clearInterval(timer); document.removeEventListener("fullscreenchange", onFullscreenChange); document.removeEventListener("visibilitychange", onVisibility); };
  }, [contestWorkspace, contest]);

  const submitCode = async () => {
    if (!sourceCode.trim()) {
      setError("Please enter some code.");
      return;
    }

    try {
      setSubmitting(true);
      setError("");
      setResult(null);

      const response = await api.post(
        `/submissions?problemId=${id}&language=${language}${contestId ? `&contestId=${contestId}` : ""}`,
        sourceCode,
        { headers: { "Content-Type": "text/plain" } }
      );

      setResult(response.data);
      if (contestId) loadContestWorkspace();
      if (!contestWorkspace) api.get(`/problems/${id}/editorial`).then(value=>setEditorial(value.data)).catch(()=>setEditorial(null));
      fetchSubmissionHistory();
      if (!contestWorkspace) api.get(`/problems/${id}/hints`).then((value) => { if (!Array.isArray(value.data)) setHintData(value.data); }).catch(() => {});
    } catch (err) {
      console.error("Submission failed:", err);
      if (err.response?.status === 401) setError("Your session expired. Please sign in again.");
      else if (err.response?.status === 403) setError("You do not have permission to submit this problem.");
      else setError(err.response?.data?.message || err.response?.data?.errorMessage || "Unable to submit. Please retry.");
    } finally {
      setSubmitting(false);
    }
  };

  const getStatusIcon = () => {
    if (!result) return null;

    if (result.status === "ACCEPTED") {
      return <CheckCircle2 size={22} />;
    }

    if (result.status === "WRONG_ANSWER") {
      return <XCircle size={22} />;
    }

    if (
      result.status === "COMPILATION_ERROR" ||
      result.status === "RUNTIME_ERROR"
    ) {
      return <AlertTriangle size={22} />;
    }

    return <Clock3 size={22} />;
  };

  if (loading) {
    return (
      <div className="solver-page">
        <div className="solver-loading">
          Loading problem...
        </div>
      </div>
    );
  }

  if (error && !problem) {
    return (
      <div className="solver-page">
        <div className="solver-error solver-unavailable">
          <h1>Problem unavailable</h1>
          <p>{error}</p>
          <div className="form-actions"><button onClick={() => navigate("/dashboard")}>Back to Problems</button><button onClick={fetchProblem}>Retry</button></div>
        </div>
      </div>
    );
  }

  if (!problem) {
    return <div className="solver-page"><div className="solver-error solver-unavailable"><h1>Problem unavailable</h1><button onClick={() => navigate("/dashboard")}>Back to Problems</button></div></div>;
  }

  const isFunctionProblem = problem.executionMode === "FUNCTION";
  const functionSignature = problem.functionSignature || { functionName: "", returnType: "", parameters: [] };
  const functionParameters = Array.isArray(functionSignature.parameters) ? functionSignature.parameters : [];
  const functionConfigured = Boolean(functionSignature.functionName && functionSignature.returnType);
  const displayedFunctionName = language === "python" ? functionSignature.functionName.replace(/([a-z0-9])([A-Z])/g,"$1_$2").toLowerCase() : functionSignature.functionName;
  const tags = Array.isArray(problem.tags) ? problem.tags : typeof problem.tags === "string" ? problem.tags.split(",") : [];
  const placeholderFor = (type) => ({String:'"hello"',int:"42",Integer:"42",long:"42",double:"3.14",boolean:"true","int[]":"[1,2,3]","long[]":"[1,2,3]","double[]":"[1.5,2.5]","String[]":'["a","b"]'}[type] || "JSON value");
  const formatRemaining = (seconds) => { if (seconds === null) return "--:--"; const hours = Math.floor(seconds / 3600); return `${hours ? `${String(hours).padStart(2, "0")}:` : ""}${String(Math.floor((seconds % 3600) / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`; };
  const leaveContest = () => navigate(`/contests/${contestId}`);

  return (
    <div className={`solver-page ${editorExpanded ? "editor-expanded" : ""} ${contestWorkspace ? "contest-solver" : ""}`}>

      <header className="solver-navbar">

        <button
          className="solver-back"
          onClick={() => contestId ? leaveContest() : navigate("/dashboard")}
        >
          <ArrowLeft size={19} />
          {contestId ? "Contest" : "Problems"}
        </button>

        <div className="solver-brand"><BrandLogo compact /><strong>{contestWorkspace ? contest?.title || "Contest focus" : "Verdixa"}</strong></div>

        {contestWorkspace && <div className="contest-workspace-status"><ShieldCheck size={14}/><span>Focus mode</span><strong>{formatRemaining(remainingSeconds)}</strong></div>}

        <div className="solver-language">
          <select
            value={language}
            onChange={(e) => changeLanguage(e.target.value)}
          >
            <option value="java">Java</option>
            <option value="cpp">C++</option>
            <option value="python">Python</option>
          </select>
        </div>
        <UserAccountMenu />

      </header>

      {contestWorkspace && <section className="contest-workspace-tabs" aria-label="Contest problems"><div className="contest-workspace-tabs-label"><ShieldCheck size={14}/> Server-timed workspace</div>{contestProblems.map((contestProblem, index) => <button key={contestProblem.id} className={`${Number(id) === contestProblem.problemId ? "active" : ""} ${contestProblem.status === "COMPLETED" ? "completed" : ""}`} onClick={() => navigate(`/problems/${contestProblem.problemId}?contest=${contestId}&workspace=1`)} aria-current={Number(id) === contestProblem.problemId ? "page" : undefined}><span>{String.fromCharCode(65 + (contestProblem.displayOrder || index + 1) - 1)}</span>{contestProblem.status === "COMPLETED" && <CheckCircle2 size={14}/>}</button>)}<button className="contest-workspace-results" onClick={leaveContest}>Results</button></section>}
      {contestWorkspace && focusWarning && <div className="contest-focus-warning" role="status"><TriangleAlert size={16}/>{focusWarning}<button onClick={() => setFocusWarning("")}>Dismiss</button></div>}

      <main className="solver-container">

        <section className="problem-panel">

          <div className="problem-panel-header">

            <div>
              <span className="solver-label">
                Problem
              </span>

              <h1>{problem.title}</h1>
            </div>

            <span
              className={`difficulty-badge ${problem.difficulty?.toLowerCase()}`}
            >
              {problem.difficulty}
            </span>

            {!contestWorkspace && <><button className={`problem-bookmark ${bookmarked ? "active" : ""}`} onClick={toggleBookmark} aria-label={bookmarked ? "Remove bookmark" : "Bookmark problem"}>
              <Bookmark size={18} fill={bookmarked ? "currentColor" : "none"} />
            </button>
            {personalLists.length > 0 && <select aria-label="Add problem to list" defaultValue="" onChange={(event) => addToList(event.target.value)}><option value="" disabled>Add to list…</option>{personalLists.map((list) => <option key={list.id} value={list.id}>{list.name}</option>)}</select>}
            <div className="problem-navigation"><button disabled={!navigation.previous} onClick={() => navigation.previous && navigate(`/problems/${navigation.previous.id}`)}>Previous</button><button disabled={!navigation.next} onClick={() => navigation.next && navigate(`/problems/${navigation.next.id}`)}>Next</button></div></>}

          </div>

          <div className="problem-description">

            <div className="problem-tabs" role="tablist"><button role="tab" aria-selected={activePanel === "description"} className={activePanel === "description" ? "active" : ""} onClick={() => setActivePanel("description")}>Description</button>{!contestWorkspace && <><button role="tab" aria-selected={activePanel === "hints"} className={activePanel === "hints" ? "active" : ""} onClick={() => setActivePanel("hints")}>Hints {hintData.total ? `(${hintData.unlocked}/${hintData.total})` : ""}</button><button role="tab" aria-selected={activePanel === "editorial"} className={activePanel === "editorial" ? "active" : ""} onClick={() => setActivePanel("editorial")}>Editorial</button><button role="tab" aria-selected={activePanel === "notes"} className={activePanel === "notes" ? "active" : ""} onClick={() => setActivePanel("notes")}>My Notes</button></>}</div>

            {activePanel === "hints" && <div className="editorial-panel hints-panel"><p className="hint-summary">{hintData.total ? `${hintData.unlocked} of ${hintData.total} hints revealed · ${hintData.attempts} submission attempt${hintData.attempts === 1 ? "" : "s"} on this problem.` : "No hints are configured for this problem."}</p>{hintData.hints.map((hint, index) => <article className={`hint-card ${hint.revealed ? "revealed" : hint.available ? "available" : "locked"}`} key={hint.id}><h3>{hint.revealed ? hint.title : `Hint ${index + 1}`}</h3>{hint.revealed ? <><p>{hint.content}</p>{hint.penaltyPoints > 0 && <small>Penalty applied: {hint.penaltyPoints} points</small>}</> : hint.available ? <><p>Available to reveal{hint.penaltyPoints > 0 ? ` · ${hint.penaltyPoints}-point penalty` : ""}.</p><button className="secondary-button" onClick={() => hint.penaltyPoints > 0 ? setPendingHint(hint) : revealHint(hint)}>Reveal hint</button></> : <><p>{Math.min(hintData.attempts, hint.attemptsRequired)} of {hint.attemptsRequired} attempts completed</p><small className="hint-unlock-status">Available after {hint.attemptsRemaining} more attempt{hint.attemptsRemaining === 1 ? "" : "s"}.</small></>}</article>)}{pendingHint && <div className="hint-confirm" role="dialog" aria-modal="true" aria-label="Confirm hint reveal"><p>Revealing this hint applies a {pendingHint.penaltyPoints}-point penalty. Continue?</p><button className="secondary-button" onClick={() => setPendingHint(null)}>Cancel</button><button className="submit-button" onClick={() => revealHint(pendingHint)}>Reveal hint</button></div>}</div>}
            {activePanel === "editorial" && <div className="editorial-panel"><EditorialPanel key={id} problemId={id} access={editorial} onChange={setEditorial}/></div>}
            {activePanel === "notes" && <div className="notes-panel"><label htmlFor="private-note">Private note</label><textarea id="private-note" value={note} onChange={(event) => { setNote(event.target.value); setNoteState("idle"); }} placeholder="Capture your approach, edge cases, and lessons learned." rows="10"/><div className="form-actions"><button onClick={saveNote} disabled={noteState === "saving"}>{noteState === "saving" ? "Saving…" : "Save Note"}</button>{note && <button onClick={deleteNote}>Delete Note</button>}</div>{noteState === "saved" && <small>Saved privately to your account.</small>}{noteState === "error" && <small>Unable to save your note.</small>}</div>}

            {activePanel === "description" && <>

            <p>
              {problem.description}
            </p>

            {problem.constraints && (
              <div className="problem-detail">
                <h3>Constraints</h3>
                <p>{problem.constraints}</p>
              </div>
            )}

            {problem.inputFormat && (
              <div className="problem-detail">
                <h3>Input Format</h3>
                <p>{problem.inputFormat}</p>
              </div>
            )}

            {problem.outputFormat && (
              <div className="problem-detail">
                <h3>Output Format</h3>
                <p>{problem.outputFormat}</p>
              </div>
            )}

            {problem.examples && (
              <div className="problem-detail">
                <h3>Examples</h3>
                <pre>{problem.examples}</pre>
              </div>
            )}

            {tags.length > 0 && (
              <div className="solver-tags">
                {tags.map((tag) => (
                  <span key={tag}>
                    {tag.trim()}
                  </span>
                ))}
              </div>
            )}
            </>}

          </div>

        </section>

        <section className="editor-panel">

          <div className="editor-header">

            <div>
              <span className="solver-label">
                Code Editor
              </span>

              <span className="editor-language">
                · {language === "cpp" ? "C++17" : language[0].toUpperCase() + language.slice(1)}
              </span>
            </div>

            <div className="editor-actions">

              <button className="editor-tool-button" onClick={resetCurrentLanguage} disabled={submitting} title="Reset current language code" aria-label="Reset code">
                <RotateCcw size={17} />
                <span>Reset</span>
              </button>

              <button className="editor-tool-button" onClick={() => setEditorExpanded((value) => !value)} title={editorExpanded ? "Exit expanded editor" : "Expand editor"} aria-label={editorExpanded ? "Exit expanded editor" : "Expand editor"}>
                {editorExpanded ? <Minimize2 size={17} /> : <Maximize2 size={17} />}
              </button>

              <button
                className="run-button"
                onClick={() => runCode()}
                disabled={submitting}
              >
                <Play size={17} />

                {submitting ? "Running..." : "Run Code"}
              </button>

              <button
                className="submit-button"
                onClick={submitCode}
                disabled={submitting}
              >
                <Send size={17} />

                {submitting ? "Submitting..." : "Submit"}
              </button>

            </div>

          </div>

          <div className="monaco-editor-wrap">
            <Editor
              height={editorExpanded ? "calc(100vh - 145px)" : "430px"}
              language={language === "cpp" ? "cpp" : language}
              value={sourceCode}
              onChange={(value) => {
                const nextValue = value ?? "";
                setSourceCode(nextValue);
                setLanguageSources((previous) => ({ ...previous, [language]: nextValue }));
              }}
              theme={theme === "dark" ? "vs-dark" : "vs"}
              options={{ minimap: { enabled: false }, fontSize: 14, tabSize: 2, automaticLayout: true, scrollBeyondLastLine: false, padding: { top: 16, bottom: 16 } }}
            />
          </div>

          <div className="custom-input-panel">
            <div className="custom-input-header">
              <div>
                <span className="solver-label">Custom Input</span>
                <p>{isFunctionProblem && functionConfigured ? `${displayedFunctionName}(${functionParameters.map((p)=>`${p.name}: ${p.type}`).join(", ")}) → ${functionSignature.returnType}` : isFunctionProblem ? "Function configuration unavailable." : "Provide stdin for Run Code. Submit uses the problem test cases."}</p>
              </div>
              <button type="button" className="clear-input" onClick={() => isFunctionProblem ? setCustomCases([{arguments:functionParameters.map(()=>""),expected:""}]) : setCustomInput("")}>Clear</button>
            </div>
            {!isFunctionProblem ? <textarea
              className="custom-input-editor"
              value={customInput}
              onChange={(e) => setCustomInput(e.target.value)}
              placeholder="Example: hello"
              spellCheck="false"
            /> : functionConfigured ? <div className="function-custom-cases">{customCases.map((testCase,caseIndex)=><div className="function-custom-case" key={caseIndex}><h3>Custom input {caseIndex+1}</h3>{functionParameters.map((parameter,index)=><label key={parameter.name}>{parameter.name} <small>{parameter.type}</small><textarea placeholder={placeholderFor(parameter.type)} value={testCase.arguments[index]||""} onChange={(e)=>setCustomCases((items)=>items.map((item,i)=>i===caseIndex?{...item,arguments:item.arguments.map((value,j)=>j===index?e.target.value:value)}:item))}/></label>)}<label>Expected <small>optional</small><textarea placeholder={placeholderFor(functionSignature.returnType)} value={testCase.expected} onChange={(e)=>setCustomCases((items)=>items.map((item,i)=>i===caseIndex?{...item,expected:e.target.value}:item))}/></label><div className="custom-case-actions"><button type="button" onClick={()=>runCode(caseIndex)}>Run Input</button>{customCases.length>1&&<button type="button" onClick={()=>setCustomCases((items)=>items.filter((_,i)=>i!==caseIndex))}>Delete</button>}</div></div>)}<div className="custom-case-actions"><button type="button" onClick={()=>setCustomCases((items)=>[...items,{arguments:functionParameters.map(()=>""),expected:""}])}>+ Add Custom Input</button><button type="button" onClick={()=>runCode()}>Run All Inputs</button></div></div>:<div className="solver-error">Function configuration unavailable. Ask an administrator to complete the FUNCTION metadata.</div>}
          </div>

          {error && (
            <div className="solver-error">
              {error}
            </div>
          )}

          {result?.testCases && <div className="function-run-results">{result.testCases.map((item)=><div className={`submission-result ${item.passed===true?"accepted":item.passed===false?"wrong_answer":item.status?.toLowerCase()}`} key={item.caseNumber}><strong>Case {item.caseNumber} — {item.passed===true?"PASSED":item.passed===false?"FAILED":item.status}</strong><span>{item.runtimeMs} ms</span>{item.actual!==null&&<pre>Actual: {JSON.stringify(item.actual)}</pre>}{item.expected!==undefined&&<pre>Expected: {JSON.stringify(item.expected)}</pre>}{item.message&&<pre>{item.message}</pre>}</div>)}</div>}

          {result?.certificateProgress && <SubmissionMilestone key={result.id} progress={result.certificateProgress}/>}
          {result && !result.testCases && (
            <div
              className={`submission-result ${
                result.status?.toLowerCase()
              }`}
            >

              <div className="result-header">

                <div className="result-status">
                  {getStatusIcon()}

                  <strong>
                    {result.status}
                  </strong>
                </div>

                <div className="result-time">
                  <Clock3 size={16} />
                  <span>Runtime</span>
                  {result.executionTimeMs ?? 0} ms
                </div>

              </div>

              <div className="result-details">

                {result.totalTestCases !== undefined ? (
                  <div>
                    <span>Test Cases</span>
                    <strong>{result.passedTestCases ?? 0} / {result.totalTestCases ?? 0}</strong>
                  </div>
                ) : (
                  <div>
                    <span>Execution Status</span>
                    <strong>Run with custom input</strong>
                  </div>
                )}

                {result.output && (
                  <div>
                    <span>Output</span>

                    <pre>{result.output}</pre>
                  </div>
                )}

                {result.errorMessage && (
                  <div>
                    <span>Error</span>

                    <pre>{result.errorMessage}</pre>
                  </div>
                )}

                {result.testCaseResults?.map((item)=><div key={`${item.hidden}-${item.testNumber}`} className="judge-case-result">{item.hidden?<strong>{item.message}</strong>:<><strong>Public Test {item.testNumber} — {item.status}</strong><pre>Arguments: {JSON.stringify(item.arguments)}</pre><pre>Expected: {JSON.stringify(item.expected)}</pre><pre>Actual: {JSON.stringify(item.actual)}</pre></>}</div>)}

              </div>

            </div>
          )}

          <section className="solver-history">
            <h3>Your submissions for this problem</h3>

            {submissions.length === 0 ? (
              <p>No submissions yet.</p>
            ) : (
              <div className="solver-history-list">
                {submissions.slice(0, 5).map((submission) => (
                  <div className="solver-history-row" key={submission.id}>
                    <strong>{submission.status?.replaceAll("_", " ")}</strong>
                    <span>{submission.language?.toUpperCase()}</span>
                    <span>{submission.passedTestCases} / {submission.totalTestCases} cases</span>
                    <span>{new Date(submission.submittedAt).toLocaleString()}</span>
                  </div>
                ))}
              </div>
            )}
          </section>

        </section>

      </main>

    </div>
  );
}

export default ProblemSolver;
