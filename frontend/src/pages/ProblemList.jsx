import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Plus,
  Search,
  Edit,
  Trash2,
  ListChecks,
  Code2,
} from "lucide-react";
import api from "../services/api";
import Pagination from "../components/Pagination";
import AdminShell from "../components/AdminShell";
import { EmptyState, LoadingState } from "../components/PageState";

function ProblemList() {
  const navigate = useNavigate();

  const [problems, setProblems] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page,setPage]=useState(0); const [pagination,setPagination]=useState(null);

  const loadProblems = async () => {
    try {
      setError("");
      const response = await api.get("/problems/admin/page", {params:{search,page,size:20}});
      setProblems(response.data.content);setPagination(response.data);
    } catch (error) {
      console.error("Failed to load problems:", error);
      setError("Unable to load problems.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const role = localStorage.getItem("algosphere_role");

    if (role !== "ADMIN") {
      navigate("/dashboard");
      return;
    }

    loadProblems();
  }, [navigate,page,search]);

  const deleteProblem = async (id) => {
    const confirmed = window.confirm(
      "Are you sure you want to delete this problem?"
    );

    if (!confirmed) return;

    try {
      await api.delete(`/problems/${id}`);
      await loadProblems();
    } catch (error) {
      console.error("Failed to delete problem:", error);
      alert("Unable to delete problem.");
    }
  };

  const filteredProblems = problems;

  return (
    <AdminShell title="Problem Management" description="Create, configure, publish, and review the platform's problem catalogue." actions={
          <button
            className="primary-button"
            onClick={() => navigate("/admin/problems/new")}
          >
            <Plus size={18} />
            Add Problem
          </button>
    }>
        <section className="dashboard-section">

          <div className="search-container">

            <Search size={20} />

            <input
              type="text"
              placeholder="Search problems..."
              value={search}
              onChange={(e) => {setSearch(e.target.value);setPage(0);}}
            />

          </div>

          {error && <div className="error-message">{error}</div>}
          {loading ? (
            <LoadingState label="Loading problem catalogue" />
          ) : filteredProblems.length === 0 ? (
            <EmptyState title="No problems found" icon={Code2}>
              {search ? "No problems match your search." : "Create your first coding problem."}
              {!search && (
                <button
                  className="primary-button"
                  onClick={() => navigate("/admin/problems/new")}
                >
                  <Plus size={18} />
                  Create Problem
                </button>
              )}
            </EmptyState>
          ) : (

            <div className="problem-table">

              <div className="table-header">
                <span>Problem</span>
                <span>Difficulty</span>
                <span>Status</span>
                <span>Actions</span>
              </div>

              {filteredProblems.map((problem) => (

                <div className="table-row" key={problem.id}>

                  <div>
                    <strong>{problem.title}</strong>

                    <small>
                      ID: #{problem.id}
                    </small>
                  </div>

                  <span
                    className={`difficulty ${problem.difficulty?.toLowerCase()}`}
                  >
                    {problem.difficulty}
                  </span>

                  <span
                    className={
                      problem.active
                        ? "status active"
                        : "status inactive"
                    }
                  >
                    {problem.active ? "Active" : "Inactive"}
                  </span>

                  <div className="action-buttons">

                    <button
                      title="Manage test cases"
                      onClick={() => navigate(`/admin/problems/${problem.id}/testcases`)}
                    >
                      <ListChecks size={17} />
                    </button>

                    <button
                      title="Edit"
                      onClick={() =>
                        navigate(`/admin/problems/edit/${problem.id}`)
                      }
                    >
                      <Edit size={17} />
                    </button>

                    <button title="Analytics" onClick={() => navigate(`/admin/problems/${problem.id}/analytics`)}>Analytics</button>

                    <button
                      title="Delete"
                      onClick={() => deleteProblem(problem.id)}
                    >
                      <Trash2 size={17} />
                    </button>

                  </div>

                </div>

              ))}

            </div>

          )}
          <Pagination data={pagination} onChange={setPage} />

        </section>

    </AdminShell>
  );
}

export default ProblemList;
