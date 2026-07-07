import type { SectionResponse } from '../api/types';

interface Props {
  sections: SectionResponse[];
  currentIndex: number;
  requiredAnswered: number;
  requiredTotal: number;
  // Jump straight to a section (the stepper is navigable, not just decorative).
  onJump: (index: number) => void;
}

// Presentational progress header for the questionnaire wizard: an overall required-answers bar,
// a "Section X of N" label, and a navigable list of section steps.
export default function QuestionnaireProgress({
  sections,
  currentIndex,
  requiredAnswered,
  requiredTotal,
  onJump,
}: Props) {
  const pct = requiredTotal === 0 ? 100 : Math.round((requiredAnswered / requiredTotal) * 100);

  return (
    <div className="qn-progress">
      <div className="qn-progress-head">
        <span className="muted">
          Section {currentIndex + 1} of {sections.length}
        </span>
        <span className="muted" data-testid="required-progress">
          {requiredAnswered}/{requiredTotal} required answered
        </span>
      </div>

      <progress
        className="qn-bar"
        max={100}
        value={pct}
        aria-label="Required answers completed"
      />

      <ol className="qn-steps">
        {sections.map((section, index) => (
          <li key={section.code}>
            <button
              type="button"
              className={`qn-step${index === currentIndex ? ' current' : ''}`}
              aria-current={index === currentIndex ? 'step' : undefined}
              onClick={() => onJump(index)}
            >
              <span className="qn-step-num" aria-hidden="true">
                {index + 1}
              </span>
              <span className="qn-step-title">{section.title}</span>
            </button>
          </li>
        ))}
      </ol>
    </div>
  );
}
