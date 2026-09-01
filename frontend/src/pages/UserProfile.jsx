import { useEffect, useState } from "react";
import { BarChart3, CheckCircle2, Flame, Send, UserRound } from "lucide-react";
import api from "../services/api";
import UserNavigation from "../components/UserNavigation";

function UserProfile() {
  const [progress, setProgress] = useState(null);
  const [analytics, setAnalytics] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([api.get("/users/me/progress"), api.get("/analytics/me")])
      .then(([response, analyticsResponse]) => { setProgress(response.data); setAnalytics(analyticsResponse.data); })
      .catch(() => setError("Unable to load your profile and progress."));
  }, []);

  return (
    <div className="user-page">
      <header className="dashboard-navbar">
        <div className="dashboard-brand"><div className="dashboard-brand-icon"><BarChart3 size={22} /></div><div><h2>Verdixa</h2><span>Master Algorithms. Build Logic.</span></div></div>
        <UserNavigation active="profile" />
      </header>
      <main className="user-page-container">
        <div className="page-heading"><div><p className="section-eyebrow">PROFILE</p><h1>Your Progress</h1><p>See the progress you have earned from accepted submissions.</p></div></div>
        {error && <div className="dashboard-error">{error}</div>}
        {!progress && !error && <div className="empty-state"><div className="loading-spinner" /><p>Loading progress...</p></div>}
        {progress && <>
          <section className="profile-hero">
            <div className="profile-avatar"><UserRound size={34} /></div>
            <div><h2>{progress.username}</h2><p>{progress.email}</p><span className="profile-role">{progress.role}</span></div>
            <div className="completion-ring"><strong>{progress.completionPercentage}%</strong><span>complete</span></div>
          </section>
          <section className="profile-stats-grid">
            <div className="stat-card"><div className="stat-icon"><CheckCircle2 size={22} /></div><div><span>Solved Problems</span><strong>{progress.solvedProblems} / {progress.totalProblems}</strong></div></div>
            <div className="stat-card"><div className="stat-icon"><Send size={22} /></div><div><span>Submissions</span><strong>{progress.submissionCount}</strong></div></div>
            <div className="stat-card"><div className="stat-icon"><BarChart3 size={22} /></div><div><span>Acceptance Rate</span><strong>{progress.acceptanceRate}%</strong></div></div>
            <div className="stat-card"><div className="stat-icon"><Flame size={22} /></div><div><span>Current Streak</span><strong>{progress.currentStreak} days</strong></div></div>
          </section>
          <section className="progress-breakdown"><div><h2>Difficulty breakdown</h2><p>Problems solved at each difficulty.</p></div><div className="difficulty-progress"><span className="easy">Easy <strong>{progress.easySolved}</strong></span><span className="medium">Medium <strong>{progress.mediumSolved}</strong></span><span className="hard">Hard <strong>{progress.hardSolved}</strong></span></div></section>
          {analytics && <section className="progress-breakdown"><h2>365-day activity</h2><p>{analytics.heatmap.activeDays} active days · {analytics.heatmap.currentStreak} day current streak · {analytics.heatmap.longestStreak} day longest streak</p><div className="heatmap" aria-label="365 day activity heatmap">{analytics.heatmap.days.map(day => <span key={day.date} title={`${day.date}: ${day.submissions} submissions`} style={{opacity: day.submissions ? Math.min(1, 0.25 + day.submissions / 5) : 0.1}} />)}</div><h3>Language usage</h3><p>{Object.entries(analytics.languageUsage || {}).map(([language,count]) => `${language}: ${count}`).join(" · ") || "No persisted submissions yet."}</p></section>}
        </>}
      </main>
    </div>
  );
}

export default UserProfile;
