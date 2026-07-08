import { Navigate, Route, Routes } from 'react-router-dom';
import AuthLayout from './components/AuthLayout';
import AppLayout from './components/AppLayout';
import ProtectedRoute from './components/ProtectedRoute';
import RoleRoute from './components/RoleRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ApplicationsPage from './pages/ApplicationsPage';
import QuestionnairePage from './pages/QuestionnairePage';
import EstatePage from './pages/EstatePage';
import ReviewsPage from './pages/ReviewsPage';
import NotFoundPage from './pages/NotFoundPage';

// Route map for the skeleton. Public auth routes use AuthLayout; everything else is gated by
// ProtectedRoute and rendered inside AppLayout.
export default function App() {
  return (
    <Routes>
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<Navigate to="/applications" replace />} />
          <Route path="/applications" element={<ApplicationsPage />} />
          <Route
            path="/applications/:applicationId/questionnaire"
            element={<QuestionnairePage />}
          />
          <Route path="/applications/:applicationId/estate" element={<EstatePage />} />
          <Route element={<RoleRoute roles={['OPERATOR', 'LAWYER', 'ADMIN', 'AUDITOR']} />}>
            <Route path="/reviews" element={<ReviewsPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
