import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="page-loading">
      <h2>Страница не найдена</h2>
      <p className="muted">
        <Link to="/applications">Вернуться к делам</Link>
      </p>
    </div>
  );
}
