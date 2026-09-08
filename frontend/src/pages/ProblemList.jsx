import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Plus,
  Search,
  Edit,
  Trash2,
  ListChecks,
  Code2,
  BarChart3,
} from "lucide-react";
import api from "../services/api";
import Pagination from "../components/Pagination";
import AdminShell from "../components/AdminShell";
import { EmptyState, LoadingState } from "../components/PageState";

function ProblemList() {
  const navigate = useNavigate();

  const [editorialStatuses,setEditorialStatuses]=useState({});
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

  useEffect(()=>{let active=true;setEditorialStatuses({});if(problems.length)api.get("/admin/editorial-statuses",{params:{ids:problems.map(p=>p.id).join(",")}}).then(r=>{if(active)setEditorialStatuses(r.data);}).catch(()=>{});return()=>{active=false;};},[problems]);
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
        <section className="dashboard-section vx-admin-problems">

          <div className="vx-admin-problems-toolbar">
          <div className="search-container vx-admin-problems-search">

            <Search size={20} />

            <input
              type="text"
              placeholder="Search problems..."
              value={search}
              onChange={(e) => {setSearch(e.target.value);setPage(0);}}
            />

          </div>
          {!loading && <p className="vx-admin-problems-count" aria-live="polite">{pagination?.totalElements ?? filteredProblems.length} problem{(pagination?.totalElements ?? filteredProblems.length) === 1 ? "" : "s"} in catalogue</p>}
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

            <div className="problem-table" role="table" aria-label="Problem catalogue">

              <div className="table-header" role="row">
                <span role="columnheader">Problem</span>
                <span role="columnheader">Difficulty</span>
                <span role="columnheader">Status</span>
                <span role="columnheader">Actions</span>
              </div>

              {filteredProblems.map((problem) => (

                <article className="table-row vx-admin-problem-row" role="row" key={problem.id}>

                  <div className="vx-admin-problem-identity" role="cell">
                    <strong>{problem.title}</strong>
                    <div className="vx-admin-problem-meta">
                      <span>ID #{problem.id}</span>
                      <span className="editorial-status">Editorial: {editorialStatuses[problem.id] || "Unavailable"}</span>
                    </div>
                  </div>

                  <div role="cell" className="vx-admin-problem-difficulty"><span
                    className={`difficulty ${problem.difficulty?.toLowerCase()}`}
                  >
                    {problem.difficulty}
                  </span></div>

                  <div role="cell"><span
                    className={
                      problem.active
                        ? "status active"
                        : "status inactive"
                    }
                  >
                    {problem.active ? "Active" : "Inactive"}
                  </span></div>

                  <div className="action-buttons" role="cell">

                    <button
                      type="button"
                      className="vx-problem-action"
                      title="Manage test cases"
                      aria-label={`Manage test cases for ${problem.title}`}
                      onClick={() => navigate(`/admin/problems/${problem.id}/testcases`)}
                    >
                      <ListChecks size={17} />
                    </button>

                    <button
                      type="button"
                      className="vx-problem-action"
                      title="Edit"
                      aria-label={`Edit ${problem.title}`}
                      onClick={() =>
                        navigate(`/admin/problems/edit/${problem.id}`)
                      }
                    >
                      <Edit size={17} />
                    </button>

                    <button type="button" className="vx-problem-action" title="Analytics" aria-label={`View analytics for ${problem.title}`} onClick={() => navigate(`/admin/problems/${problem.id}/analytics`)}><BarChart3 size={17} /></button>

                    <button
                      type="button"
                      className="vx-problem-action vx-problem-action--delete"
                      title="Delete"
                      aria-label={`Delete ${problem.title}`}
                      onClick={() => deleteProblem(problem.id)}
                    >
                      <Trash2 size={17} />
                    </button>

                  </div>

                </article>

              ))}

            </div>

          )}
          <Pagination data={pagination} onChange={setPage} />

        </section>

    </AdminShell>
  );
}

export default ProblemList;
