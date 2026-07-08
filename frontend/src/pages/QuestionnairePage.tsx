import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { questionnaireApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import type { FormResponse, QuestionResponse, ValidationResponse } from '../api/types';
import QuestionField from '../components/QuestionField';
import QuestionnaireProgress from '../components/QuestionnaireProgress';

type SaveState = 'idle' | 'saving' | 'saved' | 'error';

// Stepped questionnaire flow. The form structure, questions, and validation rules are entirely
// backend-driven — this component only renders what the API returns, autosaves each answer, and
// surfaces the backend's validation. It contains NO eligibility or legal logic.
//
// Answers are saved incrementally on commit (blur / change), so they survive step navigation,
// refresh, and resume: on mount we reload the saved answers from the server.
export default function QuestionnairePage() {
  const { applicationId } = useParams<{ applicationId: string }>();
  const [form, setForm] = useState<FormResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [stepIndex, setStepIndex] = useState(0);
  const [saveState, setSaveState] = useState<SaveState>('idle');
  const [saveError, setSaveError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [validation, setValidation] = useState<ValidationResponse | null>(null);
  const [validating, setValidating] = useState(false);

  useEffect(() => {
    if (!applicationId) return;
    let active = true;
    setLoading(true);
    questionnaireApi
      .form(applicationId)
      .then((data) => {
        if (active) setForm(data);
      })
      .catch((err) => {
        if (active) {
          setLoadError(err instanceof ApiError ? err.message : 'Не удалось загрузить анкету');
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [applicationId]);

  const sections = useMemo(
    () => (form ? [...form.sections].sort((a, b) => a.displayOrder - b.displayOrder) : []),
    [form],
  );

  const questionLabel = useMemo(() => {
    const map = new Map<string, string>();
    form?.questions.forEach((q) => map.set(q.code, q.label));
    return map;
  }, [form]);

  const questionSection = useMemo(() => {
    const map = new Map<string, string>();
    form?.questions.forEach((q) => map.set(q.code, q.sectionCode));
    return map;
  }, [form]);

  if (loading) return <p className="muted">Загрузка анкеты…</p>;
  if (loadError)
    return (
      <p className="error" role="alert">
        {loadError}
      </p>
    );
  if (!form) return null;

  const currentSection = sections[stepIndex];
  const currentQuestions = form.questions
    .filter((q) => q.sectionCode === currentSection.code)
    .sort((a, b) => a.displayOrder - b.displayOrder);
  const isLastStep = stepIndex === sections.length - 1;

  const saveAnswer = async (question: QuestionResponse, value: string) => {
    if (!applicationId) return;
    // Optimistic local update so navigation never loses an in-progress answer.
    setForm((current) =>
      current ? { ...current, answers: { ...current.answers, [question.code]: value } } : current,
    );
    setSaveState('saving');
    setSaveError(null);
    try {
      const completion = await questionnaireApi.saveAnswer(applicationId, question.code, value);
      setForm((current) => (current ? { ...current, completion } : current));
      setFieldErrors((prev) => {
        if (!prev[question.code]) return prev;
        const next = { ...prev };
        delete next[question.code];
        return next;
      });
      setSaveState('saved');
    } catch (err) {
      setSaveState('error');
      if (err instanceof ApiError && err.body?.fieldErrors?.length) {
        setFieldErrors((prev) => ({
          ...prev,
          ...Object.fromEntries(err.body!.fieldErrors!.map((fe) => [fe.field, fe.message])),
        }));
      } else {
        setSaveError(err instanceof ApiError ? err.message : 'Не удалось сохранить ответ');
      }
    }
  };

  const runValidation = async () => {
    if (!applicationId) return;
    setValidating(true);
    setSaveError(null);
    try {
      const result = await questionnaireApi.validate(applicationId);
      setValidation(result);
      setFieldErrors(Object.fromEntries(result.fieldErrors.map((fe) => [fe.field, fe.message])));
    } catch (err) {
      setSaveError(err instanceof ApiError ? err.message : 'Не удалось проверить анкету');
    } finally {
      setValidating(false);
    }
  };

  const jumpToQuestion = (code: string) => {
    const sectionCode = questionSection.get(code);
    const index = sections.findIndex((s) => s.code === sectionCode);
    if (index >= 0) setStepIndex(index);
  };

  return (
    <section className="questionnaire">
      <div className="page-head">
        <h2>{form.label}</h2>
        <SaveIndicator state={saveState} />
      </div>

      <QuestionnaireProgress
        sections={sections}
        currentIndex={stepIndex}
        requiredAnswered={form.completion.requiredAnswered}
        requiredTotal={form.completion.requiredTotal}
        onJump={setStepIndex}
      />

      {saveError && (
        <p className="error banner" role="alert">
          {saveError}
        </p>
      )}

      <fieldset className="section" key={currentSection.code}>
        <legend>{currentSection.title}</legend>
        {currentQuestions.map((question) => (
          <QuestionField
            key={question.code}
            question={question}
            value={form.answers[question.code] ?? ''}
            onCommit={(value) => saveAnswer(question, value)}
            error={fieldErrors[question.code] ?? null}
          />
        ))}
      </fieldset>

      <div className="qn-nav">
        <button
          type="button"
          className="secondary"
          onClick={() => setStepIndex((i) => Math.max(0, i - 1))}
          disabled={stepIndex === 0}
        >
          Назад
        </button>
        {isLastStep ? (
          <button type="button" onClick={runValidation} disabled={validating}>
            {validating ? 'Проверяем…' : 'Проверить ответы'}
          </button>
        ) : (
          <button type="button" onClick={() => setStepIndex((i) => Math.min(sections.length - 1, i + 1))}>
            Далее
          </button>
        )}
      </div>

      {validation && (
        <div
          className={`qn-validation ${validation.complete ? 'ok' : 'incomplete'}`}
          role="status"
        >
          {validation.complete ? (
            <p>Все обязательные вопросы отвечены. Дело готово к подаче.</p>
          ) : (
            <>
              <p className="warning">Остались обязательные вопросы без ответа:</p>
              <ul>
                {validation.missingRequired.map((code) => (
                  <li key={code}>
                    <button type="button" className="link" onClick={() => jumpToQuestion(code)}>
                      {questionLabel.get(code) ?? code}
                    </button>
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}
    </section>
  );
}

function SaveIndicator({ state }: { state: SaveState }) {
  if (state === 'idle') return null;
  const text =
    state === 'saving' ? 'Сохраняем…' : state === 'saved' ? 'Все изменения сохранены' : 'Не удалось сохранить';
  return (
    <span className={`save-indicator ${state}`} role="status" aria-live="polite">
      {text}
    </span>
  );
}
