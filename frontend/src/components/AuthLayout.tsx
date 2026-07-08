import { Link, Outlet, useLocation } from 'react-router-dom';

// Centered card layout for the public pages (home / login / register). The home page gets a wider
// card to fit the eligibility estimator and its context sections.
export default function AuthLayout() {
  const { pathname } = useLocation();
  const onLogin = pathname === '/login';
  const onHome = pathname === '/';

  return (
    <div className="auth-shell">
      <div className={onHome ? 'auth-card auth-card-wide' : 'auth-card'}>
        <h1 className="brand">
          <Link to="/">Adiaphora</Link>
        </h1>
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
