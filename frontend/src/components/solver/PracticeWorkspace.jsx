import { useCallback, useEffect, useRef, useState } from "react";
import { ArrowLeft, Bookmark, ChevronLeft, ChevronRight, Code2, FileText, LayoutTemplate, Maximize2, Minimize2, PanelLeftOpen, Play, RotateCcw, Send, Terminal } from "lucide-react";
import Editor from "@monaco-editor/react";
import BrandLogo from "../BrandLogo";
import UserAccountMenu from "../UserAccountMenu";
import ProblemReadingPane from "./ProblemReadingPane";
import ExecutionConsole from "./ExecutionConsole";
import { WorkspaceMenu, WorkspaceSplitter, WorkspaceTabs } from "./WorkspaceControls";
import useWorkspaceLayout from "./useWorkspaceLayout";
import "./workspace.css";

const languages = [{ value: "java", label: "Java" }, { value: "cpp", label: "C++" }, { value: "python", label: "Python" }];

export default function PracticeWorkspace(props) {
  const { problem, navigate, theme, language, changeLanguage, sourceCode, setSourceCode, setLanguageSources, submitting, runCode, submitCode, resetCurrentLanguage, result, error, submissions, bookmarked, toggleBookmark, personalLists, addToList, navigation } = props;
  const [layout, updateLayout, resetLayout] = useWorkspaceLayout();
  const [focus, setFocus] = useState(false);
  const [mobile, setMobile] = useState("problem");
  const [consoleTab, setConsoleTab] = useState("tests");
  const [operation, setOperation] = useState(null);
  const splitRoot = useRef(null);
  const editorRoot = useRef(null);
  const editor = useRef(null);
  const monacoApi = useRef(null);
  const executionLock = useRef(false);
  const callbacks = useRef({});
  const problemHidden = focus || layout.problemCollapsed;
  const collapsed = focus || layout.consoleCollapsed;
  const showConsole = (tab) => { setConsoleTab(tab); updateLayout({ consoleCollapsed: false }); setFocus(false); };
  const execute = async (kind, index = null) => {
    if (submitting || executionLock.current) return;
    executionLock.current = true;
    setOperation(kind);
    showConsole("result");
    setMobile("tests");
    try { await (kind === "submit" ? submitCode() : runCode(index)); }
    finally { executionLock.current = false; setOperation(null); }
  };
  const toggleFocus = () => { setFocus((value) => !value); setMobile("code"); requestAnimationFrame(() => editor.current?.focus()); };
  useEffect(() => { callbacks.current = { execute, toggleFocus, focus }; });
  useEffect(() => {
    const onKeyDown = (event) => {
      if (event.isComposing || event.repeat || event.defaultPrevented) return;
      if ((event.ctrlKey || event.metaKey) && event.key === "Enter" && !event.altKey) {
        if (event.target?.closest('[role="dialog"], dialog, .sw-menu, .vx-account-control') || document.querySelector('dialog[open]')) return;
        event.preventDefault(); event.stopPropagation(); callbacks.current.execute(event.shiftKey ? "submit" : "run");
      }
      if (event.key === "Escape" && callbacks.current.focus && !document.querySelector('dialog[open], [role="listbox"], [role="menu"], .suggest-widget.visible, .find-widget.visible')) setFocus(false);
    };
    // Capture Enter before Monaco handles it; ordinary typing and editor Escape remain intact.
    window.addEventListener("keydown", onKeyDown, true);
    return () => window.removeEventListener("keydown", onKeyDown, true);
  }, []);
  const mountEditor = (instance, monaco) => { editor.current = instance; monacoApi.current = monaco; };
  const configureThemes = useCallback((monaco) => {
    const css = getComputedStyle(editorRoot.current);
    const color = (name) => css.getPropertyValue(name).trim();
    for (const [name, base] of [["verdixa-dark", "vs-dark"], ["verdixa-light", "vs"]]) {
      monaco.editor.defineTheme(name, { base, inherit: true, rules: [], colors: { "editor.background": color("--solver-editor"), "editor.foreground": color("--solver-text"), "editorLineNumber.foreground": color("--solver-muted"), "editor.lineHighlightBackground": color("--solver-surface-2"), "editorCursor.foreground": color("--solver-accent"), "editor.selectionBackground": color("--solver-selection") } });
    }
  }, []);
  useEffect(() => {
    const frame = requestAnimationFrame(() => {
      if (!monacoApi.current) return;
      configureThemes(monacoApi.current);
      monacoApi.current.editor.setTheme(`verdixa-${theme}`);
    });
    return () => cancelAnimationFrame(frame);
  }, [theme, configureThemes]);
  const solved = submissions.some((item) => item.status === "ACCEPTED");
  return <div className={`practice-workspace ${focus ? "sw-focus" : ""} ${problemHidden ? "sw-problem-hidden" : ""}`} data-mobile-view={mobile} data-theme={theme}>
    <header className="sw-header"><button className="sw-back" onClick={() => navigate("/problems")}><ArrowLeft size={16}/><span>Problems</span></button><div className="sw-brand"><BrandLogo compact/><span>Problem Workspace</span></div><UserAccountMenu/></header>
    <div className="sw-context"><div className="sw-problem-title"><h1>{problem.title}</h1>{problem.difficulty && <span className="sw-difficulty">{problem.difficulty}</span>}{submissions.length > 0 && <span className="sw-solve-state">{solved ? "Solved" : "Attempted"}</span>}</div><div className="sw-context-actions"><button className={`sw-icon ${bookmarked ? "is-bookmarked" : ""}`} onClick={toggleBookmark} aria-label={bookmarked ? "Remove bookmark" : "Bookmark problem"} title={bookmarked ? "Remove bookmark" : "Bookmark problem"} aria-pressed={bookmarked}><Bookmark size={16} fill={bookmarked ? "currentColor" : "none"}/></button>{personalLists.length > 0 && <WorkspaceMenu label="Add problem to list" options={personalLists.map((item) => ({ value: item.id, label: item.name }))} onChange={addToList}/>}
      <nav className="sw-navigation" aria-label="Problem navigation"><button disabled={!navigation.previous} onClick={() => navigate(`/problems/${navigation.previous.id}`)} title="Previous problem" aria-label="Previous problem"><ChevronLeft size={16}/><span>Previous</span></button><button disabled={!navigation.next} onClick={() => navigate(`/problems/${navigation.next.id}`)} title="Next problem" aria-label="Next problem"><span>Next</span><ChevronRight size={16}/></button></nav><button className="sw-icon" onClick={() => { resetLayout(); setFocus(false); }} aria-label="Reset layout" title="Reset layout"><LayoutTemplate size={16}/></button></div></div>
    <div className="sw-mobile-nav"><WorkspaceTabs label="Mobile workspace" idPrefix="mobile" items={[{ value: "problem", label: "Problem", icon: <FileText size={15}/> }, { value: "code", label: "Code", icon: <Code2 size={15}/> }, { value: "tests", label: "Tests", icon: <Terminal size={15}/> }]} value={mobile} onChange={(value) => { setMobile(value); setFocus(false); if (value === "problem") updateLayout({ problemCollapsed: false }); if (value === "tests") updateLayout({ consoleCollapsed: false }); }}/></div>
    {error && <div className="sw-error" role="alert">{error}</div>}
    <main ref={splitRoot} className="sw-main" style={{ "--problem-share": `${layout.split}%` }}>
      <div className="sw-problem-slot" id="mobile-panel-problem" role="region" aria-label="Problem"><ProblemReadingPane {...props} onCollapse={() => updateLayout({ problemCollapsed: true })}/></div>
      {!problemHidden && <WorkspaceSplitter orientation="vertical" label="Resize problem pane" controls="sw-problem" value={layout.split} min={28} max={62} onChange={(split) => updateLayout({ split })} measure={(event) => { const rect = splitRoot.current.getBoundingClientRect(); return (event.clientX - rect.left) / rect.width * 100; }}/>}
      <section className="sw-editor-pane" ref={editorRoot} aria-label="Coding workspace" style={{ "--console-height": `${layout.consoleHeight}px` }}>
        <div className="sw-editor-toolbar" id="mobile-panel-code"><div className="sw-editor-identity">{problemHidden && <button className="sw-icon" title="Restore problem pane" aria-label="Restore problem pane" onClick={() => { setFocus(false); updateLayout({ problemCollapsed: false }); }}><PanelLeftOpen size={16}/></button>}<Code2 size={16}/><span className="sw-editor-label">Code Editor</span><WorkspaceMenu label="Programming language" value={language} options={languages} onChange={changeLanguage} disabled={submitting}/></div>
          <div className="sw-editor-actions"><button className="sw-icon" disabled={submitting} onClick={resetCurrentLanguage} title="Reset current language code" aria-label="Reset code"><RotateCcw size={15}/></button><button className="sw-icon" onClick={toggleFocus} title={focus ? "Exit focus mode (Esc)" : "Focus editor"} aria-label={focus ? "Exit focus mode" : "Focus editor"} aria-pressed={focus}>{focus ? <Minimize2 size={15}/> : <Maximize2 size={15}/>}</button><button className="sw-run" onClick={() => execute("run")} disabled={submitting} title="Run Code (Ctrl/Cmd + Enter)"><Play size={14}/><span>{operation === "run" ? "Running…" : "Run Code"}</span></button><button className="sw-primary" onClick={() => execute("submit")} disabled={submitting} title="Submit (Ctrl/Cmd + Shift + Enter)"><Send size={14}/><span>{operation === "submit" ? "Submitting…" : "Submit"}</span></button></div></div>
        <div className="sw-monaco" data-i18n-ignore translate="no"><Editor height="100%" language={language} value={sourceCode} beforeMount={configureThemes} onMount={mountEditor} onChange={(value) => { const next = value ?? ""; setSourceCode(next); setLanguageSources((previous) => ({ ...previous, [language]: next })); }} theme={`verdixa-${theme}`} options={{ minimap: { enabled: false }, fontSize: 14, tabSize: 2, automaticLayout: true, scrollBeyondLastLine: false, padding: { top: 20, bottom: 16 }, overviewRulerBorder: false, renderLineHighlight: "line", lineNumbersMinChars: 3, folding: true, ariaLabel: "Code editor" }}/></div>
        <div className="sw-editor-status"><span>{focus ? "Focus mode · Esc to return" : "Practice"}</span><span data-i18n-ignore>UTF-8 · Spaces: 2</span></div>
        {!collapsed && <WorkspaceSplitter orientation="horizontal" label="Resize execution console" controls="sw-console" value={layout.consoleHeight} min={160} max={480} onChange={(consoleHeight) => updateLayout({ consoleHeight })} measure={(event) => editorRoot.current.getBoundingClientRect().bottom - event.clientY}/>}
        <div className="sw-console-slot" id="mobile-panel-tests" style={{ "--console-size": collapsed ? "42px" : "var(--console-height)" }}><ExecutionConsole {...props} busy={submitting} operation={operation} result={result} tab={consoleTab} setTab={showConsole} collapsed={collapsed} toggleCollapsed={() => { setFocus(false); updateLayout({ consoleCollapsed: !collapsed }); }} run={(index) => execute("run", index)}/></div>
      </section>
    </main>
  </div>;
}

export function WorkspaceLoading() {
  return <div className="practice-workspace sw-loading" aria-busy="true"><header className="sw-header"><BrandLogo compact/><span role="status">Loading problem...</span></header><div className="sw-context"><i/></div><div className="sw-skeleton-body" aria-hidden="true"><div>{Array.from({ length: 8 }, (_, i) => <i key={i}/>)}</div><div>{Array.from({ length: 12 }, (_, i) => <i key={i}/>)}</div></div></div>;
}

export function WorkspaceUnavailable({ message, retry, navigate }) {
  return <div className="practice-workspace"><header className="sw-header"><BrandLogo compact/><span>Problem Workspace</span><UserAccountMenu/></header><main className="sw-unavailable"><FileText size={28}/><h1>Problem unavailable</h1>{message && <p role="alert">{message}</p>}<div className="sw-actions"><button onClick={() => navigate("/problems")}>Back to Problems</button><button className="sw-primary" onClick={retry}>Retry</button></div></main></div>;
}
