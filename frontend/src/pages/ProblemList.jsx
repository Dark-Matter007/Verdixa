import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  Plus,
  Search,
  Edit,
  Trash2,
  ListChecks,
  Code2,
} from "lucide-react";
import api from "../services/api";
import BrandLogo from "../components/BrandLogo";
import Pagination from "../components/Pagination";

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
    <div className="dashboard-layout">

      <aside className="sidebar">

        <div className="sidebar-brand">
          <BrandLogo compact />

          {error && <div className="error-message">{error}</div>}

          <div>
            <strong>Verdixa</strong>
            <span>ADMIN</span>
          </div>
        </div>

        <nav className="sidebar-nav">

          <button
            className="sidebar-item"
            onClick={() => navigate("/admin")}
          >
            Dashboard
          </button>

          <button className="sidebar-item active">
            Problems
          </button>

        </nav>

      </aside>

      <main className="dashboard-main">

        <header className="dashboard-header">

          <div>
            <button
              className="back-button"
              onClick={() => navigate("/admin")}
            >
              <ArrowLeft size={18} />
              Back to Dashboard
            </button>

            <h1>Problem Management</h1>
            <p>Create, edit and manage coding problems.</p>
          </div>

          <button
            className="primary-button"
            onClick={() => navigate("/admin/problems/new")}
          >
            <Plus size={18} />
            Add Problem
          </button>

        </header>

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

          {loading ? (
            <div className="loading">
              Loading problems...
            </div>
          ) : filteredProblems.length === 0 ? (
            <div className="empty-state">

              <Code2 size={45} />

              <h3>No problems found</h3>

              <p>
                {search
                  ? "No problems match your search."
                  : "Create your first coding problem."}
              </p>

              {!search && (
                <button
                  className="primary-button"
                  onClick={() => navigate("/admin/problems/new")}
                >
                  <Plus size={18} />
                  Create Problem
                </button>
              )}

            </div>
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

      </main>

    </div>
  );
}

export default ProblemList;
