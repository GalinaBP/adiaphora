import { useEffect, useState } from 'react';
import { reviewsApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import type { BankruptcyRoute, ReviewResponse } from '../api/types';

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
        if (active) setError(err instanceof ApiError ? err.message : 'Failed to load reviews');
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
      setError(err instanceof ApiError ? err.message : 'The action failed. Please try again.');
    } finally {
      setBusy(false);
    }
  };

  const approve = (review: ReviewResponse) => {
    const overriding = newRoute !== '' && newRoute !== review.route;
    if (overriding && !reason.trim()) {
      setFormError('Overriding the recommended route requires a reason.');
      return;
    }
    void perform(
      () => reviewsApi.approve(review.reviewId, overriding ? newRoute : null, reason.trim()),
      overriding ? 'Approved with route override.' : 'Approved.',
    );
  };

  const reject = (review: ReviewResponse) => {
    if (!reason.trim()) {
      setFormError('Rejecting a review requires a reason.');
      return;
    }
    void perform(() => reviewsApi.reject(review.reviewId, reason.trim()), 'Rejected.');
  };

  const requestInfo = (review: ReviewResponse) => {
    if (!reason.trim()) {
      setFormError('Requesting information requires a reason.');
      return;
    }
    void perform(
      () => reviewsApi.requestInformation(review.reviewId, reason.trim()),
      'Information requested.',
    );
  };

  const assign = (review: ReviewResponse, target: string) => {
    if (!target.trim()) {
      setFormError('Enter the user id to assign this review to.');
      return;
    }
    void perform(() => reviewsApi.assign(review.reviewId, target.trim()), 'Assigned.');
  };

  const selected = reviews.find((r) => r.reviewId === openId) ?? null;

  return (
    <section>
      <div className="page-head">
        <h2>Manual reviews</h2>
        {role && <span className="muted">Role: {role}</span>}
      </div>

      {error && <p className="error" role="alert">{error}</p>}
      {notice && !error && <p className="muted" role="status">{notice}</p>}

      {loading ? (
        <p className="muted">Loading reviews…</p>
      ) : reviews.length === 0 ? (
        <p className="muted">No review tasks.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Review</th>
              <th>Application</th>
              <th>Status</th>
              <th>Route</th>
              <th>Assignee</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {reviews.map((review) => (
              <tr key={review.reviewId}>
                <td className="mono">{review.reviewId.slice(0, 8)}</td>
                <td className="mono">{review.applicationId.slice(0, 8)}</td>
                <td>{review.status}</td>
                <td>{review.route}</td>
                <td className="mono">{review.assigneeId ? review.assigneeId.slice(0, 8) : '—'}</td>
                <td>
                  <button type="button" onClick={() => openReview(review.reviewId)}>
                    {openId === review.reviewId ? 'Close' : 'Open'}
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
            Review <span className="mono">{selected.reviewId.slice(0, 8)}</span>
          </h3>
          <p className="muted">
            Recommended route: <strong>{selected.route}</strong> (ruleset {selected.rulesetVersion})
          </p>
          {selected.lastDecisionReason && (
            <p className="muted">Last reason: {selected.lastDecisionReason}</p>
          )}

          {readOnly ? (
            <p className="muted">Read-only access: your role cannot modify reviews.</p>
          ) : (
            <>
              {formError && <p className="error" role="alert">{formError}</p>}

              {canAssign && (
                <div className="review-action">
                  <h4>Assign</h4>
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => user && assign(selected, user.userId)}
                  >
                    Assign to me
                  </button>{' '}
                  <input
                    type="text"
                    placeholder="assignee user id"
                    aria-label="Assignee user id"
                    value={assigneeId}
                    onChange={(e) => setAssigneeId(e.target.value)}
                  />{' '}
                  <button type="button" disabled={busy} onClick={() => assign(selected, assigneeId)}>
                    Assign
                  </button>
                </div>
              )}

              <div className="review-action">
                <h4>Decision</h4>
                <label htmlFor="review-reason">Reason</label>
                <textarea
                  id="review-reason"
                  value={reason}
                  maxLength={1000}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="Required for rejections, information requests and route overrides"
                />

                {canDecide && (
                  <>
                    <label htmlFor="review-route">Confirmed route</label>
                    <select
                      id="review-route"
                      value={newRoute}
                      onChange={(e) => setNewRoute(e.target.value as '' | BankruptcyRoute)}
                    >
                      <option value="">Keep recommended ({selected.route})</option>
                      {ROUTES.filter((r) => r !== selected.route).map((r) => (
                        <option key={r} value={r}>
                          Override to {r}
                        </option>
                      ))}
                    </select>
                    <div className="review-buttons">
                      <button type="button" disabled={busy} onClick={() => approve(selected)}>
                        Approve
                      </button>{' '}
                      <button type="button" disabled={busy} onClick={() => reject(selected)}>
                        Reject
                      </button>
                    </div>
                  </>
                )}

                {canRequestInfo && (
                  <div className="review-buttons">
                    <button type="button" disabled={busy} onClick={() => requestInfo(selected)}>
                      Request information
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
