import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  Search,
  RefreshCw,
  Shield,
  User,
  Users,
  Code2,
  ChevronDown,
} from "lucide-react";
import api from "../services/api";
import BrandLogo from "../components/BrandLogo";
import Pagination from "../components/Pagination";

function UserManagement() {
  const navigate = useNavigate();

  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState(null);
  const [error, setError] = useState("");
  const [page,setPage]=useState(0); const [pagination,setPagination]=useState(null);

  const loadUsers = async () => {
    try {
      setError("");

      const response = await api.get("/users/page",{params:{search,page,size:20}});
      setUsers(response.data.content); setPagination(response.data);
    } catch (err) {
      console.error("Failed to load users:", err);

      if (err.response?.status === 403) {
        setError("You do not have permission to view users.");
      } else {
        setError("Unable to load users.");
      }
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

    loadUsers();
  }, [navigate,page,search]);

  const handleSearch = async (value) => {
    setSearch(value);

    setPage(0);
  };

  const updateRole = async (userId, newRole) => {
    const confirmed = window.confirm(
      `Change this user's role to ${newRole}?`
    );

    if (!confirmed) {
      return;
    }

    try {
      setUpdatingId(userId);
      setError("");

      await api.put(`/users/${userId}/role`, null, {
        params: {
          role: newRole,
        },
      });

      await loadUsers();
    } catch (err) {
      console.error("Failed to update role:", err);

      if (err.response?.status === 403) {
        setError("You do not have permission to change user roles.");
      } else {
        setError("Unable to update user role.");
      }
    } finally {
      setUpdatingId(null);
    }
  };

  const totalUsers = users.length;

  const adminCount = users.filter(
    (user) => user.role === "ADMIN"
  ).length;

  const regularUserCount = users.filter(
    (user) => user.role === "USER"
  ).length;

  return (
    <div className="dashboard-layout">

      {/* SIDEBAR */}

      <aside className="sidebar">

        <div className="sidebar-brand">

          <BrandLogo compact />

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

          <button
            className="sidebar-item"
            onClick={() => navigate("/admin/problems")}
          >
            Problems
          </button>

          <button className="sidebar-item active">
            Users
          </button>

        </nav>

      </aside>

      {/* MAIN */}

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

            <h1>User Management</h1>

            <p>
              View users and manage their roles.
            </p>

          </div>

          <button
            className="primary-button"
            onClick={loadUsers}
            disabled={loading}
          >
            <RefreshCw
              size={18}
              className={loading ? "spin" : ""}
            />
            Refresh
          </button>

        </header>

        {/* STAT CARDS */}

        <section className="stats-grid">

          <div className="stat-card">

            <div className="stat-icon">
              <Users size={22} />
            </div>

            <div>
              <span>Total Users</span>
              <strong>{totalUsers}</strong>
            </div>

          </div>

          <div className="stat-card">

            <div className="stat-icon">
              <User size={22} />
            </div>

            <div>
              <span>Regular Users</span>
              <strong>{regularUserCount}</strong>
            </div>

          </div>

          <div className="stat-card">

            <div className="stat-icon">
              <Shield size={22} />
            </div>

            <div>
              <span>Administrators</span>
              <strong>{adminCount}</strong>
            </div>

          </div>

        </section>

        {/* USER SECTION */}

        <section className="dashboard-section">

          <div className="search-container">

            <Search size={20} />

            <input
              type="text"
              placeholder="Search by username or email..."
              value={search}
              onChange={(e) =>
                handleSearch(e.target.value)
              }
            />

          </div>

          {error && (
            <div className="error-message">
              {error}
            </div>
          )}

          {loading ? (

            <div className="loading">
              Loading users...
            </div>

          ) : users.length === 0 ? (

            <div className="empty-state">

              <Users size={45} />

              <h3>No users found</h3>

              <p>
                {search
                  ? "No users match your search."
                  : "There are no users to display."}
              </p>

            </div>

          ) : (

            <div className="problem-table">

              {/* TABLE HEADER */}

              <div
                className="table-header user-table-header"
              >
                <span>User</span>
                <span>Email</span>
                <span>Role</span>
                <span>Actions</span>
              </div>

              {/* USERS */}

              {users.map((user) => (

                <div
                  className="table-row user-table-row"
                  key={user.id}
                >

                  {/* USER */}

                  <div className="user-info">

                    <div className="user-avatar">
                      {user.username
                        ?.charAt(0)
                        .toUpperCase()}
                    </div>

                    <div>

                      <strong>
                        {user.username}
                      </strong>

                      <small>
                        ID: #{user.id}
                      </small>

                    </div>

                  </div>

                  {/* EMAIL */}

                  <span className="user-email">
                    {user.email}
                  </span>

                  {/* ROLE */}

                  <span
                    className={
                      user.role === "ADMIN"
                        ? "role-badge admin"
                        : "role-badge user"
                    }
                  >

                    {user.role === "ADMIN" ? (
                      <Shield size={15} />
                    ) : (
                      <User size={15} />
                    )}

                    {user.role}

                  </span>

                  {/* ACTION */}

                  <div className="role-action">

                    <button onClick={() => navigate(`/admin/users/${user.id}`)}>Analytics</button>

                    <div className="role-select-wrapper">

                      <select
                        value={user.role}
                        disabled={
                          updatingId === user.id
                        }
                        onChange={(e) =>
                          updateRole(
                            user.id,
                            e.target.value
                          )
                        }
                      >

                        <option value="USER">
                          USER
                        </option>

                        <option value="ADMIN">
                          ADMIN
                        </option>

                      </select>

                      <ChevronDown size={15} />

                    </div>

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

export default UserManagement;
