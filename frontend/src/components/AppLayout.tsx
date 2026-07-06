import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

// Shell for authenticated pages: top bar with navigation and a sign-out action.
export default function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div className="app-shell">
      <header className="app-header">
        <Link to="/applications" className="brand">
          Adiaphora
        </Link>
        <nav className="app-nav">
          <NavLink to="/applications">Cases</NavLink>
        </nav>
        <div className="app-user">
          {user && <span className="muted">{user.email}</span>}
          <button type="button" onClick={handleLogout}>
            Sign out
          </button>
        </div>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}
