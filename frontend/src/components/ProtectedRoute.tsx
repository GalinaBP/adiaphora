import { Navigate, Outlet } from 'react-router-dom';
import { tokenStore } from '../api/client';
import { useAuth } from '../auth/AuthContext';

// Gate for authenticated routes. Redirects to /login when there is no session.
export default function ProtectedRoute() {
  const { initializing } = useAuth();

  if (initializing) {
    return <div className="page-loading">Loading…</div>;
  }
  if (!tokenStore.get()) {
    return <Navigate to="/login" replace />;
  }
  return <Outlet />;
}
