import {useCallback,useEffect,useMemo,useRef,useState} from "react";
import {Activity,Award,CheckCircle2,Code2,Flame,RefreshCw,Signal,Target,TrendingUp} from "lucide-react";
import api from "../services/api";
import UserShell from "../components/UserShell";
import WorkspacePageHeader from "../components/WorkspacePageHeader";
import ActivityHeatmap from "../components/ActivityHeatmap";
import {ErrorState,LoadingState} from "../components/PageState";

const dateLabel=value=>value?new Date(`${value}T00:00:00`).toLocaleDateString(undefined,{month:"short",day:"numeric"}):"—";
const percent=(value,total)=>total?Math.round((value||0)/total*100):0;

function Metric({label,value,suffix="",icon:Icon}){
  return <article className="vx-briefing-metric"><span>{Icon&&<Icon size={15}/>} {label}</span><strong>{value??0}<small>{suffix}</small></strong></article>;
}

function SubmissionGraph({days,range}){
  const [hover,setHover]=useState(null);
  const points=useMemo(()=>{
    const length=range==="7D"?7:range==="30D"?30:range==="90D"?90:365;
    const visible=days.slice(-length);
    if(range!=="1Y")return visible.map(day=>({date:day.date,value:day.submissions||0}));
    return Array.from({length:Math.ceil(visible.length/7)},(_,index)=>{const slice=visible.slice(index*7,index*7+7);return {date:slice[0]?.date,value:slice.reduce((total,day)=>total+(day.submissions||0),0)};});
  },[days,range]);
  const max=Math.max(...points.map(point=>point.value),1);
  const coords=points.map((point,index)=>({x:4+(index/Math.max(points.length-1,1))*92,y:88-(point.value/max)*72,...point}));
  const polyline=coords.map(point=>`${point.x},${point.y}`).join(" ");
  const move=event=>{const bounds=event.currentTarget.getBoundingClientRect();const target=((event.clientX-bounds.left)/bounds.width)*100;setHover(coords.reduce((closest,point)=>Math.abs(point.x-target)<Math.abs(closest.x-target)?point:closest,coords[0]));};
  return <div className="vx-submission-graph" role="img" aria-label={`Submission activity for ${range}`}><svg viewBox="0 0 100 100" preserveAspectRatio="none" onMouseMove={move} onMouseLeave={()=>setHover(null)}><path className="vx-graph-grid" d="M4 16H96M4 40H96M4 64H96M4 88H96"/><polygon points={`4,88 ${polyline} 96,88`}/><polyline points={polyline}/>{coords.map(point=><circle key={`${point.date}-${point.x}`} cx={point.x} cy={point.y} r="1.15" className={hover===point?"active":""}/>)}</svg>{hover&&<div className="vx-graph-tooltip"><strong>{hover.value}</strong> submission{hover.value===1?"":"s"}<span>{dateLabel(hover.date)}</span></div>}<footer><span>{dateLabel(points[0]?.date)}</span><span>{range==="1Y"?"Weekly totals":dateLabel(points.at(-1)?.date)}</span></footer></div>;
}

function Distribution({label,value,total,tone}){
  const ratio=percent(value,total);
  return <div className={`vx-distribution-row vx-distribution-row--${tone}`}><div><span>{label}</span><strong>{value||0}</strong></div><div className="vx-distribution-track"><i style={{width:`${ratio}%`}}/></div><small>{ratio}%</small></div>;
}

export default function Analytics(){
  const [data,setData]=useState(null),[error,setError]=useState(""),[updated,setUpdated]=useState(null),[busy,setBusy]=useState(false),[range,setRange]=useState("1Y");
  const active=useRef(true),running=useRef(false);
  const load=useCallback(async()=>{if(running.current)return;running.current=true;setBusy(true);try{const response=await api.get("/analytics/me");setData(response.data);setUpdated(new Date());setError("");}catch{setError("Analytics could not be loaded.");}finally{running.current=false;setBusy(false);}},[]);
  useEffect(()=>{load();const visibility=()=>{active.current=!document.hidden;if(active.current)load();};document.addEventListener("visibilitychange",visibility);const timer=setInterval(()=>active.current&&load(),15000);return()=>{clearInterval(timer);document.removeEventListener("visibilitychange",visibility);};},[load]);
  const days=useMemo(()=>data?.heatmap?.days||[],[data]);
  const insight=useMemo(()=>{const activeDays=days.filter(day=>day.submissions>0);const weeks=Array.from({length:Math.ceil(days.length/7)},(_,index)=>{const slice=days.slice(index*7,index*7+7);return {date:slice[0]?.date,total:slice.reduce((sum,day)=>sum+(day.submissions||0),0)};});const busiest=weeks.reduce((best,week)=>week.total>best.total?week:best,weeks[0]||{total:0});const languages=Object.entries(data?.languageUsage||{}).sort(([,a],[,b])=>b-a);return {activeDays,busiest,languages,primaryLanguage:languages[0]};},[data,days]);
  const accepted=data?.acceptedSubmissions||0;
  const acceptance=Number(data?.acceptanceRate)||0;
  return <UserShell context="Analytics" headerless>
    <WorkspacePageHeader eyebrow="Your performance" title="Analytics" description="A focused read on your work, momentum, and the skills you are building in Verdixa." actions={<div className="vx-analytics-header-actions"><span><Signal size={14}/>Live data</span><small>{updated?`Updated ${updated.toLocaleTimeString()}`:"Refreshing"}</small><button className="secondary-button" onClick={load} disabled={busy}><RefreshCw size={15}/>Refresh</button></div>}/>
    {error&&<ErrorState message={error} onRetry={load}/>}{!data&&!error&&<LoadingState label="Building your performance briefing"/>}
    {data&&<main className="vx-analytics vx-performance-briefing">
      <section className="vx-briefing-hero"><div className="vx-briefing-hero-copy"><span>Performance briefing</span><h2>Progress, made <em>legible.</em></h2><p>Every saved attempt contributes to the signal below. Keep the cadence; the system will show the shape of your work.</p><div className="vx-briefing-live"><i/><b>{insight.activeDays.length}</b> active days in the past year</div></div><div className="vx-briefing-score" style={{"--score":`${Math.max(0,Math.min(100,acceptance))}%`}} aria-label={`${acceptance}% acceptance rate`}><div><Target size={19}/><strong>{acceptance}<small>%</small></strong><span>Acceptance rate</span></div></div><div className="vx-briefing-highlights"><div><Flame size={16}/><span>Current run</span><strong>{data.heatmap?.currentStreak||0}<small> days</small></strong></div><div><Code2 size={16}/><span>Primary language</span><strong>{insight.primaryLanguage?.[0]||"—"}</strong><small>{insight.primaryLanguage?`${insight.primaryLanguage[1]} submissions`:"Start a first submission"}</small></div></div></section>
      <section className="vx-briefing-metrics"><Metric label="Problems solved" value={data.totalSolved} icon={CheckCircle2}/><Metric label="Total submissions" value={data.totalSubmissions} icon={Activity}/><Metric label="Longest streak" value={data.heatmap?.longestStreak} suffix=" days" icon={Flame}/><Metric label="Certificates" value={data.certificatesEarned} icon={Award}/></section>
      <section className="vx-briefing-grid"><article className="vx-briefing-panel vx-activity-panel"><header><div><span>Consistency map</span><h2>A year of practice</h2><p>Hover any day to inspect the activity behind it.</p></div><span className="vx-panel-index">01</span></header><ActivityHeatmap days={days}/></article><aside className="vx-briefing-panel vx-signal-panel"><header><div><span>Activity signals</span><h2>What stands out</h2></div><TrendingUp size={18}/></header><dl><div><dt>Busiest week</dt><dd>{insight.busiest.total}<small> submissions · {dateLabel(insight.busiest.date)}</small></dd></div><div><dt>Active days</dt><dd>{insight.activeDays.length}<small> of the last 365</small></dd></div><div><dt>Accepted verdicts</dt><dd>{accepted}<small> saved successes</small></dd></div></dl></aside></section>
      <section className="vx-briefing-panel vx-momentum-panel"><header><div><span>Practice velocity</span><h2>Momentum over time</h2><p>Choose a window to look at the rhythm of your saved submissions.</p></div><nav className="vx-range-control" aria-label="Submission activity range">{["7D","30D","90D","1Y"].map(item=><button type="button" key={item} className={range===item?"active":""} onClick={()=>setRange(item)}>{item}</button>)}</nav></header><SubmissionGraph days={days} range={range}/></section>
      <section className="vx-briefing-grid vx-briefing-grid--profile"><article className="vx-briefing-panel vx-outcome-panel"><header><div><span>Submission outcomes</span><h2>Quality of attempts</h2></div><span className="vx-panel-index">02</span></header><div className="vx-outcome-feature"><strong>{accepted}</strong><div><span>Accepted submissions</span><p>{data.failedSubmissions||0} attempts are still awaiting a better solution.</p></div></div><div className="vx-outcome-bar"><i style={{width:`${percent(accepted,data.totalSubmissions)}%`}}/></div><footer><span>{percent(accepted,data.totalSubmissions)}% accepted</span><span>{data.failedSubmissions||0} not accepted</span></footer></article><article className="vx-briefing-panel vx-profile-panel"><header><div><span>Practice profile</span><h2>Where your effort lands</h2></div><span className="vx-panel-index">03</span></header><div className="vx-profile-columns"><div><h3>Difficulty</h3>{[["Easy",data.easySolved,"easy"],["Medium",data.mediumSolved,"medium"],["Hard",data.hardSolved,"hard"]].map(([label,value,tone])=><Distribution key={label} label={label} value={value} total={data.totalSolved} tone={tone}/>)}</div><div><h3>Languages</h3>{insight.languages.length?insight.languages.slice(0,3).map(([label,value])=><Distribution key={label} label={label} value={value} total={data.totalSubmissions} tone="language"/>):<p className="vx-empty-profile">No submissions yet.</p>}</div></div></article></section>
    </main>}
  </UserShell>;
}
