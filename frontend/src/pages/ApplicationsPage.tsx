import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { applicationsApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import type { ApplicationResponse } from '../api/types';
import { routeRu, statusRu } from '../i18n/labels';

// Displays the list of cases and lets the user open one or start a new one.
// Presentation only — status/route are shown verbatim as the backend returns them.
export default function ApplicationsPage() {
  const navigate = useNavigate();
  const [applications, setApplications] = useState<ApplicationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    let active = true;
    applicationsApi
      .list()
      .then((page) => {
        if (active) setApplications(page.items);
      })
      .catch((err) => {
        if (active) setError(err instanceof ApiError ? err.message : 'Не удалось загрузить дела');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const createCase = async () => {
    setCreating(true);
    setError(null);
    try {
      const created = await applicationsApi.create();
      navigate(`/applications/${created.applicationId}/questionnaire`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Не удалось создать дело');
      setCreating(false);
    }
  };

  return (
    <section>
      <div className="page-head">
        <h2>Ваши дела</h2>
        <button type="button" onClick={createCase} disabled={creating}>
          {creating ? 'Создаём…' : 'Новое дело'}
        </button>
      </div>

      {error && <p className="error" role="alert">{error}</p>}
      {loading ? (
        <p className="muted">Загрузка дел…</p>
      ) : applications.length === 0 ? (
        <p className="muted">Дел пока нет. Создайте новое, чтобы начать анкету.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Дело</th>
              <th>Статус</th>
              <th>Маршрут</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {applications.map((app) => (
              <tr key={app.applicationId}>
                <td className="mono">{app.applicationId.slice(0, 8)}</td>
                <td>{statusRu(app.status)}</td>
                <td>{routeRu(app.route)}</td>
                <td>
                  <button
                    type="button"
                    onClick={() => navigate(`/applications/${app.applicationId}/questionnaire`)}
                  >
                    Открыть
                  </button>{' '}
                  <button
                    type="button"
                    onClick={() => navigate(`/applications/${app.applicationId}/estate`)}
                  >
                    Имущество
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
