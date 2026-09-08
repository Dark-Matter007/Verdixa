import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ShieldCheck } from "lucide-react";
import axios from "axios";
import { certificatePdfUrl } from "../components/CertificateProgress";
export default function CertificateVerification(){
  const {publicId}=useParams();const [data,setData]=useState(null);const [error,setError]=useState("");
  useEffect(()=>{let active=true;setData(null);setError("");axios.get(`${import.meta.env.VITE_API_BASE_URL||"http://localhost:8080/api"}/certificates/${encodeURIComponent(publicId)}`).then(r=>{if(active)setData(r.data);}).catch(e=>{if(active)setError(e.response?.status===404?"Certificate not found.":"Verification is unavailable. Please retry later.");});return()=>{active=false;};},[publicId]);
  return <main className="certificate-page"><Link to="/">Verdixa</Link>{error?<p role="alert">{error}</p>:!data?<p>Verifying certificate…</p>:<><p className="certificate-verified"><ShieldCheck size={18}/> {data.status}</p><article className={`certificate-document tier-${data.milestone}`}><p className="certificate-brand">VERDIXA</p><h1>Certificate of Achievement</h1><p>Presented to</p><h2>{data.recipientName}</h2><h3>{data.milestone} Problem Milestone</h3><p className="certificate-tier-name">{({50:"Foundation",100:"Distinction",150:"Excellence"})[data.milestone]}</p><p>Successfully solved {data.milestone} unique programming problems on Verdixa.</p><p>Issued: {new Date(data.issuedAt).toLocaleDateString(undefined,{dateStyle:"long"})}</p><footer><p>Certificate ID: {data.publicCertificateId}</p><p>Verification code: {data.verificationCode}</p><p>Verified against official Verdixa submission records · Version {data.certificateVersion}</p></footer></article><a className="primary-button" href={certificatePdfUrl(data.publicCertificateId)}>Download PDF</a></>}</main>;
}
