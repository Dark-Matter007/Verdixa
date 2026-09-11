import { useEffect, useState } from "react";
import { LoaderCircle } from "lucide-react";
import { useNavigate, useSearchParams } from "react-router-dom";
import api from "../services/api";
import BrandLogo from "../components/BrandLogo";

export default function OAuthCallback() {
  const navigate = useNavigate(); const [params] = useSearchParams(); const [error, setError] = useState("");
  useEffect(() => {
    const code = params.get("code");
    if (!code) { setError("Your sign-in session expired. Please try again."); return; }
    let active = true;
    api.post("/auth/oauth/exchange", { code }).then(({ data }) => {
      if (!active) return;
      localStorage.setItem("algosphere_token", data.token); localStorage.setItem("algosphere_username", data.username); localStorage.setItem("algosphere_role", data.role);
      window.dispatchEvent(new Event("verdixa-authenticated")); navigate(data.role === "ADMIN" ? "/admin" : "/dashboard", { replace: true });
    }).catch(() => active && setError("Your sign-in session expired. Please try again."));
    return () => { active = false; };
  }, [navigate, params]);
  return <AuthState error={error} loading />;
}

export function AuthState({ error, loading = false }) {
  return <main className="vx-auth vx-oauth-state"><section className="vx-auth-manifest"><div className="vx-auth-brand"><BrandLogo/><span>VERDIXA / 01</span></div><div><p className="vx-eyebrow">Secure workspace access</p><h1>One moment.</h1><p>We are finishing your secure sign-in.</p></div></section><section className="vx-auth-form"><div>{error ? <><span className="vx-section-meta">Sign-in unavailable</span><h2>We couldn’t sign you in</h2><p className="error-message" role="alert">{error}</p><div className="vx-oauth-actions"><a className="login-button" href="/login">Back to login</a></div></> : <><LoaderCircle className="vx-oauth-loader" size={28}/><span className="vx-section-meta">Secure sign-in</span><h2>Signing you into Verdixa…</h2><p>{loading && "Please keep this window open."}</p></>}</div></section></main>;
}
