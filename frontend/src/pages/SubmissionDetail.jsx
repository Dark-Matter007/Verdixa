import { useContext, useEffect,useState } from "react";
import { useNavigate,useParams } from "react-router-dom";
import Editor from "@monaco-editor/react";
import api from "../services/api";
import UserNavigation from "../components/UserNavigation";
import { ThemeContext } from "../components/ThemeProvider";
export default function SubmissionDetail() {
  const { theme } = useContext(ThemeContext);
  const { id } = useParams();
  const navigate = useNavigate();
  const [submission, setSubmission] = useState(null);
  const [error, setError] = useState("");
  useEffect(() => { api.get(`/submissions/${id}`).then((r) => setSubmission(r.data)).catch(() => setError("Submission not found or access denied.")); }, [id]);
  if (error) return <div className="user-page"><p>{error}</p></div>;
  if (!submission) return <div className="user-page"><p>Loading submission…</p></div>;
  const replay = () => navigate(`/problems/${submission.problemId}`, { state: { replay: { language: submission.language, sourceCode: submission.sourceCode } } });
  return <div className="user-page"><UserNavigation active="history"/><main className="user-page-container"><h1>{submission.problemTitle}</h1><div className="submission-detail-meta"><strong>{submission.status}</strong><span>{submission.language?.toUpperCase()}</span><span>{submission.executionTimeMs || 0} ms</span><span>{submission.passedTestCases}/{submission.totalTestCases}</span><span>{new Date(submission.submittedAt).toLocaleString()}</span></div><Editor theme={theme === "dark" ? "vs-dark" : "vs"} height="55vh" language={submission.language === "cpp" ? "cpp" : submission.language} value={submission.sourceCode} options={{ readOnly: true, minimap: { enabled: false } }} /><div className="form-actions"><button onClick={replay}>OPEN IN EDITOR</button><button onClick={replay}>RETRY</button></div></main></div>;
}
