import { useState } from "react";
import { ChevronDown, ChevronUp, Plus, Terminal, Trash2 } from "lucide-react";
import SubmissionMilestone from "../SubmissionMilestone";
import { WorkspaceTabs } from "./WorkspaceControls";

const placeholderFor = (type) => ({ String: '"hello"', int: "42", Integer: "42", long: "42", double: "3.14", boolean: "true", "int[]": "[1,2,3]", "long[]": "[1,2,3]", "double[]": "[1.5,2.5]", "String[]": '["a","b"]' }[type] || "JSON value");
const present = (value) => value !== undefined && value !== null;
const statusTone = (status, passed) => {
  if (passed === true || ["ACCEPTED", "SUCCESS", "PASSED"].includes(status)) return "success";
  if (/(RUNTIME|COMPILATION|ERROR)/.test(status || "")) return "error";
  return "failure";
};
function Output({ label, value }) { return present(value) && <div className="sw-output"><span>{label}</span><pre data-i18n-ignore translate="no">{typeof value === "string" ? value : JSON.stringify(value)}</pre></div>; }

export function ResultPanel({ result, busy, operation }) {
  if (busy) return <div className="sw-empty" role="status"><Terminal size={22}/><strong>{operation === "submit" ? "Submitting solution…" : "Running code…"}</strong><p>Waiting for the execution service.</p></div>;
  if (!result) return <div className="sw-empty"><Terminal size={22}/><strong>Ready when you are</strong><p>Run your code to inspect output, or submit to check the problem test cases.</p></div>;
  return <div className="sw-results">
    {result.status && <div className={`sw-verdict is-${statusTone(result.status)}`}><strong>{result.status.replaceAll("_", " ")}</strong>{present(result.executionTimeMs) && <span>Runtime <b>{result.executionTimeMs} ms</b></span>}{present(result.passedTestCases) && present(result.totalTestCases) && <span>Test Cases <b>{result.passedTestCases} / {result.totalTestCases}</b></span>}</div>}
    <Output label="Output" value={result.output}/><Output label="Error" value={result.errorMessage}/><Output label="Message" value={result.message}/>
    {result.testCases?.map((item) => <article className={`sw-case-result is-${statusTone(item.status, item.passed)}`} key={item.caseNumber}><div className="sw-section-heading"><strong>Case {item.caseNumber} — {item.passed === true ? "PASSED" : item.passed === false ? "FAILED" : item.status}</strong>{present(item.runtimeMs) && <span>{item.runtimeMs} ms</span>}</div><Output label="Actual" value={item.actual}/><Output label="Expected" value={item.expected}/><Output label="Message" value={item.message}/></article>)}
    {result.testCaseResults?.map((item) => <article className={`sw-case-result is-${statusTone(item.status)}`} key={`${item.hidden}-${item.testNumber}`}>{item.hidden ? <strong>{item.message}</strong> : <><strong>Public Test {item.testNumber} — {item.status}</strong><Output label="Arguments" value={item.arguments}/><Output label="Expected" value={item.expected}/><Output label="Actual" value={item.actual}/></>}</article>)}
    {result.certificateProgress && <SubmissionMilestone key={result.id} progress={result.certificateProgress}/>}
  </div>;
}

export function SubmissionPanel({ submissions }) {
  const hasRuntime = submissions.some((item) => present(item.executionTimeMs));
  return <section className="sw-submissions" aria-label="Your submissions for this problem">{submissions.length === 0 ? <div className="sw-empty"><strong>No submissions yet.</strong><p>Your official submissions will appear here.</p></div> : <table><thead><tr><th>Status</th><th>Language</th><th>Cases</th>{hasRuntime && <th>Runtime</th>}<th>Submitted</th></tr></thead><tbody>{submissions.map((item) => <tr key={item.id}><td><strong className={`sw-status is-${statusTone(item.status)}`}>{item.status?.replaceAll("_", " ")}</strong></td><td data-i18n-ignore>{item.language?.toUpperCase()}</td><td>{present(item.passedTestCases) && present(item.totalTestCases) ? `${item.passedTestCases} / ${item.totalTestCases}` : "—"}</td>{hasRuntime && <td>{present(item.executionTimeMs) ? `${item.executionTimeMs} ms` : "—"}</td>}<td>{item.submittedAt && <time dateTime={item.submittedAt}>{new Date(item.submittedAt).toLocaleString()}</time>}</td></tr>)}</tbody></table>}</section>;
}

function TestCasesPanel({ problem, language, customInput, setCustomInput, customCases, setCustomCases, run, busy }) {
  const [selected, setSelected] = useState(0);
  const functionMode = problem.executionMode === "FUNCTION";
  const signature = problem.functionSignature || {};
  const params = signature.parameters || [];
  const configured = signature.functionName && signature.returnType;
  const index = Math.min(selected, customCases.length - 1);
  const current = customCases[index];
  const name = language === "python" ? signature.functionName?.replace(/([a-z0-9])([A-Z])/g, "$1_$2").toLowerCase() : signature.functionName;
  const clear = () => { if (functionMode) { setCustomCases([{ arguments: params.map(() => ""), expected: "" }]); setSelected(0); } else setCustomInput(""); };
  const update = (key, value) => setCustomCases((cases) => cases.map((item, i) => i === index ? { ...item, [key]: value } : item));
  return <div className="sw-testcases"><div className="sw-test-heading"><code data-i18n-ignore>{functionMode ? configured ? `${name}(${params.map((p) => `${p.name}: ${p.type}`).join(", ")}) → ${signature.returnType}` : "Function configuration unavailable." : "stdin"}</code><button onClick={clear} disabled={busy}>Clear</button></div>
    {!functionMode && <><p className="sw-muted">Provide stdin for Run Code. Submit uses the problem test cases.</p><label className="sw-input-label">Input<textarea className="sw-test-input" value={customInput} onChange={(event) => setCustomInput(event.target.value)} placeholder="Example: hello" spellCheck={false}/></label><div className="sw-actions"><button onClick={() => run()} disabled={busy}>Run Input</button></div></>}
    {functionMode && !configured && <p role="alert">Function configuration unavailable. Ask an administrator to complete the FUNCTION metadata.</p>}
    {functionMode && configured && <><div className="sw-case-tabs"><WorkspaceTabs label="Custom inputs" idPrefix="custom" items={customCases.map((_, i) => ({ value: i, label: `Custom ${i + 1}` }))} value={index} onChange={setSelected}/><button title="Add Custom Input" aria-label="Add Custom Input" onClick={() => { setCustomCases((cases) => [...cases, { arguments: params.map(() => ""), expected: "" }]); setSelected(customCases.length); }} disabled={busy}><Plus size={14}/><span>Add</span></button></div>
      <div className="sw-test-fields" role="tabpanel" id={`custom-panel-${index}`} aria-labelledby={`custom-${index}`}>
        {params.map((parameter, parameterIndex) => <label className="sw-input-label" key={parameter.name}><span data-i18n-ignore>{parameter.name} <small>{parameter.type}</small></span><textarea spellCheck={false} placeholder={placeholderFor(parameter.type)} value={current.arguments[parameterIndex] || ""} onChange={(event) => update("arguments", params.map((_, i) => i === parameterIndex ? event.target.value : current.arguments[i] || ""))}/></label>)}
        <label className="sw-input-label">Expected Output <small>optional</small><textarea spellCheck={false} placeholder={placeholderFor(signature.returnType)} value={current.expected} onChange={(event) => update("expected", event.target.value)}/></label>
      </div><div className="sw-actions"><button onClick={() => run(index)} disabled={busy}>Run Input</button><button onClick={() => run()} disabled={busy}>Run All Inputs</button>{customCases.length > 1 && <button className="sw-delete-case" title="Delete custom input" aria-label="Delete custom input" disabled={busy} onClick={() => { setCustomCases((cases) => cases.filter((_, i) => i !== index)); setSelected(Math.max(0, index - 1)); }}><Trash2 size={14}/></button>}</div></>}
  </div>;
}

export default function ExecutionConsole({ tab, setTab, collapsed, toggleCollapsed, ...props }) {
  return <section className={`sw-console ${collapsed ? "is-collapsed" : ""}`} id="sw-console" aria-label="Execution console">
    <div className="sw-console-bar"><Terminal size={15}/><WorkspaceTabs label="Execution console" idPrefix="console" items={[{ value: "tests", label: "Test Cases" }, { value: "result", label: "Result" }, { value: "submissions", label: "Submissions" }]} value={tab} onChange={setTab}/><span className="sw-console-status" role="status">{props.busy ? "Working…" : ""}</span><button className="sw-icon" aria-label={collapsed ? "Expand console" : "Collapse console"} title={collapsed ? "Expand console" : "Collapse console"} aria-expanded={!collapsed} onClick={toggleCollapsed}>{collapsed ? <ChevronUp size={16}/> : <ChevronDown size={16}/>}</button></div>
    <div className="sw-console-content" hidden={collapsed} role="tabpanel" id={`console-panel-${tab}`} aria-labelledby={`console-${tab}`} tabIndex={0}>
      <div hidden={tab !== "tests"}><TestCasesPanel {...props}/></div>
      {tab === "result" && <ResultPanel result={props.result} busy={props.busy} operation={props.operation}/>}
      {tab === "submissions" && <SubmissionPanel submissions={props.submissions}/>}
    </div>
  </section>;
}
