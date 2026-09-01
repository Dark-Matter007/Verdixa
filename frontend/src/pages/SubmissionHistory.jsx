import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { CheckCircle2, Clock3, FileCode2, XCircle } from "lucide-react";
import api from "../services/api";
import UserNavigation from "../components/UserNavigation";
import Pagination from "../components/Pagination";

function SubmissionHistory() {
  const navigate = useNavigate();
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [pagination, setPagination] = useState(null);
  const [page, setPage] = useState(0);

  useEffect(() => {
    const loadHistory = async () => {
      try {
        const userResponse = await api.get("/users/me");
        const response = await api.get(`/submissions/user/${userResponse.data.id}/page`, { params: { page, size: 20 } });
        setSubmissions(response.data.content || []); setPagination(response.data);
      } catch {
        setError("Unable to load your submission history.");
      } finally {
        setLoading(false);
      }
    };

    loadHistory();
  }, [page]);

  return (
    <div className="user-page">
      <header className="dashboard-navbar">
        <div className="dashboard-brand"><div className="dashboard-brand-icon"><FileCode2 size={22} /></div><div><h2>Verdixa</h2><span>Master Algorithms. Build Logic.</span></div></div>
        <UserNavigation active="history" />
      </header>
      <main className="user-page-container">
        <div className="page-heading"><div><p className="section-eyebrow">ACTIVITY</p><h1>Submission History</h1><p>Review every code submission and its result.</p></div></div>
        {loading && <div className="empty-state"><div className="loading-spinner" /><p>Loading submissions...</p></div>}
        {!loading && error && <div className="dashboard-error">{error}</div>}
        {!loading && !error && submissions.length === 0 && <div className="empty-state"><FileCode2 size={42} /><h3>No submissions yet</h3><p>Solve a problem to start building your history.</p></div>}
        {!loading && !error && submissions.length > 0 && <div className="history-list">
          {submissions.map((submission) => {
            const accepted = submission.status === "ACCEPTED";
            return <article className="history-card" key={submission.id} onClick={() => navigate(`/submissions/${submission.id}`)} role="link" tabIndex={0}>
              <div className={accepted ? "history-status accepted" : "history-status"}>{accepted ? <CheckCircle2 size={20} /> : <XCircle size={20} />}</div>
              <div className="history-main"><h2>{submission.problemTitle}</h2><p>{submission.language?.toUpperCase()} · {new Date(submission.submittedAt).toLocaleString()}</p></div>
              <div className={accepted ? "submission-chip accepted" : "submission-chip"}>{submission.status?.replaceAll("_", " ")}</div>
              <div className="history-metric"><Clock3 size={15} /> {submission.executionTimeMs ?? 0} ms</div>
              <div className="history-cases">{submission.passedTestCases} / {submission.totalTestCases} cases</div>
            </article>;
          })}
        </div>}
        {!loading && !error && <Pagination data={pagination} onChange={setPage} />}
      </main>
    </div>
  );
}

export default SubmissionHistory;
