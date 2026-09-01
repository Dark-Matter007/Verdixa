import { useEffect, useState } from "react";
import { Crown, Trophy, Users } from "lucide-react";
import api from "../services/api";
import UserNavigation from "../components/UserNavigation";
import Pagination from "../components/Pagination";

function Leaderboard() {
  const [entries, setEntries] = useState([]);
  const [error, setError] = useState("");
  const [page,setPage]=useState(0); const [pagination,setPagination]=useState(null);
  const username = localStorage.getItem("algosphere_username");

  useEffect(() => {
    api.get("/users/leaderboard/page", {params:{page,size:20}})
      .then((response) => {setEntries(response.data.content || []);setPagination(response.data);})
      .catch(() => setError("Unable to load the leaderboard."));
  }, [page]);

  return (
    <div className="user-page">
      <header className="dashboard-navbar">
        <div className="dashboard-brand"><div className="dashboard-brand-icon"><Trophy size={22} /></div><div><h2>Verdixa</h2><span>Master Algorithms. Build Logic.</span></div></div>
        <UserNavigation active="leaderboard" />
      </header>
      <main className="user-page-container">
        <div className="page-heading"><div><p className="section-eyebrow">COMMUNITY</p><h1>Leaderboard</h1><p>Ranked by distinct accepted problems, then accepted submissions.</p></div></div>
        {error && <div className="dashboard-error">{error}</div>}
        {!error && entries.length === 0 && <div className="empty-state"><Users size={42} /><h3>No ranked users yet</h3><p>Accepted solutions will appear here.</p></div>}
        {!error && entries.length > 0 && <div className="leaderboard-table">
          <div className="leaderboard-row leaderboard-header-row"><span>Rank</span><span>Competitor</span><span>Solved</span><span>Accepted</span><span>Total</span><span>Acceptance</span></div>
          {entries.map((entry) => <div className={`leaderboard-row ${entry.username === username ? "current-user" : ""}`} key={entry.userId}>
            <span className={entry.rank <= 3 ? "rank top-rank" : "rank"}>{entry.rank <= 3 ? <Crown size={18} /> : `#${entry.rank}`}</span>
            <strong>{entry.username}{entry.username === username ? " (You)" : ""}</strong><span>{entry.solvedProblems}</span><span>{entry.acceptedSubmissionCount}</span><span>{entry.submissionCount}</span><span>{entry.acceptanceRate}%</span>
          </div>)}
        </div>}
        {!error && <Pagination data={pagination} onChange={setPage} />}
      </main>
    </div>
  );
}

export default Leaderboard;
