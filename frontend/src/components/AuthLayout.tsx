import { Link, Outlet, useLocation } from 'react-router-dom';

// Public home owns its full-width layout. Login and registration keep the compact auth card.
export default function AuthLayout() {
  const { pathname } = useLocation();
  const onHome = pathname === '/';
  const onLogin = pathname === '/login';

  if (onHome) {
    return <Outlet />;
  }

  return (
    <div className="auth-shell">
      <main className="auth-card">
        <h1 className="brand">
          <Link to="/">Adiaphora</Link>
        </h1>
        <p className="muted">Подготовка документов для банкротства физических лиц</p>
        <Outlet />
        <p className="auth-switch">
          {onLogin ? (
            <>
              Нет аккаунта? <Link to="/register">Зарегистрироваться</Link>
            </>
          ) : (
            <>
              Уже есть аккаунт? <Link to="/login">Войти</Link>
            </>
          )}
        </p>
      </main>
    </div>
  );
}
