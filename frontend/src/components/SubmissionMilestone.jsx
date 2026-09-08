import { useState } from "react";
import { Link } from "react-router-dom";
export default function SubmissionMilestone({ progress }) {
  const [dismissed,setDismissed]=useState(false);
  if(!progress)return null;
  const next=progress.milestones.find(t=>!t.earned);
  return <aside className="submission-milestone" aria-live="polite"><p>{progress.solvedCount} unique problems solved{next?` · Next: ${next.target} Problem Certificate · ${next.remaining} problems remaining`:" · All milestones earned"}</p>
    {!dismissed&&progress.newlyIssued?.map(c=><div key={c.publicCertificateId}><strong>Milestone achieved — {c.milestone} problems solved.</strong><p>Your Verdixa certificate is ready.</p><Link to={`/certificate/${c.publicCertificateId}`}>View Certificate</Link> <button onClick={()=>setDismissed(true)}>Continue Solving</button></div>)}
  </aside>;
}
