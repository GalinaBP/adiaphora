import { useState } from 'react';
import type { QuestionResponse } from '../api/types';

interface Props {
  question: QuestionResponse;
  value: string;
  // Called when the user finishes editing (blur / change for discrete inputs).
  onCommit: (value: string) => void;
}

// Renders a single question as the appropriate input for its type. Purely a presentational
// mapping from QuestionType to a control — no validation beyond the HTML `required` hint.
export default function QuestionField({ question, value, onCommit }: Props) {
  const [local, setLocal] = useState(value);
  const id = `q-${question.code}`;

  const control = () => {
    switch (question.type) {
      case 'TEXTAREA':
        return (
          <textarea
            id={id}
            value={local}
            required={question.required}
            onChange={(e) => setLocal(e.target.value)}
            onBlur={() => onCommit(local)}
          />
        );
      case 'INTEGER':
      case 'MONEY':
        return (
          <input
            id={id}
            type="number"
            inputMode="decimal"
            value={local}
            required={question.required}
            onChange={(e) => setLocal(e.target.value)}
            onBlur={() => onCommit(local)}
          />
        );
      case 'BOOLEAN':
        return (
          <input
            id={id}
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
            id={id}
            value={local}
            required={question.required}
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
            id={id}
            type="text"
            value={local}
            required={question.required}
            onChange={(e) => setLocal(e.target.value)}
            onBlur={() => onCommit(local)}
          />
        );
    }
  };

  return (
    <div className="field">
      <label htmlFor={id}>
        {question.label}
        {question.required && <span className="req" aria-hidden="true"> *</span>}
      </label>
      {control()}
      {question.helpText && <p className="help muted">{question.helpText}</p>}
    </div>
  );
}
