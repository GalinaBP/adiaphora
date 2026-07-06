import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="page-loading">
      <h2>Page not found</h2>
      <p className="muted">
        <Link to="/applications">Back to your cases</Link>
      </p>
    </div>
  );
}
