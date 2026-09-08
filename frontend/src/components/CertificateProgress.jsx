import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Award, BadgeCheck, Download, Eye, LockKeyhole, X } from "lucide-react";
import api from "../services/api";

export const certificatePdfUrl = id => `${api.defaults?.baseURL || import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api"}/certificates/${encodeURIComponent(id)}/pdf`;

const tierDetails = {
  50: { name: "Foundation", label: "First signal" },
  100: { name: "Distinction", label: "Steady craft" },
  150: { name: "Excellence", label: "Deep work" },
};

function CertificatePreview({ tier, recipientName, onClose }) {
  const detail = tierDetails[tier.target] || { name: "Achievement", label: "Milestone" };

  useEffect(() => {
    const closeOnEscape = event => event.key === "Escape" && onClose();
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [onClose]);

  return <div className="certificate-preview-backdrop" role="presentation" onMouseDown={onClose}>
    <section className="certificate-preview-modal" role="dialog" aria-modal="true" aria-labelledby="certificate-preview-title" onMouseDown={event => event.stopPropagation()}>
      <header className="certificate-preview-header">
        <div><span className="vx-section-meta">Certificate preview</span><h2 id="certificate-preview-title">{tier.target}-problem milestone</h2></div>
        <button className="certificate-close" onClick={onClose} aria-label="Close certificate preview"><X size={18}/></button>
      </header>
      <article className={`certificate-preview-document tier-${tier.target}`}>
        <div className="certificate-preview-corner certificate-preview-corner--top" />
        <div className="certificate-preview-mark"><Award size={25}/></div>
        <p className="certificate-preview-brand">Verdixa</p>
        <span className="certificate-preview-label">{detail.label}</span>
        <h3>Certificate of Achievement</h3>
        <p className="certificate-preview-presented">This is presented to</p>
        <strong className="certificate-preview-recipient">{recipientName}</strong>
        <div className="certificate-preview-divider" />
        <p className="certificate-preview-milestone">{detail.name} · {tier.target} problems</p>
        <p className="certificate-preview-copy">For successfully solving {tier.target} unique programming problems through verified Verdixa submissions.</p>
        <footer><span>Verdixa learning platform</span><span>{tier.earned ? `Issued ${new Date(tier.issuedAt).toLocaleDateString(undefined, { dateStyle: "medium" })}` : "Preview · not yet issued"}</span></footer>
        <div className="certificate-preview-corner certificate-preview-corner--bottom" />
      </article>
      <div className="certificate-preview-actions">
        <p>{tier.earned ? <><BadgeCheck size={15}/> This certificate has been earned and verified.</> : <><LockKeyhole size={15}/> {tier.remaining} more {tier.remaining === 1 ? "problem" : "problems"} to unlock this certificate.</>}</p>
        {tier.earned && <a className="certificate-download" href={certificatePdfUrl(tier.certificateId)}><Download size={15}/> Download PDF</a>}
      </div>
    </section>
  </div>;
}

export default function CertificateProgress({ userId, compact = false }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [previewTier, setPreviewTier] = useState(null);
  const recipientName = localStorage.getItem("algosphere_username") || "Verdixa Developer";

  useEffect(() => {
    let active = true;
    setData(null); setError("");
    api.get(userId ? `/admin/users/${userId}/certificate-progress` : "/users/me/certificate-progress")
      .then(response => { if (active && Array.isArray(response.data?.milestones)) setData(response.data); })
      .catch(() => { if (active) setError("Unable to load certificate progress."); });
    return () => { active = false; };
  }, [userId, reload]);

  return <section className={`certificate-progress ${compact ? "compact" : ""}`} aria-label="Certificate milestones">
    <header className="certificate-progress-header">
      <div className="certificate-progress-title"><span className="certificate-title-icon"><Award size={19}/></span><div><span className="vx-section-meta">Verdixa recognition</span><h2>Certificate milestones</h2></div></div>
      {compact && <Link className="certificate-all-link" to="/profile/certificates">View all <span>→</span></Link>}
    </header>
    {error ? <p role="alert">{error} <button onClick={() => setReload(value => value + 1)}>Retry</button></p> : !data ? <p className="certificate-loading">Loading certificate progress…</p> : <>
      <div className="certificate-progress-summary"><strong>{data.solvedCount}</strong><span>unique problems solved</span><i /><span>{data.nextMilestone ? `${data.nextMilestone - data.solvedCount} to your next certificate` : "All milestones earned"}</span></div>
      <div className="certificate-tiers">{data.milestones.map(tier => {
        const detail = tierDetails[tier.target] || { name: "Achievement" };
        const percentage = Math.min(100, Math.round((tier.current / tier.target) * 100));
        return <article className={`certificate-tier tier-${tier.target} ${tier.earned ? "is-earned" : ""}`} key={tier.target}>
          <div className="certificate-tier-top"><span className="certificate-tier-number">{String(tier.target).padStart(3, "0")}</span><span className="certificate-tier-status">{tier.earned ? <><BadgeCheck size={13}/> Earned</> : <><LockKeyhole size={12}/> Locked</>}</span></div>
          <h3>{detail.name}</h3><p className="certificate-tier-target">{tier.target} problem certificate</p>
          <div className="certificate-progress-line" role="progressbar" aria-label={`${tier.target} problem milestone`} aria-valuemin="0" aria-valuemax={tier.target} aria-valuenow={tier.current}><i style={{ width: `${percentage}%` }} /></div>
          <div className="certificate-tier-count"><strong>{tier.current}</strong><span>/ {tier.target} solved</span><b>{percentage}%</b></div>
          <p className="certificate-tier-description">{tier.earned ? "Official Verdixa credential issued." : `${tier.remaining} problems remain to unlock.`}</p>
          <div className="certificate-actions"><button className="certificate-preview-button" onClick={() => setPreviewTier(tier)}><Eye size={15}/> Preview</button>{tier.earned && <Link className="certificate-view-link" to={`/certificate/${tier.certificateId}`}>Open certificate <span>→</span></Link>}</div>
        </article>;
      })}</div>
    </>}
    {previewTier && <CertificatePreview tier={previewTier} recipientName={recipientName} onClose={() => setPreviewTier(null)} />}
  </section>;
}
