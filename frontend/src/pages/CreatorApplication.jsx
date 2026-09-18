import {useEffect,useRef,useState} from "react";
import {FileLock2,MailCheck,ShieldCheck,Upload} from "lucide-react";
import api from "../services/api";
import UserShell from "../components/UserShell";
import WorkspacePageHeader from "../components/WorkspacePageHeader";

const MAX_DOCUMENT_BYTES=8*1024*1024;
const DOCUMENT_TYPES=new Set(["image/jpeg","image/png","application/pdf"]);
const VERIFICATION_PROOFS=["Certificate of incorporation","Business licence or trade certificate","Tax registration certificate (redacted)","Official organization letter","Accreditation or affiliation certificate","Passport","Aadhaar (redacted)","Other organization proof"];
const fields=[["fullName","Full name"],["organization","Organization / institution"],["roleTitle","Role / designation"],["purpose","Purpose"],["website","Organization website (optional)"],["organizationEmail","Organization email (optional)"],["country","Country"]];

function requestMessage(error,fallback){
  const data=error.response?.data;
  return typeof data==="string"?data:data?.message||data?.error||fallback;
}

export default function CreatorApplication(){
  const [status,setStatus]=useState(null);
  const [form,setForm]=useState({fullName:"",organization:"",roleTitle:"",purpose:"",website:"",organizationEmail:"",intendedUse:"",country:"",documentType:VERIFICATION_PROOFS[0]});
  const [document,setDocument]=useState(null);
  const [digits,setDigits]=useState(["","","","","",""]);
  const [error,setError]=useState("");
  const [message,setMessage]=useState("");
  const [submitting,setSubmitting]=useState(false);
  const [verifying,setVerifying]=useState(false);
  const otpRefs=useRef([]);

  const load=async()=>{
    try{const response=await api.get("/assessment-creator/me/status");setStatus(response.data);if(["REJECTED","REVOKED"].includes(response.data.status)){setForm(current=>({...current,fullName:response.data.fullName||current.fullName,organization:response.data.organization||current.organization,roleTitle:response.data.role||current.roleTitle,purpose:response.data.purpose||current.purpose,website:response.data.website||current.website,organizationEmail:response.data.organizationEmail||current.organizationEmail,intendedUse:response.data.intendedUse||current.intendedUse,country:response.data.country||current.country,documentType:VERIFICATION_PROOFS.includes(response.data.documentType)?response.data.documentType:current.documentType}))}}
    catch(ex){setError(requestMessage(ex,"Creator application status could not be loaded."))}
  };

  useEffect(()=>{load()},[]);
  const change=event=>setForm(current=>({...current,[event.target.name]:event.target.value}));

  const chooseDocument=event=>{
    const selected=event.target.files?.[0]||null;
    setError("");
    if(!selected){setDocument(null);return}
    if(!DOCUMENT_TYPES.has(selected.type)){
      setDocument(null);event.target.value="";setError("Choose a JPEG, PNG, or PDF document.");return;
    }
    if(selected.size>MAX_DOCUMENT_BYTES){
      setDocument(null);event.target.value="";setError("The verification document must be 8 MB or smaller.");return;
    }
    setDocument(selected);
  };

  const apply=async event=>{
    event.preventDefault();setError("");setMessage("");
    if(!document){setError("Attach a verification document before sending the code.");return}
    setSubmitting(true);
    const body=new FormData();
    Object.entries(form).forEach(([key,value])=>body.append(key,value));
    body.append("document",document);
    try{
      // Leave Content-Type unset so the browser supplies the multipart boundary.
      const response=await api.post("/assessment-creator/applications",body);
      setStatus(response.data);setMessage("A one-time code was sent to your verified email.");
    }catch(ex){setError(requestMessage(ex,"Application could not be submitted."))}
    finally{setSubmitting(false)}
  };

  const verify=async()=>{
    setError("");setMessage("");
    const otp=digits.join("");
    if(otp.length!==6){setError("Enter the six-digit verification code.");return}
    setVerifying(true);
    try{
      const response=await api.post(`/assessment-creator/applications/${status.id}/verify-otp`,{code:otp});
      setStatus(response.data);setMessage("Your application is pending administrator review.");
    }catch(ex){setError(requestMessage(ex,"Code verification failed."))}
    finally{setVerifying(false)}
  };

  const setDigit=(index,value)=>{const digit=value.replace(/\D/g,"").slice(-1);setDigits(current=>current.map((item,itemIndex)=>itemIndex===index?digit:item));if(digit&&index<5)otpRefs.current[index+1]?.focus()};
  const pasteOtp=event=>{const values=event.clipboardData.getData("text").replace(/\D/g,"").slice(0,6).split("");if(values.length){event.preventDefault();setDigits([...values,...Array(6-values.length).fill("")]);otpRefs.current[Math.min(values.length,5)]?.focus()}};
  const canReapply=["REJECTED","REVOKED"].includes(status?.status);
  return <UserShell context="Creator verification" headerless>
    <WorkspacePageHeader eyebrow="Trusted hosting" title="Assessment Creator Verification" description="A private, measured review for organizations that host coding evaluations."/>
    {error&&<div className="form-error" role="alert">{error}</div>}
    {message&&<div className="form-success" role="status">{message}</div>}
    {status?.status==="EMAIL_OTP_PENDING"?<section className="vx-access-gate vx-creator-state">
      <MailCheck/><span>Step 2 of 3</span><h2>Confirm your email</h2>
      <p>Enter the single-use code sent to your registered address before the private review begins.</p>
      <div className="vx-otp-inputs" onPaste={pasteOtp}>{digits.map((digit,index)=><input key={index} ref={element=>otpRefs.current[index]=element} value={digit} inputMode="numeric" autoComplete={index===0?"one-time-code":"off"} aria-label={`Verification digit ${index+1}`} onChange={event=>setDigit(index,event.target.value)} onKeyDown={event=>{if(event.key==="Backspace"&&!digits[index]&&index)otpRefs.current[index-1]?.focus()}}/> )}</div>
      <button className="primary-button" type="button" onClick={verify} disabled={verifying||digits.join("").length!==6}>{verifying?"Verifying…":"Verify and submit"}</button>
    </section>:status&&status.status!=="NONE"&&!canReapply?<section className="vx-access-gate vx-creator-state">
      <ShieldCheck/><span>Application status</span><h2>{status.status.replaceAll("_"," ")}</h2>
      <p>{status.decisionReason||"We will show updates here when an administrator reviews your application."}</p>
    </section>:<form className="vx-creator-workspace" onSubmit={apply}>
      <aside className="vx-creator-rail"><div><span>Application flow</span><ol><li className="active"><b>01</b><div><strong>Identity details</strong><small>Tell us who is hosting.</small></div></li><li><b>02</b><div><strong>Email verification</strong><small>Confirm your verified address.</small></div></li><li><b>03</b><div><strong>Admin review</strong><small>We review protected evidence.</small></div></li></ol></div><div className="vx-creator-security"><FileLock2 size={19}/><strong>Private by default</strong><p>Only authorized administrators can access encrypted supporting documents.</p></div></aside>
      <div className="vx-creator-form-stack">{canReapply&&<div className="form-success" role="status"><strong>Submit a revised application</strong><p>Your earlier application was {status.status.toLowerCase()}. Review the administrator’s feedback, update the information or evidence below, and submit a new application.</p>{status.decisionReason&&<p><b>Feedback:</b> {status.decisionReason}</p>}</div>}
        <section><header><span>01 / Identity</span><h2>Personal details</h2><p>Use the name and role connected to your assessment work.</p></header><div className="vx-form-grid">{fields.slice(0,1).map(([name,label])=><label key={name}>{label}<input name={name} value={form[name]} onChange={change} required/></label>)}{fields.slice(2,3).map(([name,label])=><label key={name}>{label}<input name={name} value={form[name]} onChange={change} required/></label>)}</div></section>
        <section><header><span>02 / Organization</span><h2>Hosting context</h2><p>These details help us verify that the workspace has a legitimate owner.</p></header><div className="vx-form-grid">{fields.slice(1,2).concat(fields.slice(3)).map(([name,label])=><label key={name}>{label}<input name={name} value={form[name]} onChange={change} required={!name.includes("website")&&!name.includes("Email")}/></label>)}</div></section>
        <section><header><span>03 / Protected evidence</span><h2>Verification proof</h2><p>Upload redacted organization or business evidence, or your Passport or Aadhaar when needed to verify the accountable applicant. Student and employee IDs are not accepted.</p></header><div className="vx-form-grid"><label>Verification proof type<select name="documentType" value={form.documentType} onChange={change}>{VERIFICATION_PROOFS.map(item=><option key={item}>{item}</option>)}</select></label><label className="wide">Intended assessment use<textarea name="intendedUse" value={form.intendedUse} onChange={change} rows="5" required/></label><label className="wide vx-upload-zone"><input type="file" accept="image/jpeg,image/png,application/pdf" onChange={chooseDocument} required/><Upload size={21}/><strong>{document?document.name:"Drop a verification proof document here"}</strong><small>JPEG, PNG, or PDF · maximum 8 MB · encrypted at rest</small></label></div></section>
        <footer>{error&&<p className="vx-form-action-error" role="alert">{error}</p>}<p>A one-time code will be sent to your verified email address next.</p><button className="vx-primary-action" type="submit" disabled={!document||submitting}>{submitting?"Sending verification code…":"Send verification code"}</button></footer>
      </div>
    </form>}
  </UserShell>;
}
