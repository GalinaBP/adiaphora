import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { questionnaireApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import type { FormResponse, QuestionResponse } from '../api/types';
import QuestionField from '../components/QuestionField';

// Shell for answering an application's questionnaire. The form structure, questions, and
// validation rules are entirely backend-driven — this component only renders what the API
// returns and posts answers back. Intentionally contains NO eligibility or legal logic.
export default function QuestionnairePage() {
  const { applicationId } = useParams<{ applicationId: string }>();
  const [form, setForm] = useState<FormResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!applicationId) return;
    let active = true;
    questionnaireApi
      .form(applicationId)
      .then((data) => {
        if (active) setForm(data);
      })
      .catch((err) => {
        if (active) setError(err instanceof ApiError ? err.message : 'Failed to load questionnaire');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [applicationId]);

  const saveAnswer = async (question: QuestionResponse, value: string) => {
    if (!applicationId || !form) return;
    setForm({ ...form, answers: { ...form.answers, [question.code]: value } });
    try {
      const completion = await questionnaireApi.saveAnswer(applicationId, question.code, value);
      setForm((current) => (current ? { ...current, completion } : current));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to save answer');
    }
  };

  if (loading) return <p className="muted">Loading questionnaire…</p>;
  if (error) return <p className="error" role="alert">{error}</p>;
  if (!form) return null;

  const questionsBySection = (sectionCode: string) =>
    form.questions
      .filter((q) => q.sectionCode === sectionCode)
      .sort((a, b) => a.displayOrder - b.displayOrder);

  return (
    <section className="questionnaire">
      <div className="page-head">
        <h2>{form.label}</h2>
        <span className="muted">
          {form.completion.requiredAnswered}/{form.completion.requiredTotal} required answered
        </span>
      </div>

      {form.sections
        .slice()
        .sort((a, b) => a.displayOrder - b.displayOrder)
        .map((section) => (
          <fieldset key={section.code} className="section">
            <legend>{section.title}</legend>
            {questionsBySection(section.code).map((question) => (
              <QuestionField
                key={question.code}
                question={question}
                value={form.answers[question.code] ?? ''}
                onCommit={(value) => saveAnswer(question, value)}
              />
            ))}
          </fieldset>
        ))}
    </section>
  );
}
