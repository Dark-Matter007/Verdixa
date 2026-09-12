import { useEffect, useState } from "react";
import { BadgeCheck, Eye, EyeOff, KeyRound, Link2, Loader2, Mail, ShieldCheck, UserRound } from "lucide-react";
import { useNavigate } from "react-router-dom";
import AdminShell from "../components/AdminShell";
import UserShell from "../components/UserShell";
import api from "../services/api";
import { ErrorState, LoadingState } from "../components/PageState";

const messageOf = (error, fallback) => error.response?.data?.message || (error.request ? "Verdixa could not reach the server. Try again." : fallback);

function useCooldown() {
  const [seconds, setSeconds] = useState(0);
  useEffect(() => {
    if (!seconds) return undefined;
    const timer = window.setInterval(() => setSeconds((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearInterval(timer);
  }, [seconds]);
  return [seconds, setSeconds];
}

function InlineStatus({ error, notice }) {
  if (error) return <div className="vx-settings-alert is-error" role="alert">{error}</div>;
  if (notice) return <div className="vx-settings-alert is-success" role="status">{notice}</div>;
  return null;
}

function PasswordInput({ id, label, value, onChange, autoComplete }) {
  const [visible, setVisible] = useState(false);
  return <label className="vx-settings-field" htmlFor={id}><span>{label}</span><div className="vx-password-input"><input id={id} type={visible ? "text" : "password"} value={value} onChange={onChange} autoComplete={autoComplete} required minLength={6} maxLength={100}/><button type="button" onClick={() => setVisible((state) => !state)} aria-label={`${visible ? "Hide" : "Show"} ${label.toLowerCase()}`}>{visible ? <EyeOff size={16}/> : <Eye size={16}/>}</button></div></label>;
}

function OtpStep({ id, label, otp, setOtp, loading, cooldown, onVerify, onResend }) {
  return <form className="vx-security-flow" onSubmit={onVerify}>
    <label className="vx-settings-field" htmlFor={id}><span>{label}</span><input id={id} className="vx-otp-input" value={otp} onChange={(event) => setOtp(event.target.value.replace(/\D/g, ""))} inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} placeholder="000000" required/></label>
    <div className="vx-flow-actions"><button className="primary-button" disabled={loading || otp.length !== 6}>{loading ? <><Loader2 className="spin" size={16}/>Verifying…</> : "Verify code"}</button><button className="vx-text-button" type="button" disabled={loading || cooldown > 0} onClick={onResend}>{cooldown ? `Resend in ${cooldown}s` : "Resend code"}</button></div>
  </form>;
}

function UsernameChangePanel({ profile, onSensitiveComplete }) {
  const [open, setOpen] = useState(false), [stage, setStage] = useState("REQUEST"), [username, setUsername] = useState(""), [otp, setOtp] = useState(""), [loading, setLoading] = useState(false), [error, setError] = useState(""), [notice, setNotice] = useState("");
  const [cooldown, setCooldown] = useCooldown();
  const request = async (event, resend = false) => { event?.preventDefault(); setLoading(true); setError(""); try { const { data } = await api.post(`/profile/username/change/${resend ? "resend" : "request"}`, { username }); setStage("VERIFY"); setNotice(data.message); setCooldown(data.cooldownSeconds || 60); } catch (reason) { setError(messageOf(reason, "Unable to request a username change.")); } finally { setLoading(false); } };
  const verify = async (event) => { event.preventDefault(); setLoading(true); setError(""); try { const { data } = await api.post("/profile/username/change/verify", { otp }); onSensitiveComplete(data.message); } catch (reason) { setError(messageOf(reason, "Unable to verify the code.")); } finally { setLoading(false); } };
  return <SecurityCard icon={UserRound} title="Username" subtitle="Your public Verdixa identity." value={`@${profile.username}`} actionLabel={open ? "Close" : "Change username"} onAction={() => { setOpen((value) => !value); setError(""); setNotice(""); }}>
    {open && <div className="vx-panel-body"><InlineStatus error={error} notice={notice}/>{stage === "REQUEST" ? <form className="vx-security-flow" onSubmit={request}><label className="vx-settings-field" htmlFor="new-username"><span>New username</span><input id="new-username" value={username} onChange={(event) => setUsername(event.target.value)} minLength={3} maxLength={100} pattern="[A-Za-z0-9_][A-Za-z0-9_.-]*" placeholder="new_username" required/><small>3–100 characters. Letters, numbers, dots, underscores, and hyphens.</small></label><button className="primary-button" disabled={loading}>{loading ? "Sending code…" : "Continue securely"}</button></form> : <OtpStep id="username-otp" label="Verification code" otp={otp} setOtp={setOtp} loading={loading} cooldown={cooldown} onVerify={verify} onResend={() => request(null, true)}/>}</div>}
  </SecurityCard>;
}

function EmailChangePanel({ profile, onSensitiveComplete }) {
  const [open, setOpen] = useState(false), [stage, setStage] = useState("REQUEST"), [newEmail, setNewEmail] = useState(""), [password, setPassword] = useState(""), [otp, setOtp] = useState(""), [loading, setLoading] = useState(false), [error, setError] = useState(""), [notice, setNotice] = useState("");
  const [cooldown, setCooldown] = useCooldown();
  const request = async (event, resend = false) => { event?.preventDefault(); setLoading(true); setError(""); try { const { data } = await api.post(`/profile/email/change/${resend ? "resend-current" : "request"}`, { newEmail, currentPassword: profile.hasLocalPassword ? password : null }); setStage("CURRENT"); setOtp(""); setNotice(data.message); setCooldown(data.cooldownSeconds || 60); } catch (reason) { setError(messageOf(reason, "Unable to start the email change.")); } finally { setLoading(false); } };
  const verifyCurrent = async (event) => { event.preventDefault(); setLoading(true); setError(""); try { const { data } = await api.post("/profile/email/change/verify-current", { otp }); setStage("NEW"); setOtp(""); setNotice(data.message); setCooldown(data.cooldownSeconds || 60); } catch (reason) { setError(messageOf(reason, "Unable to confirm your current email.")); } finally { setLoading(false); } };
  const verifyNew = async (event) => { event.preventDefault(); setLoading(true); setError(""); try { const { data } = await api.post("/profile/email/change/verify-new", { otp }); onSensitiveComplete(data.message); } catch (reason) { setError(messageOf(reason, "Unable to verify your new email.")); } finally { setLoading(false); } };
  const resendNew = async () => { setLoading(true); setError(""); try { const { data } = await api.post("/profile/email/change/resend-new"); setNotice(data.message); setCooldown(data.cooldownSeconds || 60); } catch (reason) { setError(messageOf(reason, "Unable to resend the code.")); } finally { setLoading(false); } };
  return <SecurityCard icon={Mail} title="Email address" subtitle="Your verified sign-in and security address." value={profile.email} badge="Verified" actionLabel={open ? "Close" : "Change email"} onAction={() => { setOpen((value) => !value); setError(""); setNotice(""); }}>
    {open && <div className="vx-panel-body"><div className="vx-step-track" aria-label="Email change progress"><span className="active">1. New address</span><span className={stage !== "REQUEST" ? "active" : ""}>2. Current email</span><span className={stage === "NEW" ? "active" : ""}>3. New email</span></div><InlineStatus error={error} notice={notice}/>
      {stage === "REQUEST" && <form className="vx-security-flow" onSubmit={request}><label className="vx-settings-field" htmlFor="new-email"><span>New email address</span><input id="new-email" type="email" value={newEmail} onChange={(event) => setNewEmail(event.target.value)} autoComplete="email" maxLength={150} required/></label>{profile.hasLocalPassword && <PasswordInput id="email-current-password" label="Current password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password"/>}<button className="primary-button" disabled={loading}>{loading ? "Confirming…" : "Confirm account"}</button></form>}
      {stage === "CURRENT" && <OtpStep id="current-email-otp" label="Code sent to your current email" otp={otp} setOtp={setOtp} loading={loading} cooldown={cooldown} onVerify={verifyCurrent} onResend={() => request(null, true)}/>} 
      {stage === "NEW" && <OtpStep id="new-email-otp" label={`Code sent to ${newEmail}`} otp={otp} setOtp={setOtp} loading={loading} cooldown={cooldown} onVerify={verifyNew} onResend={resendNew}/>} 
    </div>}
  </SecurityCard>;
}

function PasswordPanel({ profile, onSensitiveComplete }) {
  const blank = { currentPassword: "", newPassword: "", confirmPassword: "" };
  const [form, setForm] = useState(blank), [stage, setStage] = useState(profile.hasLocalPassword ? "CHANGE" : "INTRO"), [otp, setOtp] = useState(""), [loading, setLoading] = useState(false), [error, setError] = useState(""), [notice, setNotice] = useState("");
  const [cooldown, setCooldown] = useCooldown();
  const field = (name) => (event) => setForm((value) => ({ ...value, [name]: event.target.value }));
  const change = async (event) => { event.preventDefault(); setError(""); if (form.newPassword !== form.confirmPassword) return setError("Passwords do not match."); setLoading(true); try { const { data } = await api.post("/profile/password/change", form); onSensitiveComplete(data.message); } catch (reason) { setError(messageOf(reason, "Unable to update your password.")); } finally { setLoading(false); } };
  const requestSetup = async () => { setLoading(true); setError(""); try { const { data } = await api.post("/profile/password/setup/request"); setStage("VERIFY"); setNotice(data.message); setCooldown(data.cooldownSeconds || 60); } catch (reason) { setError(messageOf(reason, "Unable to request a password setup code.")); } finally { setLoading(false); } };
  const verifySetup = async (event) => { event.preventDefault(); setLoading(true); setError(""); try { const { data } = await api.post("/profile/password/setup/verify", { otp }); setStage("SET"); setNotice(data.message); } catch (reason) { setError(messageOf(reason, "Unable to verify the code.")); } finally { setLoading(false); } };
  const setup = async (event) => { event.preventDefault(); setError(""); if (form.newPassword !== form.confirmPassword) return setError("Passwords do not match."); setLoading(true); try { const { data } = await api.post("/profile/password/setup", { newPassword: form.newPassword, confirmPassword: form.confirmPassword }); onSensitiveComplete(data.message); } catch (reason) { setError(messageOf(reason, "Unable to create your password.")); } finally { setLoading(false); } };
  const resendSetup = async () => { setLoading(true); setError(""); try { const { data } = await api.post("/profile/password/setup/resend"); setNotice(data.message); setCooldown(data.cooldownSeconds || 60); } catch (reason) { setError(messageOf(reason, "Unable to resend the code.")); } finally { setLoading(false); } };
  return <SecurityCard icon={KeyRound} title="Password & security" subtitle={profile.hasLocalPassword ? "Change your local credential securely." : "Add an optional Verdixa password without disconnecting OAuth."} value={profile.hasLocalPassword ? "Local password enabled" : "OAuth sign-in only"} alwaysOpen>
    <div className="vx-panel-body"><InlineStatus error={error} notice={notice}/>
      {stage === "CHANGE" && <form className="vx-security-flow vx-password-grid" onSubmit={change}><PasswordInput id="current-password" label="Current password" value={form.currentPassword} onChange={field("currentPassword")} autoComplete="current-password"/><PasswordInput id="new-password" label="New password" value={form.newPassword} onChange={field("newPassword")} autoComplete="new-password"/><PasswordInput id="confirm-password" label="Confirm new password" value={form.confirmPassword} onChange={field("confirmPassword")} autoComplete="new-password"/><button className="primary-button" disabled={loading}>{loading ? "Updating…" : "Change password"}</button></form>}
      {stage === "INTRO" && <div className="vx-security-flow"><p>Your Google or GitHub connection remains active. Email verification is required before adding a local password.</p><button className="primary-button" type="button" disabled={loading} onClick={requestSetup}>{loading ? "Sending…" : "Set a Verdixa password"}</button></div>}
      {stage === "VERIFY" && <OtpStep id="password-setup-otp" label="Verification code" otp={otp} setOtp={setOtp} loading={loading} cooldown={cooldown} onVerify={verifySetup} onResend={resendSetup}/>} 
      {stage === "SET" && <form className="vx-security-flow" onSubmit={setup}><PasswordInput id="setup-password" label="New password" value={form.newPassword} onChange={field("newPassword")} autoComplete="new-password"/><PasswordInput id="setup-confirm" label="Confirm new password" value={form.confirmPassword} onChange={field("confirmPassword")} autoComplete="new-password"/><button className="primary-button" disabled={loading}>{loading ? "Creating…" : "Create password"}</button></form>}
    </div>
  </SecurityCard>;
}

function SecurityCard({ icon: Icon, title, subtitle, value, badge, actionLabel, onAction, alwaysOpen, children }) {
  return <section className={`vx-settings-card ${alwaysOpen ? "is-expanded" : ""}`}><header><div className="vx-settings-icon"><Icon size={18}/></div><div><h2>{title}</h2><p>{subtitle}</p></div>{value && <div className="vx-settings-value"><strong>{value}</strong>{badge && <span><BadgeCheck size={13}/>{badge}</span>}</div>}{actionLabel && <button className="secondary-button" type="button" onClick={onAction}>{actionLabel}</button>}</header>{children}</section>;
}

export default function ProfileSettings() {
  const navigate = useNavigate();
  const role = localStorage.getItem("algosphere_role");
  const [profile, setProfile] = useState(null), [error, setError] = useState(""), [completion, setCompletion] = useState("");
  const load = () => { setError(""); api.get("/profile").then(({ data }) => setProfile(data)).catch((reason) => setError(messageOf(reason, "Profile settings could not be loaded."))); };
  useEffect(load, []);
  const signInAgain = () => { ["algosphere_token", "algosphere_username", "algosphere_role"].forEach((key) => localStorage.removeItem(key)); window.dispatchEvent(new Event("verdixa-signed-out")); navigate("/login", { replace: true, state: { verified: completion } }); };
  const content = <div className="vx-settings-page">
    {completion && <div className="vx-session-notice" role="status"><ShieldCheck size={20}/><div><strong>{completion}</strong><span>Your previous session is no longer valid.</span></div><button className="primary-button" onClick={signInAgain}>Sign in again</button></div>}
    {error && <ErrorState message={error} onRetry={load}/>} {!profile && !error && <LoadingState label="Loading secure profile settings"/>}
    {profile && !completion && <><section className="vx-profile-summary"><div className="vx-profile-monogram">{profile.username.slice(0, 2).toUpperCase()}</div><div><span className="vx-section-meta">Profile information</span><h2>{profile.username}</h2><p>{profile.email}</p></div><span className="vx-role-badge"><ShieldCheck size={14}/>{profile.role}</span></section><div className="vx-settings-stack"><UsernameChangePanel profile={profile} onSensitiveComplete={setCompletion}/><EmailChangePanel profile={profile} onSensitiveComplete={setCompletion}/><PasswordPanel profile={profile} onSensitiveComplete={setCompletion}/><SecurityCard icon={Link2} title="Connected accounts" subtitle="Provider identities remain stable when your Verdixa email changes." value={profile.connectedProviders.length ? profile.connectedProviders.join(" · ") : "No connected providers"} alwaysOpen><div className="vx-panel-body vx-connected-note"><BadgeCheck size={17}/><p>{profile.connectedProviders.length ? "Connected sign-in methods remain available. Verdixa never stores provider access or refresh tokens." : "This account currently uses a local Verdixa password."}</p></div></SecurityCard></div></>}
  </div>;
  return role === "ADMIN" ? <AdminShell eyebrow="Account security" title="Edit profile" description="Manage your own administrator identity and credentials. Admin privileges never bypass verification.">{content}</AdminShell> : <UserShell context="Profile settings"><header className="vx-settings-intro"><p className="vx-eyebrow">Account / Security</p><h1>Edit profile</h1><p>Manage your Verdixa identity through verified, single-use security flows.</p></header>{content}</UserShell>;
}
