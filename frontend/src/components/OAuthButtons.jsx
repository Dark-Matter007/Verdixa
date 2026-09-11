function GoogleIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true"><path fill="#4285F4" d="M21.35 12.23c0-.71-.06-1.4-.18-2.05H12v3.88h5.24a4.48 4.48 0 0 1-1.94 2.94v2.52h3.15c1.84-1.7 2.9-4.2 2.9-7.29Z"/><path fill="#34A853" d="M12 21.75c2.62 0 4.81-.87 6.41-2.23L15.26 17A5.77 5.77 0 0 1 6.67 13.97H3.42v2.6A9.68 9.68 0 0 0 12 21.75Z"/><path fill="#FBBC05" d="M6.67 13.97A5.83 5.83 0 0 1 6.35 12c0-.68.12-1.34.32-1.97v-2.6H3.42A9.75 9.75 0 0 0 2.25 12c0 1.57.38 3.06 1.17 4.57l3.25-2.6Z"/><path fill="#EA4335" d="M12 6.25c1.5 0 2.85.52 3.91 1.53l2.93-2.93C16.81 2.95 14.62 2.25 12 2.25a9.68 9.68 0 0 0-8.58 5.18l3.25 2.6A5.77 5.77 0 0 1 12 6.25Z"/></svg>;
}

function GitHubIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M12 2.25a9.75 9.75 0 0 0-3.08 19c.49.09.67-.21.67-.47v-1.86c-2.73.59-3.3-1.16-3.3-1.16-.45-1.13-1.09-1.43-1.09-1.43-.89-.61.07-.6.07-.6 1 .07 1.51 1.02 1.51 1.02.87 1.49 2.29 1.06 2.85.81.09-.63.34-1.06.62-1.3-2.18-.25-4.47-1.09-4.47-4.86 0-1.08.39-1.96 1.02-2.65-.1-.25-.44-1.26.1-2.63 0 0 .83-.27 2.69 1.01a9.29 9.29 0 0 1 4.9 0c1.86-1.28 2.69-1.01 2.69-1.01.54 1.37.2 2.38.1 2.63.63.69 1.01 1.57 1.01 2.65 0 3.78-2.3 4.6-4.49 4.85.35.3.67.88.67 1.77v2.63c0 .26.18.57.68.47A9.75 9.75 0 0 0 12 2.25Z"/></svg>;
}

export default function OAuthButtons() {
  const begin = (provider) => {
    const apiBase = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
    window.location.assign(`${apiBase.replace(/\/api\/?$/, "")}/oauth2/authorization/${provider}`);
  };
  return <div className="vx-oauth" aria-label="Social sign-in options">
    <div className="vx-oauth-divider"><span>or continue with</span></div>
    <button type="button" className="vx-oauth-button" onClick={() => begin("google")} aria-label="Continue with Google"><GoogleIcon/><span>Continue with Google</span></button>
    <button type="button" className="vx-oauth-button" onClick={() => begin("github")} aria-label="Continue with GitHub"><GitHubIcon/><span>Continue with GitHub</span></button>
  </div>;
}
