import { BrowserRouter, Routes, Route, Navigate, Link, useLocation } from "react-router-dom";
import LanguageSelector from "./components/LanguageSelector";
import ThemeToggle from "./components/ThemeToggle";

import Login from "./pages/Login";
import Register from "./pages/Register";
import VerifyEmail from "./pages/VerifyEmail";
import ForgotPassword from "./pages/ForgotPassword";
import OAuthCallback from "./pages/OAuthCallback";
import OAuthError from "./pages/OAuthError";
import TestCaseManagement from "./pages/TestCaseManagement";
import Dashboard from "./pages/Dashboard";
import AdminDashboard from "./pages/AdminDashboard";
import ProblemList from "./pages/ProblemList";
import ProblemForm from "./pages/ProblemForm";
import UserManagement from "./pages/UserManagement";
import ProblemSolver from "./pages/ProblemSolver";
import SubmissionHistory from "./pages/SubmissionHistory";
import SubmissionDetail from "./pages/SubmissionDetail";
import PersonalLists from "./pages/PersonalLists";
import PersonalListDetail from "./pages/PersonalListDetail";
import Collections from "./pages/Collections";
import UserProfile from "./pages/UserProfile";
import Leaderboard from "./pages/Leaderboard";
import LearningPaths from "./pages/LearningPaths";
import DailyChallenge from "./pages/DailyChallenge";
import Contests from "./pages/Contests";
import ContestDetail from "./pages/ContestDetail";
import ContestLaunch from "./pages/ContestLaunch";
import AdminContests from "./pages/AdminContests";
import AdminCollections from "./pages/AdminCollections";
import AdminUserDetail from "./pages/AdminUserDetail";
import AdminProblemAnalytics from "./pages/AdminProblemAnalytics";
import AdminLearningPaths from "./pages/AdminLearningPaths";
import AdminDailyChallenges from "./pages/AdminDailyChallenges";
import ProtectedRoute from "./components/ProtectedRoute";
import Landing from "./pages/Landing";
import ProfileSettings from "./pages/ProfileSettings";
import Analytics from "./pages/Analytics";
import Assessments from "./pages/Assessments";
import AssessmentDetail from "./pages/AssessmentDetail";
import AssessmentStudio from "./pages/AssessmentStudio";
import AssessmentBuilder from "./pages/AssessmentBuilder";
import AssessmentAnalytics from "./pages/AssessmentAnalytics";
import AssessmentParticipants from "./pages/AssessmentParticipants";
import AssessmentTake from "./pages/AssessmentTake";
import CreatorApplication from "./pages/CreatorApplication";
import AdminCreatorApplications from "./pages/AdminCreatorApplications";
import AssessmentAccess from "./pages/AssessmentAccess";

import Certificates from "./pages/Certificates";
import CertificateVerification from "./pages/CertificateVerification";

function GuestUtilities() {
  const { pathname } = useLocation();
  const assessmentFocusRoute = /^\/assessment\/\d+\/(access|preflight)$/.test(pathname) || /^\/assessments\/\d+\/take$/.test(pathname);
  if (localStorage.getItem("algosphere_token") || pathname === "/" || assessmentFocusRoute) return null;
  return <div className="vx-global-utilities"><LanguageSelector /><ThemeToggle /><Link to="/login">Sign in</Link></div>;
}

function App() {
  return (
    <BrowserRouter>

      <GuestUtilities />

      <Routes>

        <Route path="/" element={<Landing />} />

        <Route
          path="/login"
          element={<Login />}
        />
        <Route path="/certificate/:publicId" element={<CertificateVerification />} />
        <Route path="/profile/certificates" element={<ProtectedRoute allowedRoles={["USER"]}><Certificates /></ProtectedRoute>} />
        <Route path="/register" element={<Register />} />
        <Route path="/verify-email" element={<VerifyEmail />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/oauth/callback" element={<OAuthCallback />} />
        <Route path="/oauth/error" element={<OAuthError />} />
        <Route path="/assessment/:id/access" element={<AssessmentAccess />} />
        <Route path="/assessment/:id/preflight" element={<AssessmentTake />} />

        {/* USER */}

        <Route
          path="/dashboard"
          element={<ProtectedRoute allowedRoles={["USER"]}><Dashboard /></ProtectedRoute>}
        />
        <Route path="/problems" element={<ProtectedRoute allowedRoles={["USER"]}><Dashboard /></ProtectedRoute>} />

        <Route
          path="/problems/:id"
          element={<ProtectedRoute allowedRoles={["USER"]}><ProblemSolver /></ProtectedRoute>}
        />

        <Route
          path="/history"
          element={<ProtectedRoute allowedRoles={["USER"]}><SubmissionHistory /></ProtectedRoute>}
        />
        <Route path="/submissions/:id" element={<ProtectedRoute allowedRoles={["USER"]}><SubmissionDetail /></ProtectedRoute>} />
        <Route path="/lists" element={<ProtectedRoute allowedRoles={["USER"]}><PersonalLists /></ProtectedRoute>} />
        <Route path="/lists/:id" element={<ProtectedRoute allowedRoles={["USER"]}><PersonalListDetail /></ProtectedRoute>} />
        <Route path="/collections" element={<ProtectedRoute allowedRoles={["USER"]}><Collections /></ProtectedRoute>} />
        <Route path="/learning-paths" element={<ProtectedRoute allowedRoles={["USER"]}><LearningPaths /></ProtectedRoute>} />
        <Route path="/paths" element={<ProtectedRoute allowedRoles={["USER"]}><LearningPaths /></ProtectedRoute>} />
        <Route path="/daily-challenge" element={<ProtectedRoute allowedRoles={["USER"]}><DailyChallenge /></ProtectedRoute>} />
        <Route path="/daily" element={<ProtectedRoute allowedRoles={["USER"]}><DailyChallenge /></ProtectedRoute>} />
        <Route path="/contests" element={<ProtectedRoute allowedRoles={["USER"]}><Contests /></ProtectedRoute>} />
        <Route path="/contests/:id" element={<ProtectedRoute allowedRoles={["USER"]}><ContestDetail /></ProtectedRoute>} />
        <Route path="/contests/:id/solve" element={<ProtectedRoute allowedRoles={["USER"]}><ContestLaunch /></ProtectedRoute>} />
        <Route path="/analytics" element={<ProtectedRoute allowedRoles={["USER"]}><Analytics /></ProtectedRoute>} />
        <Route path="/assessments" element={<ProtectedRoute allowedRoles={["USER"]}><Assessments /></ProtectedRoute>} />
        <Route path="/assessments/:id" element={<ProtectedRoute allowedRoles={["USER"]}><AssessmentDetail /></ProtectedRoute>} />
        <Route path="/assessments/:id/take" element={<ProtectedRoute allowedRoles={["USER"]}><AssessmentTake /></ProtectedRoute>} />
        <Route path="/assessments/studio" element={<ProtectedRoute allowedRoles={["USER"]}><AssessmentStudio /></ProtectedRoute>} />
        <Route path="/assessments/studio/create" element={<ProtectedRoute allowedRoles={["USER"]}><AssessmentBuilder /></ProtectedRoute>} />
        <Route path="/assessments/studio/:id/edit" element={<ProtectedRoute allowedRoles={["USER"]}><AssessmentBuilder /></ProtectedRoute>} />
        <Route path="/assessments/studio/:id/analytics" element={<ProtectedRoute allowedRoles={["USER"]}><AssessmentAnalytics /></ProtectedRoute>} />
        <Route path="/assessments/studio/:id/participants" element={<ProtectedRoute allowedRoles={["USER"]}><AssessmentParticipants /></ProtectedRoute>} />
        <Route path="/assessment-creator/apply" element={<ProtectedRoute allowedRoles={["USER"]}><CreatorApplication /></ProtectedRoute>} />

        <Route
          path="/profile"
          element={<ProtectedRoute allowedRoles={["USER"]}><UserProfile /></ProtectedRoute>}
        />
        <Route path="/progress" element={<ProtectedRoute allowedRoles={["USER"]}><UserProfile /></ProtectedRoute>} />
        <Route path="/profile/edit" element={<ProtectedRoute allowedRoles={["USER"]}><ProfileSettings /></ProtectedRoute>} />

        <Route
          path="/leaderboard"
          element={<ProtectedRoute allowedRoles={["USER"]}><Leaderboard /></ProtectedRoute>}
        />

        {/* ADMIN */}

        <Route
          path="/admin"
          element={<ProtectedRoute allowedRoles={["ADMIN"]}><AdminDashboard /></ProtectedRoute>}
        />

        <Route
          path="/admin/problems"
          element={<ProtectedRoute allowedRoles={["ADMIN"]}><ProblemList /></ProtectedRoute>}
        />

        <Route
          path="/admin/problems/new"
          element={<ProtectedRoute allowedRoles={["ADMIN"]}><ProblemForm /></ProtectedRoute>}
        />

        <Route
          path="/admin/problems/edit/:id"
          element={<ProtectedRoute allowedRoles={["ADMIN"]}><ProblemForm /></ProtectedRoute>}
        />
        <Route
          path="/admin/problems/:id/testcases"
          element={<ProtectedRoute allowedRoles={["ADMIN"]}><TestCaseManagement /></ProtectedRoute>}
        />

        <Route
          path="/admin/users"
          element={<ProtectedRoute allowedRoles={["ADMIN"]}><UserManagement /></ProtectedRoute>}
        />
        <Route path="/admin/collections" element={<ProtectedRoute allowedRoles={["ADMIN"]}><AdminCollections /></ProtectedRoute>} />
        <Route path="/admin/learning-paths" element={<ProtectedRoute allowedRoles={["ADMIN"]}><AdminLearningPaths /></ProtectedRoute>} />
        <Route path="/admin/daily-challenges" element={<ProtectedRoute allowedRoles={["ADMIN"]}><AdminDailyChallenges /></ProtectedRoute>} />
        <Route path="/admin/contests" element={<ProtectedRoute allowedRoles={["ADMIN"]}><AdminContests /></ProtectedRoute>} />
        <Route path="/admin/users/:id" element={<ProtectedRoute allowedRoles={["ADMIN"]}><AdminUserDetail /></ProtectedRoute>} />
        <Route path="/admin/problems/:id/analytics" element={<ProtectedRoute allowedRoles={["ADMIN"]}><AdminProblemAnalytics /></ProtectedRoute>} />
        <Route path="/admin/profile/edit" element={<ProtectedRoute allowedRoles={["ADMIN"]}><ProfileSettings /></ProtectedRoute>} />
        <Route path="/admin/assessment-creators" element={<ProtectedRoute allowedRoles={["ADMIN"]}><AdminCreatorApplications /></ProtectedRoute>} />

        <Route
          path="*"
          element={<Navigate to="/login" replace />}
        />

      </Routes>

    </BrowserRouter>
  );
}

export default App;
