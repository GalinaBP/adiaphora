import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { applicationsApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import type { ApplicationResponse } from '../api/types';

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
        if (active) setError(err instanceof ApiError ? err.message : 'Failed to load cases');
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
      setError(err instanceof ApiError ? err.message : 'Failed to create case');
      setCreating(false);
    }
  };

  return (
    <section>
      <div className="page-head">
        <h2>Your cases</h2>
        <button type="button" onClick={createCase} disabled={creating}>
          {creating ? 'Creating…' : 'New case'}
        </button>
      </div>

      {error && <p className="error" role="alert">{error}</p>}
      {loading ? (
        <p className="muted">Loading cases…</p>
      ) : applications.length === 0 ? (
        <p className="muted">No cases yet. Start one to begin the questionnaire.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Case</th>
              <th>Status</th>
              <th>Route</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {applications.map((app) => (
              <tr key={app.applicationId}>
                <td className="mono">{app.applicationId.slice(0, 8)}</td>
                <td>{app.status}</td>
                <td>{app.route}</td>
                <td>
                  <button
                    type="button"
                    onClick={() => navigate(`/applications/${app.applicationId}/questionnaire`)}
                  >
                    Open
                  </button>{' '}
                  <button
                    type="button"
                    onClick={() => navigate(`/applications/${app.applicationId}/estate`)}
                  >
                    Estate
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
