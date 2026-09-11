import { useState } from "react";
import { ArrowRight, BookOpen, Braces, Check, ChevronRight, Code2, Crown, Gauge, GitBranch, LockKeyhole, Play, ShieldCheck, Sparkles, TerminalSquare, Trophy } from "lucide-react";
import { Link } from "react-router-dom";
import BrandLogo from "../components/BrandLogo";
import ThemeToggle from "../components/ThemeToggle";
import VerdixaAssistant from "../components/VerdixaAssistant";
import "./Landing.css";

const languageSamples = {
  Java: ["public int[] twoSum(int[] nums, int target) {", "  Map<Integer, Integer> seen = new HashMap<>();", "  for (int i = 0; i < nums.length; i++) {", "    int need = target - nums[i];", "    if (seen.containsKey(need)) return new int[] { seen.get(need), i };", "    seen.put(nums[i], i);", "  }", "  return new int[0];", "}"],
  "C++": ["vector<int> twoSum(vector<int>& nums, int target) {", "  unordered_map<int, int> seen;", "  for (int i = 0; i < nums.size(); ++i) {", "    int need = target - nums[i];", "    if (seen.count(need)) return {seen[need], i};", "    seen[nums[i]] = i;", "  }", "  return {};", "}"],
  Python: ["def two_sum(nums, target):", "    seen = {}", "    for index, value in enumerate(nums):", "        need = target - value", "        if need in seen:", "            return [seen[need], index]", "        seen[value] = index", "    return []"],
};

function SectionTitle({ eyebrow, title, children }) {
  return <header className="landing-section-title"><span>{eyebrow}</span><h2>{title}</h2>{children && <p>{children}</p>}</header>;
}

export default function Landing() {
  const [language, setLanguage] = useState("Java");
  return <main className="landing">
    <a className="landing-skip" href="#main-content">Skip to content</a>
    <header className="landing-nav">
      <Link className="landing-brand" to="/" aria-label="Verdixa home"><BrandLogo compact/><strong>Verdixa</strong><small>CODE SYSTEM</small></Link>
      <nav aria-label="Landing navigation"><a href="#platform">Platform</a><a href="#judge">Judge</a><a href="#contests">Contests</a><a href="#learn">Learn</a></nav>
      <div className="landing-nav-actions"><ThemeToggle /><Link to="/login">Sign in</Link><Link className="landing-nav-cta" to="/register">Start coding <ArrowRight size={14}/></Link></div>
    </header>

    <div id="main-content" className="landing-main">
      <section className="landing-hero" aria-labelledby="landing-title">
        <div className="landing-hero-copy">
          <p className="landing-kicker"><i/> A complete environment for deliberate practice</p>
          <h1 id="landing-title">Build sharper<br/><em>algorithmic</em> instincts.</h1>
          <p className="landing-lede">Verdixa combines a real multi-language judge, focused practice, competition, and guided learning into one serious coding environment.</p>
          <div className="landing-hero-actions"><Link className="landing-button landing-button--primary" to="/register">Start solving <ArrowRight size={17}/></Link><Link className="landing-button landing-button--quiet" to="/login">Enter workspace</Link></div>
          <div className="landing-hero-note"><span><ShieldCheck size={15}/> Secure JWT access</span><span><Code2 size={15}/> Java · C++ · Python</span></div>
        </div>
        <div className="landing-hero-art" aria-label="Verdixa coding workspace preview">
          <div className="landing-orbit landing-orbit--one"/><div className="landing-orbit landing-orbit--two"/>
          <div className="landing-workspace">
            <div className="landing-windowbar"><span className="landing-live-dot"/><span>VERDIXA / SOLVER</span><span>Java</span></div>
            <div className="landing-workspace-body">
              <aside><small>PROBLEM 014</small><h3>Pair Index</h3><div className="landing-difficulty">MEDIUM</div><p>Return the two indices whose values resolve the target.</p><div className="landing-mini-tabs"><b>Brief</b><span>Hints</span><span>Editorial</span></div><div className="landing-constraint">O(n) target<br/>Hidden validation enabled</div></aside>
              <div className="landing-code-preview"><div className="landing-codebar"><span><Braces size={14}/> solution.java</span><button aria-label="Run preview"><Play size={13}/> Run</button></div><pre>{languageSamples.Java.map((line, index) => <code key={line}><i>{index + 1}</i>{line}</code>)}</pre><div className="landing-verdict"><span><Check size={14}/> Accepted</span><small>all validation cases passed</small></div></div>
            </div>
          </div>
          <div className="landing-float-card landing-float-card--rank"><small>CONTEST POSITION</small><strong>#{" "}08</strong><span><Trophy size={14}/> standing updated</span></div>
          <div className="landing-float-card landing-float-card--judge"><small>EXECUTION</small><strong>42ms</strong><span><i/> hidden cases verified</span></div>
        </div>
      </section>

      <section className="landing-capability-strip" aria-label="Verdixa platform capabilities"><span>BUILT FOR THE FULL LOOP</span><div><b>Java</b><b>C++</b><b>Python</b><b>Monaco editor</b><b>Hidden tests</b><b>JWT + RBAC</b><b>Contests</b></div></section>

      <section id="platform" className="landing-platform landing-section">
        <SectionTitle eyebrow="01 / A considered platform" title={<>The work happens in<br/>one <em>connected</em> system.</>}>Not a loose collection of challenges. Each surface is designed to move practice forward.</SectionTitle>
        <div className="landing-platform-layout">
          <article className="landing-feature-primary"><div className="landing-feature-icon"><TerminalSquare size={22}/></div><span>THE CORE LOOP</span><h3>From first read to a trusted verdict.</h3><p>Inspect the specification, code in a real workspace, run custom input, and submit to a judge built to handle public and hidden validation.</p><div className="landing-process-mini"><b>Read</b><i/><b>Code</b><i/><b>Verify</b><i/><b>Improve</b></div></article>
          <div className="landing-feature-rail"><article><BookOpen size={19}/><div><span>Practice library</span><p>Filter by difficulty, topic, and completion without losing context.</p></div></article><article><Sparkles size={19}/><div><span>Progressive guidance</span><p>Hints and editorials arrive when they help, not before.</p></div></article><article><Gauge size={19}/><div><span>Meaningful progress</span><p>Track solving history, acceptance, difficulty, and active practice.</p></div></article></div>
        </div>
      </section>

      <section className="landing-flow landing-section"><SectionTitle eyebrow="02 / Execution, made visible" title={<>An honest workflow<br/>for better <em>thinking.</em></>}/><div className="landing-flow-rail">{[["01","Discover","A problem with a clear contract."],["02","Construct","Choose a language and build the solution."],["03","Challenge","Use custom input before committing."],["04","Judge","Run against declared and hidden tests."],["05","Refine","Use verdicts, hints, and editorials to improve."]].map(([number,title,copy])=><article key={number}><span>{number}</span><i/><h3>{title}</h3><p>{copy}</p></article>)}</div><div className="landing-execution-line"><span>source</span><i/><span>compiler / runtime</span><i/><span>visible + hidden cases</span><i/><strong>verdict</strong></div></section>

      <section id="judge" className="landing-judge landing-section">
        <div className="landing-judge-copy"><SectionTitle eyebrow="03 / Multi-language judge" title={<>One problem.<br/>Three native ways<br/>to <em>solve it.</em></>}>Switch languages without stepping outside the problem. Verdixa preserves the same contract and evaluates the submitted solution through its language-specific judge pipeline.</SectionTitle><ul><li><Check/> Language-aware execution</li><li><Check/> Hidden test validation</li><li><Check/> Clear compile and runtime verdicts</li></ul></div>
        <div className="landing-judge-console"><div className="landing-console-tabs" role="tablist" aria-label="Judge languages">{Object.keys(languageSamples).map((item)=><button key={item} onClick={()=>setLanguage(item)} className={language===item?"active":""} role="tab" aria-selected={language===item}>{item}</button>)}</div><div className="landing-console-head"><span><Code2 size={15}/> submission.{language === "C++" ? "cpp" : language === "Python" ? "py" : "java"}</span><span>READY</span></div><pre className="landing-console-code">{languageSamples[language].map((line,index)=><code key={`${line}-${index}`}><i>{index+1}</i>{line}</code>)}</pre><footer><span><i/> Judge ready</span><span>hidden test protection <LockKeyhole size={13}/></span></footer></div>
      </section>

      <section className="landing-workspace-showcase landing-section"><SectionTitle eyebrow="04 / The problem workspace" title={<>Designed for the moment<br/>the solution <em>clicks.</em></>}>The reading surface, code editor, test input, and result state stay in one disciplined frame.</SectionTitle><div className="landing-workspace-wide"><div className="landing-statement"><div><span>ARRAYS / MEDIUM</span><b>Signal Merge</b></div><nav><strong>Overview</strong><span>Hints</span><span>Editorial</span></nav><p>Combine two ordered streams while preserving their relative order.</p><section><small>CONSTRAINTS</small><p>Work within linear time. Avoid mutating the source streams.</p></section><section><small>PROGRESSIVE HINT</small><p>What must remain true at every step of a two-pointer traversal?</p></section></div><div className="landing-editor"><header><span><Braces size={16}/> workspace</span><div><button>Reset</button><button className="run">Run</button><button className="submit">Submit</button></div></header><pre><code><i>1</i><span>while</span> (left &lt; a.length &amp;&amp; right &lt; b.length) {'{'} </code><code><i>2</i>  result.push(a[left] &lt; b[right] ? a[left++] : b[right++]);</code><code><i>3</i>{'}'}</code></pre><div className="landing-custom-input"><span>CUSTOM INPUT</span><small>Use your own cases before the judge sees it.</small><button>+ Add case</button></div></div></div></section>

      <section id="contests" className="landing-contests landing-section"><div className="landing-contest-intro"><SectionTitle eyebrow="05 / Timed competition" title={<>Practice in private.<br/>Perform in <em>public.</em></>}>Contests turn the familiar solver into a focused arena: fixed windows, ordered problems, submission context, and standings.</SectionTitle><Link to="/login" className="landing-text-action">Explore contests <ChevronRight size={15}/></Link></div><div className="landing-arena"><div className="landing-arena-head"><span><i/> LIVE ARENA</span><time>01:42:19 remaining</time></div><h3>Verdixa Circuit</h3><p>Four problems. One decisive session.</p><div className="landing-problem-letters"><b className="solved">A<small>SOLVED</small></b><b className="attempted">B<small>ATTEMPTED</small></b><b>C<small>OPEN</small></b><b>D<small>OPEN</small></b></div><div className="landing-standings"><span>STANDINGS SNAPSHOT</span><div><b>01</b><strong>northstar</strong><em>3 solved</em></div><div className="current"><b>08</b><strong>you</strong><em>1 solved</em></div><div><b>09</b><strong>lambda</strong><em>1 solved</em></div></div></div></section>

      <section id="learn" className="landing-learn landing-section"><div className="landing-learn-article"><span>06 / Guided improvement</span><h2>When you need a nudge,<br/><em>not an answer.</em></h2><p>Progressive hints respect the attempt. Editorials turn a completed problem into a reusable mental model. Paths and daily practice give the work a cadence.</p><Link to="/register" className="landing-text-action">Build a practice habit <ChevronRight size={15}/></Link></div><div className="landing-guidance-stack"><article className="landing-hint-card"><span>HINT 02 / AVAILABLE AFTER ATTEMPTS</span><h3>Try stating the invariant before choosing the container.</h3><p>Every operation should leave the observed state valid for the next comparison.</p><div><LockKeyhole size={14}/> Revealed deliberately</div></article><article className="landing-editorial-card"><small>EDITORIAL / PUBLISHED</small><h3>Merge sequences through a two-pointer invariant.</h3><div><span>INTUITION</span><span>ALGORITHM</span><span>COMPLEXITY</span></div></article></div></section>

      <section className="landing-architecture landing-section"><SectionTitle eyebrow="07 / Built with intent" title={<>A developer platform<br/>with real <em>engineering</em> beneath it.</>}/><div className="landing-architecture-grid"><article><ShieldCheck/><h3>Secure by design</h3><p>JWT authentication and role-aware access keep the user and administration surfaces correctly scoped.</p></article><article><GitBranch/><h3>One coherent product</h3><p>Practice, contests, learning assets, submissions, and administration share a consistent data model.</p></article><article><Crown/><h3>Judgement over decoration</h3><p>A precise workspace and controlled feedback make the technical work the centre of attention.</p></article></div></section>

      <section className="landing-final"><div><span>VERDIXA / READY WHEN YOU ARE</span><h2>Turn each attempt<br/>into <em>better judgment.</em></h2></div><div><p>Begin with a problem. Leave with a system for solving the next one.</p><div><Link className="landing-button landing-button--primary" to="/register">Create your workspace <ArrowRight size={17}/></Link><Link className="landing-button landing-button--quiet" to="/login">Sign in</Link></div></div></section>
    </div>
    <footer className="landing-footer"><div className="landing-footer-brand"><BrandLogo compact/><strong>Verdixa</strong><p>Practice with precision. Compete with context.</p></div><div><span>PRODUCT</span><a href="#platform">Platform</a><a href="#judge">Judge</a><a href="#contests">Contests</a></div><div><span>EXPLORE</span><Link to="/login">Practice</Link><Link to="/login">Leaderboard</Link><Link to="/login">Sign in</Link></div><small>© {new Date().getFullYear()} Verdixa. Built for disciplined problem solving.</small></footer><VerdixaAssistant mode="public" />
  </main>;
}
