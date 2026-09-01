import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowRight, Lock, Mail, User } from "lucide-react";
import api from "../services/api";
import BrandLogo from "../components/BrandLogo";

function Register() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: "", email: "", password: "", confirmPassword: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const submit = async (event) => {
    event.preventDefault();
    if (form.password !== form.confirmPassword) return setError("Passwords do not match.");
    setLoading(true); setError("");
    try {
      await api.post("/auth/register", { username: form.username.trim(), email: form.email.trim(), password: form.password });
      navigate("/login", { replace: true, state: { registered: true } });
    } catch (err) { setError(err.response?.data?.message || "Unable to create your account."); }
    finally { setLoading(false); }
  };
  const fields = [["username", "Username", User, "text"], ["email", "Email", Mail, "email"], ["password", "Password", Lock, "password"], ["confirmPassword", "Confirm password", Lock, "password"]];
  return <div className="login-page"><div className="login-background"><div className="login-card">
    <div className="brand"><BrandLogo className="auth-logo" /><p>Code. Execute. Evolve.</p></div>
    <div className="login-header"><h2>Create account</h2><p>Register as a Verdixa user.</p></div>
    {error && <div className="error-message">{error}</div>}
    <form onSubmit={submit}>{fields.map(([name, label, Icon, type]) => <div className="input-group" key={name}><label>{label}</label><div className="input-wrapper"><Icon size={19}/><input name={name} type={type} value={form[name]} onChange={(e) => setForm({...form, [name]: e.target.value})} required minLength={name.includes("password") ? 6 : undefined}/></div></div>)}
      <button type="submit" className="login-button" disabled={loading}>{loading ? "Creating account..." : "Create account"}<ArrowRight size={19}/></button></form>
    <div className="register-link"><span>Already have an account?</span><Link to="/login">Sign in</Link></div>
  </div></div></div>;
}
export default Register;
