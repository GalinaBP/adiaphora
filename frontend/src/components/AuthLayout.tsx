import { Link, Outlet, useLocation } from 'react-router-dom';

// Centered card layout for the unauthenticated pages (login / register).
export default function AuthLayout() {
  const { pathname } = useLocation();
  const onLogin = pathname === '/login';

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <h1 className="brand">Adiaphora</h1>
        <p className="muted">Personal bankruptcy document platform</p>
        <Outlet />
        <nav className="auth-switch">
          {onLogin ? (
            <span>
              No account? <Link to="/register">Register</Link>
            </span>
          ) : (
            <span>
              Have an account? <Link to="/login">Sign in</Link>
            </span>
          )}
        </nav>
      </div>
    </div>
  );
}
