import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import Login from "./pages/Login";
import Register from "./pages/Register";
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
import AdminContests from "./pages/AdminContests";
import AdminCollections from "./pages/AdminCollections";
import AdminUserDetail from "./pages/AdminUserDetail";
import AdminProblemAnalytics from "./pages/AdminProblemAnalytics";
import AdminLearningPaths from "./pages/AdminLearningPaths";
import AdminDailyChallenges from "./pages/AdminDailyChallenges";
import ProtectedRoute from "./components/ProtectedRoute";
import Landing from "./pages/Landing";

import Certificates from "./pages/Certificates";
import CertificateVerification from "./pages/CertificateVerification";

function App() {
  return (
    <BrowserRouter>

      <Routes>

        <Route path="/" element={<Landing />} />

        <Route
          path="/login"
          element={<Login />}
        />
        <Route path="/certificate/:publicId" element={<CertificateVerification />} />
        <Route path="/profile/certificates" element={<ProtectedRoute allowedRoles={["USER"]}><Certificates /></ProtectedRoute>} />
        <Route path="/register" element={<Register />} />

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

        <Route
          path="/profile"
          element={<ProtectedRoute allowedRoles={["USER"]}><UserProfile /></ProtectedRoute>}
        />
        <Route path="/progress" element={<ProtectedRoute allowedRoles={["USER"]}><UserProfile /></ProtectedRoute>} />

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

        <Route
          path="*"
          element={<Navigate to="/login" replace />}
        />

      </Routes>

    </BrowserRouter>
  );
}

export default App;
