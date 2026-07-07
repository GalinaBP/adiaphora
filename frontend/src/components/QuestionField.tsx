import { useEffect, useState } from 'react';
import type { QuestionResponse } from '../api/types';

interface Props {
  question: QuestionResponse;
  value: string;
  // Called when the user finishes editing (blur / change for discrete inputs).
  onCommit: (value: string) => void;
  // Validation message to surface for this question, if any (from the backend).
  error?: string | null;
}

// Renders a single question as the appropriate input for its type. Purely a presentational
// mapping from QuestionType to a control — no validation beyond the HTML `required` hint; the
// authoritative validation is the backend's, surfaced here via the `error` prop.
export default function QuestionField({ question, value, onCommit, error }: Props) {
  const [local, setLocal] = useState(value);
  const id = `q-${question.code}`;
  const errorId = `${id}-error`;
  const helpId = `${id}-help`;

  // Keep the control in sync when the saved value changes underneath us (resume / refetch),
  // without clobbering in-progress typing (the parent mirrors committed values back as `value`).
  useEffect(() => {
    setLocal(value);
  }, [value]);

  const describedBy =
    [error ? errorId : null, question.helpText ? helpId : null].filter(Boolean).join(' ') ||
    undefined;

  const common = {
    id,
    required: question.required,
    'aria-invalid': error ? true : undefined,
    'aria-describedby': describedBy,
  } as const;

  const control = () => {
    switch (question.type) {
      case 'TEXTAREA':
        return (
          <textarea
            {...common}
            value={local}
            onChange={(e) => setLocal(e.target.value)}
            onBlur={() => onCommit(local)}
          />
        );
      case 'INTEGER':
      case 'MONEY':
        return (
          <input
            {...common}
            type="number"
            inputMode="decimal"
            value={local}
            onChange={(e) => setLocal(e.target.value)}
            onBlur={() => onCommit(local)}
          />
        );
      case 'DATE':
        return (
          <input
            {...common}
            type="date"
            value={local}
            onChange={(e) => setLocal(e.target.value)}
            onBlur={() => onCommit(local)}
          />
        );
      case 'BOOLEAN':
        return (
          <input
            {...common}
            type="checkbox"
            checked={local === 'true'}
            onChange={(e) => {
              const next = String(e.target.checked);
              setLocal(next);
              onCommit(next);
            }}
          />
        );
      case 'SINGLE_CHOICE':
      case 'MULTIPLE_CHOICE':
        return (
          <select
            {...common}
            value={local}
            onChange={(e) => {
              setLocal(e.target.value);
              onCommit(e.target.value);
            }}
          >
            <option value="">—</option>
            {question.options.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        );
      default:
        return (
          <input
            {...common}
            type="text"
            value={local}
            onChange={(e) => setLocal(e.target.value)}
            onBlur={() => onCommit(local)}
          />
        );
    }
  };

  return (
    <div className={`field${error ? ' field-invalid' : ''}`}>
      <label htmlFor={id}>
        {question.label}
        {question.required && <span className="req" aria-hidden="true"> *</span>}
      </label>
      {control()}
      {question.helpText && (
        <p id={helpId} className="help muted">
          {question.helpText}
        </p>
      )}
      {error && (
        <p id={errorId} className="field-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
