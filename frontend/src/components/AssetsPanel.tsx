import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { assetsApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import type { AssetResponse, AssetType } from '../api/types';

const TYPES: AssetType[] = [
  'REAL_ESTATE',
  'VEHICLE',
  'BANK_ACCOUNT',
  'SECURITIES',
  'CASH',
  'RECEIVABLE',
  'MOVABLE_PROPERTY',
  'OTHER',
];

interface Draft {
  type: AssetType;
  description: string;
  estimatedValue: string;
}

const EMPTY: Draft = { type: 'REAL_ESTATE', description: '', estimatedValue: '' };

export default function AssetsPanel({ applicationId }: { applicationId: string }) {
  const [items, setItems] = useState<AssetResponse[]>([]);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draft, setDraft] = useState<Draft>(EMPTY);
  const [error, setError] = useState<string | null>(null);
  const [warning, setWarning] = useState<string | null>(null);

  const load = () =>
    assetsApi
      .list(applicationId)
      .then(setItems)
      .catch((e) => setError(e instanceof ApiError ? e.message : 'Failed to load assets'));

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [applicationId]);

  const reset = () => {
    setEditingId(null);
    setDraft(EMPTY);
  };

  const startEdit = (a: AssetResponse) => {
    setEditingId(a.assetId);
    setDraft({ type: a.type, description: a.description, estimatedValue: String(a.estimatedValue) });
    setWarning(null);
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setWarning(null);
    const body = {
      type: draft.type,
      description: draft.description,
      estimatedValue: Number(draft.estimatedValue),
    };
    try {
      const saved = editingId
        ? await assetsApi.update(applicationId, editingId, body)
        : await assetsApi.create(applicationId, body);
      if (saved.duplicateWarning) {
        setWarning('A similar asset already exists in this case.');
      }
      reset();
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to save asset');
    }
  };

  const remove = async (id: string) => {
    setError(null);
    try {
      await assetsApi.remove(applicationId, id);
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to delete asset');
    }
  };

  return (
    <section className="panel">
      <h3>Assets</h3>
      {error && <p className="error" role="alert">{error}</p>}
      {warning && <p className="warning" role="status">{warning}</p>}

      {items.length === 0 ? (
        <p className="muted">No assets yet.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Description</th>
              <th>Type</th>
              <th>Value</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {items.map((a) => (
              <tr key={a.assetId}>
                <td>{a.description}</td>
                <td>{a.type}</td>
                <td>{a.estimatedValue} {a.currency}</td>
                <td>
                  <button type="button" onClick={() => startEdit(a)}>Edit</button>{' '}
                  <button type="button" onClick={() => remove(a.assetId)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <form className="inline-form" onSubmit={submit} aria-label="Asset form">
        <input
          aria-label="Asset description"
          placeholder="Description"
          value={draft.description}
          required
          onChange={(e) => setDraft({ ...draft, description: e.target.value })}
        />
        <select
          aria-label="Asset type"
          value={draft.type}
          onChange={(e) => setDraft({ ...draft, type: e.target.value as AssetType })}
        >
          {TYPES.map((t) => (
            <option key={t} value={t}>{t}</option>
          ))}
        </select>
        <input
          aria-label="Estimated value"
          type="number"
          placeholder="Estimated value"
          value={draft.estimatedValue}
          required
          min="0"
          onChange={(e) => setDraft({ ...draft, estimatedValue: e.target.value })}
        />
        <button type="submit">{editingId ? 'Save' : 'Add asset'}</button>
        {editingId && (
          <button type="button" onClick={reset}>Cancel</button>
        )}
      </form>
    </section>
  );
}
