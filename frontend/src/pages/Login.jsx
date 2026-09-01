import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { Lock, User, ArrowRight } from "lucide-react";
import api from "../services/api";
import BrandLogo from "../components/BrandLogo";

function Login() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    username: "",
    password: "",
  });

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setForm({
      ...form,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    setError("");
    setLoading(true);

    try {
      const response = await api.post("/auth/login", form);

      const { token, username, role } = response.data;

      localStorage.setItem("algosphere_token", token);
      localStorage.setItem("algosphere_username", username);
      localStorage.setItem("algosphere_role", role);

      if (role === "ADMIN") {
        navigate("/admin");
      } else {
        navigate("/dashboard");
      }
    } catch (err) {
      if (err.response?.status === 401 || err.response?.status === 403) {
        setError("Invalid username or password.");
      } else {
        setError("Unable to connect to Verdixa server.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-background">
        <div className="login-card">

          <div className="brand">
            <BrandLogo className="auth-logo" />
            <p>Code. Execute. Evolve.</p>
          </div>

          <div className="login-header">
            <h2>Welcome back</h2>
            <p>Sign in to continue your coding journey.</p>
          </div>

          {error && (
            <div className="error-message">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit}>

            <div className="input-group">
              <label>Username</label>

              <div className="input-wrapper">
                <User size={19} />

                <input
                  type="text"
                  name="username"
                  placeholder="Enter your username"
                  value={form.username}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>

            <div className="input-group">
              <label>Password</label>

              <div className="input-wrapper">
                <Lock size={19} />

                <input
                  type="password"
                  name="password"
                  placeholder="Enter your password"
                  value={form.password}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>

            <button
              type="submit"
              className="login-button"
              disabled={loading}
            >
              {loading ? "Signing in..." : "Sign In"}

              {!loading && <ArrowRight size={19} />}
            </button>

          </form>

          <div className="register-link">
            <span>Don't have an account?</span>

            <Link to="/register">
              Create account
            </Link>
          </div>

        </div>
      </div>
    </div>
  );
}

export default Login;
