import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import type { UserRole } from '../api/types';

// Gate for role-restricted routes (used inside ProtectedRoute, so a session already exists).
// Users without one of the allowed roles are sent back to their cases; the backend enforces the
// same rules, this only keeps staff screens out of ordinary users' navigation.
export default function RoleRoute({ roles }: { roles: UserRole[] }) {
  const { user, initializing } = useAuth();

  if (initializing) {
    return <div className="page-loading">Loading…</div>;
  }
  if (!user || !roles.includes(user.role)) {
    return <Navigate to="/applications" replace />;
  }
  return <Outlet />;
}
