import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Search,
  RefreshCw,
  Shield,
  User,
  Users,
} from "lucide-react";
import api from "../services/api";
import Pagination from "../components/Pagination";
import AdminShell from "../components/AdminShell";
import Avatar from "../components/Avatar";
import { EmptyState, LoadingState } from "../components/PageState";

function RoleSwitcher({ role, disabled, onChange }) {
  const options = [
    { value: "USER", label: "User", Icon: User },
    { value: "ADMIN", label: "Admin", Icon: Shield },
  ];

  return <div className="role-switcher" aria-label="Assign user role">
    <span className="role-switcher-label">Access role</span>
    <div className="role-switcher-options" role="radiogroup" aria-label="User role">
      {options.map(({ value, label, Icon }) => {
        const selected = role === value;
        return <button
          key={value}
          type="button"
          role="radio"
          aria-checked={selected}
          className={selected ? "active" : ""}
          disabled={disabled || selected}
          onClick={() => onChange(value)}
        >
          <Icon size={15} aria-hidden="true" />
          <span>{label}</span>
        </button>;
      })}
    </div>
  </div>;
}

function UserManagement() {
  const navigate = useNavigate();

  const [milestones,setMilestones]=useState({});
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

  useEffect(()=>{let active=true;setMilestones({});if(users.length)api.get("/admin/user-milestones",{params:{ids:users.map(u=>u.id).join(",")}}).then(r=>{if(active)setMilestones(r.data);}).catch(()=>{});return()=>{active=false;};},[users]);
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
    <AdminShell eyebrow="Identity control" title="User registry" description="Review identities, account roles, and user-level performance records." actions={
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
    }>

        {/* STAT CARDS */}

        <section className="stats-grid vx-user-registry-metrics" aria-label="User registry totals">

          <div className="stat-card vx-user-registry-metric">

            <div className="stat-icon">
              <Users size={22} />
            </div>

            <div>
              <span>Total Users</span>
              <strong>{totalUsers}</strong>
            </div>

          </div>

          <div className="stat-card vx-user-registry-metric">

            <div className="stat-icon">
              <User size={22} />
            </div>

            <div>
              <span>Regular Users</span>
              <strong>{regularUserCount}</strong>
            </div>

          </div>

          <div className="stat-card vx-user-registry-metric">

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

        <section className="dashboard-section vx-user-registry">

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
            <LoadingState label="Loading user registry" />

          ) : users.length === 0 ? (

            <EmptyState title="No users found" icon={Users}>
                {search
                  ? "No users match your search."
                  : "There are no users to display."}
            </EmptyState>

          ) : (

            <div className="problem-table user-management-table">

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

                    <Avatar name={user.username} size="md" />

                    <div>

                      <strong>
                        {user.username}
                      </strong>

                      <small>
                        ID: #{user.id}
                        {milestones[user.id] && <span className="user-milestone-summary">{milestones[user.id].solved} solved · Highest: {milestones[user.id].highestMilestone || "None"} · {milestones[user.id].certificates} certificates</span>}
                      </small>

                    </div>

                  </div>

                  {/* EMAIL */}

                  <span className="user-email" data-label="Email">
                    {user.email}
                  </span>

                  {/* ROLE */}

                  <span
                    data-label="Current role"
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

                  <div className="role-action" data-label="Access controls">
                    <RoleSwitcher role={user.role} disabled={updatingId === user.id} onChange={(role) => updateRole(user.id, role)} />
                    <button className="role-analytics-link" onClick={() => navigate(`/admin/users/${user.id}`)}>Analytics &amp; Certificates</button>
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

export default UserManagement;
