import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { assetsApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import type { AssetResponse, AssetType } from '../api/types';
import { assetTypeRu } from '../i18n/labels';

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
      .catch((e) => setError(e instanceof ApiError ? e.message : 'Не удалось загрузить имущество'));

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
        setWarning('Похожее имущество уже есть в этом деле.');
      }
      reset();
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Не удалось сохранить имущество');
    }
  };

  const remove = async (id: string) => {
    setError(null);
    try {
      await assetsApi.remove(applicationId, id);
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Не удалось удалить имущество');
    }
  };

  return (
    <section className="panel">
      <h3>Имущество</h3>
      {error && <p className="error" role="alert">{error}</p>}
      {warning && <p className="warning" role="status">{warning}</p>}

      {items.length === 0 ? (
        <p className="muted">Имущество пока не добавлено.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Описание</th>
              <th>Тип</th>
              <th>Стоимость</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {items.map((a) => (
              <tr key={a.assetId}>
                <td>{a.description}</td>
                <td>{assetTypeRu(a.type)}</td>
                <td>{a.estimatedValue} {a.currency}</td>
                <td>
                  <button type="button" onClick={() => startEdit(a)}>Изменить</button>{' '}
                  <button type="button" onClick={() => remove(a.assetId)}>Удалить</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <form className="inline-form" onSubmit={submit} aria-label="Форма имущества">
        <input
          aria-label="Описание имущества"
          placeholder="Описание"
          value={draft.description}
          required
          onChange={(e) => setDraft({ ...draft, description: e.target.value })}
        />
        <select
          aria-label="Тип имущества"
          value={draft.type}
          onChange={(e) => setDraft({ ...draft, type: e.target.value as AssetType })}
        >
          {TYPES.map((t) => (
            <option key={t} value={t}>{t}</option>
          ))}
        </select>
        <input
          aria-label="Оценочная стоимость"
          type="number"
          placeholder="Оценочная стоимость"
          value={draft.estimatedValue}
          required
          min="0"
          onChange={(e) => setDraft({ ...draft, estimatedValue: e.target.value })}
        />
        <button type="submit">{editingId ? 'Сохранить' : 'Добавить имущество'}</button>
        {editingId && (
          <button type="button" onClick={reset}>Отмена</button>
        )}
      </form>
    </section>
  );
}
