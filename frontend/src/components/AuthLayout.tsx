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
        <p className="muted">Подготовка документов для банкротства физических лиц</p>
        <Outlet />
        <nav className="auth-switch">
          {onLogin ? (
            <span>
              Нет аккаунта? <Link to="/register">Зарегистрироваться</Link>
            </span>
          ) : (
            <span>
              Уже есть аккаунт? <Link to="/login">Войти</Link>
            </span>
          )}
        </nav>
      </div>
    </div>
  );
}
