import { useEffect, useState } from 'react';
import { reviewsApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import type { BankruptcyRoute, ReviewResponse } from '../api/types';
import { reviewStatusRu, routeRu } from '../i18n/labels';

const ROUTES: BankruptcyRoute[] = [
  'MFC_PRELIMINARY',
  'COURT_PRELIMINARY',
  'INSUFFICIENT_INFORMATION',
  'NOT_CURRENTLY_RECOMMENDED',
];

// Staff screen for the manual-review workflow: list review tasks, assign them (admin), request
// information, and record decisions. Mirrors the backend's documentation rules client-side so the
// reviewer sees why a submit is rejected before the request is even made: a rejection always needs a
// reason, and approving with a changed route needs one too. The backend remains the authority — its
// 422s are shown verbatim in the error banner.
export default function ReviewsPage() {
  const { user } = useAuth();
  const [reviews, setReviews] = useState<ReviewResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [openId, setOpenId] = useState<string | null>(null);
  const [reason, setReason] = useState('');
  const [newRoute, setNewRoute] = useState<'' | BankruptcyRoute>('');
  const [assigneeId, setAssigneeId] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const role = user?.role;
  const canAssign = role === 'ADMIN';
  const canRequestInfo = role === 'OPERATOR' || role === 'LAWYER' || role === 'ADMIN';
  const canDecide = role === 'LAWYER' || role === 'ADMIN';
  const readOnly = !canAssign && !canRequestInfo && !canDecide;

  useEffect(() => {
    let active = true;
    reviewsApi
      .list()
      .then((page) => {
        if (active) setReviews(page.items);
      })
      .catch((err) => {
        if (active) setError(err instanceof ApiError ? err.message : 'Не удалось загрузить проверки');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const openReview = (reviewId: string) => {
    setOpenId(openId === reviewId ? null : reviewId);
    setReason('');
    setNewRoute('');
    setAssigneeId('');
    setFormError(null);
    setNotice(null);
  };

  const perform = async (action: () => Promise<ReviewResponse>, done: string) => {
    setBusy(true);
    setError(null);
    setFormError(null);
    try {
      const updated = await action();
      setReviews((current) =>
        current.map((r) => (r.reviewId === updated.reviewId ? updated : r)),
      );
      setReason('');
      setNewRoute('');
      setNotice(done);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Действие не выполнено. Попробуйте ещё раз.');
    } finally {
      setBusy(false);
    }
  };

  const approve = (review: ReviewResponse) => {
    const overriding = newRoute !== '' && newRoute !== review.route;
    if (overriding && !reason.trim()) {
      setFormError('Для изменения маршрута требуется причина.');
      return;
    }
    void perform(
      () => reviewsApi.approve(review.reviewId, overriding ? newRoute : null, reason.trim()),
      overriding ? 'Одобрено с изменением маршрута.' : 'Одобрено.',
    );
  };

  const reject = (review: ReviewResponse) => {
    if (!reason.trim()) {
      setFormError('Для отклонения требуется причина.');
      return;
    }
    void perform(() => reviewsApi.reject(review.reviewId, reason.trim()), 'Отклонено.');
  };

  const requestInfo = (review: ReviewResponse) => {
    if (!reason.trim()) {
      setFormError('Для запроса информации требуется причина.');
      return;
    }
    void perform(
      () => reviewsApi.requestInformation(review.reviewId, reason.trim()),
      'Информация запрошена.',
    );
  };

  const assign = (review: ReviewResponse, target: string) => {
    if (!target.trim()) {
      setFormError('Укажите ID пользователя для назначения.');
      return;
    }
    void perform(() => reviewsApi.assign(review.reviewId, target.trim()), 'Назначено.');
  };

  const selected = reviews.find((r) => r.reviewId === openId) ?? null;

  return (
    <section>
      <div className="page-head">
        <h2>Ручные проверки</h2>
        {role && <span className="muted">Роль: {role}</span>}
      </div>

      {error && <p className="error" role="alert">{error}</p>}
      {notice && !error && <p className="muted" role="status">{notice}</p>}

      {loading ? (
        <p className="muted">Loading reviews…</p>
      ) : reviews.length === 0 ? (
        <p className="muted">Задач на проверку нет.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Проверка</th>
              <th>Дело</th>
              <th>Статус</th>
              <th>Маршрут</th>
              <th>Исполнитель</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {reviews.map((review) => (
              <tr key={review.reviewId}>
                <td className="mono">{review.reviewId.slice(0, 8)}</td>
                <td className="mono">{review.applicationId.slice(0, 8)}</td>
                <td>{reviewStatusRu(review.status)}</td>
                <td>{routeRu(review.route)}</td>
                <td className="mono">{review.assigneeId ? review.assigneeId.slice(0, 8) : '—'}</td>
                <td>
                  <button type="button" onClick={() => openReview(review.reviewId)}>
                    {openId === review.reviewId ? 'Скрыть' : 'Открыть'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {selected && (
        <div className="review-detail">
          <h3>
            Проверка <span className="mono">{selected.reviewId.slice(0, 8)}</span>
          </h3>
          <p className="muted">
            Рекомендованный маршрут: <strong>{routeRu(selected.route)}</strong> (набор правил {selected.rulesetVersion})
          </p>
          {selected.lastDecisionReason && (
            <p className="muted">Последняя причина: {selected.lastDecisionReason}</p>
          )}

          {readOnly ? (
            <p className="muted">Доступ только для чтения: ваша роль не может изменять проверки.</p>
          ) : (
            <>
              {formError && <p className="error" role="alert">{formError}</p>}

              {canAssign && (
                <div className="review-action">
                  <h4>Назначение</h4>
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => user && assign(selected, user.userId)}
                  >
                    Назначить себе
                  </button>{' '}
                  <input
                    type="text"
                    placeholder="ID пользователя"
                    aria-label="ID пользователя-исполнителя"
                    value={assigneeId}
                    onChange={(e) => setAssigneeId(e.target.value)}
                  />{' '}
                  <button type="button" disabled={busy} onClick={() => assign(selected, assigneeId)}>
                    Назначить
                  </button>
                </div>
              )}

              <div className="review-action">
                <h4>Решение</h4>
                <label htmlFor="review-reason">Причина</label>
                <textarea
                  id="review-reason"
                  value={reason}
                  maxLength={1000}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="Обязательна для отклонения, запроса информации и изменения маршрута"
                />

                {canDecide && (
                  <>
                    <label htmlFor="review-route">Подтверждённый маршрут</label>
                    <select
                      id="review-route"
                      value={newRoute}
                      onChange={(e) => setNewRoute(e.target.value as '' | BankruptcyRoute)}
                    >
                      <option value="">Оставить рекомендованный ({routeRu(selected.route)})</option>
                      {ROUTES.filter((r) => r !== selected.route).map((r) => (
                        <option key={r} value={r}>
                          Изменить на {routeRu(r)}
                        </option>
                      ))}
                    </select>
                    <div className="review-buttons">
                      <button type="button" disabled={busy} onClick={() => approve(selected)}>
                        Одобрить
                      </button>{' '}
                      <button type="button" disabled={busy} onClick={() => reject(selected)}>
                        Отклонить
                      </button>
                    </div>
                  </>
                )}

                {canRequestInfo && (
                  <div className="review-buttons">
                    <button type="button" disabled={busy} onClick={() => requestInfo(selected)}>
                      Запросить информацию
                    </button>
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      )}
    </section>
  );
}
