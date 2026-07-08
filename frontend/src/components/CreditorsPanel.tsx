import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { creditorsApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import type { CreditorResponse, CreditorType } from '../api/types';
import { creditorTypeRu } from '../i18n/labels';

const TYPES: CreditorType[] = ['BANK', 'MICROFINANCE', 'INDIVIDUAL', 'TAX_AUTHORITY', 'UTILITY', 'OTHER'];

interface Draft {
  name: string;
  type: CreditorType;
  inn: string;
  totalAmount: string;
}

const EMPTY: Draft = { name: '', type: 'BANK', inn: '', totalAmount: '' };

export default function CreditorsPanel({ applicationId }: { applicationId: string }) {
  const [items, setItems] = useState<CreditorResponse[]>([]);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draft, setDraft] = useState<Draft>(EMPTY);
  const [error, setError] = useState<string | null>(null);
  const [warning, setWarning] = useState<string | null>(null);

  const load = () =>
    creditorsApi
      .list(applicationId)
      .then(setItems)
      .catch((e) => setError(e instanceof ApiError ? e.message : 'Не удалось загрузить кредиторов'));

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [applicationId]);

  const reset = () => {
    setEditingId(null);
    setDraft(EMPTY);
  };

  const startEdit = (c: CreditorResponse) => {
    setEditingId(c.creditorId);
    setDraft({ name: c.name, type: c.type, inn: c.inn ?? '', totalAmount: String(c.totalAmount) });
    setWarning(null);
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setWarning(null);
    const body = {
      name: draft.name,
      type: draft.type,
      inn: draft.inn || null,
      totalAmount: Number(draft.totalAmount),
    };
    try {
      const saved = editingId
        ? await creditorsApi.update(applicationId, editingId, body)
        : await creditorsApi.create(applicationId, body);
      if (saved.duplicateWarning) {
        setWarning('Похожий кредитор уже есть в этом деле.');
      }
      reset();
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Не удалось сохранить кредитора');
    }
  };

  const remove = async (id: string) => {
    setError(null);
    try {
      await creditorsApi.remove(applicationId, id);
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Не удалось удалить кредитора');
    }
  };

  return (
    <section className="panel">
      <h3>Кредиторы</h3>
      {error && <p className="error" role="alert">{error}</p>}
      {warning && <p className="warning" role="status">{warning}</p>}

      {items.length === 0 ? (
        <p className="muted">Кредиторы пока не добавлены.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Название</th>
              <th>Тип</th>
              <th>Сумма</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {items.map((c) => (
              <tr key={c.creditorId}>
                <td>{c.name}</td>
                <td>{creditorTypeRu(c.type)}</td>
                <td>{c.totalAmount} {c.currency}</td>
                <td>
                  <button type="button" onClick={() => startEdit(c)}>Изменить</button>{' '}
                  <button type="button" onClick={() => remove(c.creditorId)}>Удалить</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <form className="inline-form" onSubmit={submit} aria-label="Форма кредитора">
        <input
          aria-label="Название кредитора"
          placeholder="Название"
          value={draft.name}
          required
          onChange={(e) => setDraft({ ...draft, name: e.target.value })}
        />
        <select
          aria-label="Тип кредитора"
          value={draft.type}
          onChange={(e) => setDraft({ ...draft, type: e.target.value as CreditorType })}
        >
          {TYPES.map((t) => (
            <option key={t} value={t}>{creditorTypeRu(t)}</option>
          ))}
        </select>
        <input
          aria-label="ИНН кредитора"
          placeholder="ИНН (необязательно)"
          value={draft.inn}
          onChange={(e) => setDraft({ ...draft, inn: e.target.value })}
        />
        <input
          aria-label="Общая сумма"
          type="number"
          placeholder="Общая сумма"
          value={draft.totalAmount}
          required
          min="0"
          onChange={(e) => setDraft({ ...draft, totalAmount: e.target.value })}
        />
        <button type="submit">{editingId ? 'Сохранить' : 'Добавить кредитора'}</button>
        {editingId && (
          <button type="button" onClick={reset}>Отмена</button>
        )}
      </form>
    </section>
  );
}
