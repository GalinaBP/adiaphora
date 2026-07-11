import { useEffect, useRef, useState, type ChangeEvent, type FormEvent, type ReactNode } from 'react';
import { Link } from 'react-router-dom';

import { ApiError } from '../api/client';
import { eligibilityApi } from '../api/endpoints';
import type {
  EligibilityEstimateRequest,
  EligibilityEstimateResponse,
  EligibilityVerdict,
  MfcStatutoryGround,
  TriStateAnswer,
} from '../api/types';
import './HomePage.css';

type VerdictCopy = {
  title: string;
  body: string;
  tone: 'positive' | 'attention' | 'neutral';
};

const VERDICT_TEXT: Record<EligibilityVerdict, VerdictCopy> = {
  MFC_ELIGIBLE: {
    title: 'Вам доступно внесудебное банкротство',
    body: 'По указанным ответам условия процедуры через МФЦ выполняются. Ниже — основания, которые подтвердились, со ссылками на закон. Следующий шаг — собрать полный список кредиторов и подготовить заявление.',
    tone: 'positive',
  },
  AMOUNT_OUT_OF_RANGE: {
    title: 'Внесудебное банкротство недоступно: сумма долга вне диапазона',
    body: 'Для процедуры через МФЦ общая сумма учитываемых обязательств должна быть от 25 000 до 1 000 000 ₽ (п. 1 ст. 223.2 Закона № 127-ФЗ). Вам может подойти судебное банкротство — оно проходит через арбитражный суд.',
    tone: 'attention',
  },
  JUDICIAL_ROUTE: {
    title: 'Внесудебное банкротство недоступно — рассмотрите судебную процедуру',
    body: 'По вашим ответам условия процедуры через МФЦ не выполняются. Списание долгов остаётся возможным через судебное банкротство.',
    tone: 'attention',
  },
  MANUAL_REVIEW: {
    title: 'Проверьте документы и пройдите проверку ещё раз',
    body: 'По ответам «не уверен(а)» вывод сделать нельзя. Ниже — что именно и где проверить. После проверки вернитесь и ответьте точно.',
    tone: 'neutral',
  },
  NEEDS_INFORMATION: {
    title: 'Недостаточно данных для предварительной проверки',
    body: 'Ответьте на вопросы всех шагов. Итоговая возможность подачи через МФЦ всё равно подтверждается по официальным сведениям и документам.',
    tone: 'neutral',
  },
};

// Statutory categories for the extrajudicial (MFC) procedure (п. 1 ст. 223.2 Закона № 127-ФЗ).
// Multi-select: the qualifying conditions themselves are asked per category on the next step.
const CATEGORY_OPTIONS: Array<{
  value: MfcStatutoryGround;
  title: string;
  description: string;
}> = [
  {
    value: 'enforcement_ended',
    title: 'Приставы уже работали с моим долгом и закрыли дело',
    description: 'Исполнительное производство оканчивалось из-за отсутствия имущества или денег.',
  },
  {
    value: 'pensioner',
    title: 'Пенсионер',
    description: 'Пенсия — ваш единственный или основной доход.',
  },
  {
    value: 'child_benefit',
    title: 'Получаю единое пособие на ребёнка',
    description:
      'Именно единое пособие, назначаемое Социальным фондом по заявлению — не декретные '
      + 'и не пособия от работодателя.',
  },
  {
    value: 'svo_participant',
    title: 'Участвую или участвовал(а) в СВО',
    description: 'Есть документ, подтверждающий участие в специальной военной операции.',
  },
  {
    value: 'long_enforcement',
    title: 'Долг взыскивают уже 7 лет или дольше',
    description: 'Исполнительный документ выдан не менее семи лет назад.',
  },
  {
    value: 'none',
    title: 'Ни одна ситуация не подходит',
    description: 'Перечисленные категории не о вас.',
  },
];

const MFC_LOWER_BOUND = 25000;
const MFC_UPPER_BOUND = 1000000;

/** True when the previous procedure ended less than 5 years ago (п. 8 ст. 223.2 127-ФЗ). */
function insideFiveYearBar(endedOn: string): boolean {
  const ended = new Date(endedOn);
  if (Number.isNaN(ended.getTime())) {
    return false;
  }
  const barEnd = new Date(ended);
  barEnd.setFullYear(barEnd.getFullYear() + 5);
  return barEnd > new Date();
}

const TRI_STATE_LABELS: Record<TriStateAnswer, string> = {
  yes: 'Да',
  no: 'Нет',
  not_sure: 'Не уверен(а)',
};

function Icon({ children, size = 24 }: { children: ReactNode; size?: number }) {
  return (
    <svg
      aria-hidden="true"
      className="home-icon"
      fill="none"
      height={size}
      viewBox="0 0 24 24"
      width={size}
    >
      {children}
    </svg>
  );
}

function CheckIcon() {
  return (
    <Icon size={18}>
      <path d="m5 12 4 4L19 6" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" />
    </Icon>
  );
}

function ArrowIcon() {
  return (
    <Icon size={18}>
      <path d="M5 12h14m-5-5 5 5-5 5" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" />
    </Icon>
  );
}

type TriState = '' | TriStateAnswer;

function TriStateField({ legend, name, value, onChange, help }: {
  legend: string;
  name: string;
  value: TriState;
  onChange: (value: TriStateAnswer) => void;
  help?: ReactNode;
}) {
  return (
    <fieldset className="home-tristate home-field-wide">
      <legend>{legend}</legend>
      <div className="home-tristate-options">
        {(['yes', 'no', 'not_sure'] as const).map((option) => (
          <label
            className={`home-tristate-option${value === option ? ' selected' : ''}`}
            key={option}
          >
            <input
              checked={value === option}
              name={name}
              onChange={() => onChange(option)}
              type="radio"
              value={option}
            />
            <span>{TRI_STATE_LABELS[option]}</span>
          </label>
        ))}
      </div>
      {help}
    </fieldset>
  );
}

export default function HomePage() {
  // The flow is staged with hard gates: 1 debt amount, 2 prior bankruptcy (5-year bar),
  // 3 category multi-select, 4 category follow-up blocks. Failing stage 1–2 or picking
  // "none" alone submits what has been answered and shows the judicial-route result.
  const [stage, setStage] = useState<1 | 2 | 3 | 4>(1);
  const [debt, setDebt] = useState('');
  const [priorBankruptcy, setPriorBankruptcy] = useState<'' | 'yes' | 'no'>('');
  const [priorEndedOn, setPriorEndedOn] = useState('');
  const [grounds, setGrounds] = useState<MfcStatutoryGround[]>([]);
  const [bailiffsClosed, setBailiffsClosed] = useState<TriState>('');
  const [benefitConfirmed, setBenefitConfirmed] = useState<TriState>('');
  const [writOverYear, setWritOverYear] = useState<TriState>('');
  const [sellableProperty, setSellableProperty] = useState<TriState>('');
  const [writOverSevenYears, setWritOverSevenYears] = useState<TriState>('');
  const [stageError, setStageError] = useState<string | null>(null);
  const [result, setResult] = useState<EligibilityEstimateResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // Switching stages changes the card height drastically; keep the form in view.
  const formRef = useRef<HTMLFormElement>(null);
  const initialRender = useRef(true);
  useEffect(() => {
    if (initialRender.current) {
      initialRender.current = false;
      return;
    }
    formRef.current?.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
  }, [stage, result]);

  const hasGround = (value: MfcStatutoryGround) => grounds.includes(value);
  // Block B is shared by the pensioner/child-benefit/SVO categories and shown once. When the
  // unified benefit is not confirmed, the shared questions stay relevant only if another
  // category from the block is selected.
  const showsSharedBlock = hasGround('pensioner') || hasGround('svo_participant')
    || (hasGround('child_benefit') && benefitConfirmed === 'yes');

  const toggleGround = (value: MfcStatutoryGround) => {
    setStageError(null);
    setGrounds((current) => {
      if (current.includes(value)) {
        return current.filter((g) => g !== value);
      }
      // "none" is exclusive: picking it clears the real categories and vice versa.
      return value === 'none' ? ['none'] : [...current.filter((g) => g !== 'none'), value];
    });
  };

  const buildRequest = (): EligibilityEstimateRequest => ({
    totalDebtAmount: debt === '' ? null : Number(debt),
    previousBankruptcy: priorBankruptcy === '' ? null : priorBankruptcy === 'yes',
    previousBankruptcyEndedOn:
      priorBankruptcy === 'yes' && priorEndedOn !== '' ? priorEndedOn : null,
    mfcStatutoryGrounds: grounds.length > 0 ? grounds : null,
    bailiffsCaseClosedNoNew:
      hasGround('enforcement_ended') && bailiffsClosed !== '' ? bailiffsClosed : null,
    childBenefitConfirmed:
      hasGround('child_benefit') && benefitConfirmed !== '' ? benefitConfirmed : null,
    writUnpaidOverOneYear: showsSharedBlock && writOverYear !== '' ? writOverYear : null,
    ownsSellableProperty: showsSharedBlock && sellableProperty !== '' ? sellableProperty : null,
    writIssuedOverSevenYears:
      hasGround('long_enforcement') && writOverSevenYears !== '' ? writOverSevenYears : null,
  });

  const submitEstimate = async () => {
    setBusy(true);
    setError(null);
    try {
      setResult(await eligibilityApi.estimate(buildRequest()));
    } catch (caught) {
      setResult(null);
      setError(
        caught instanceof ApiError
          ? caught.message
          : 'Проверка сейчас недоступна. Попробуйте позже.',
      );
    } finally {
      setBusy(false);
    }
  };

  // Stage 1 hard gate: an out-of-range amount ends the flow (the estimate is still submitted so
  // the decision and its legal basis come from the backend).
  const continueFromDebt = () => {
    if (debt === '') {
      setStageError('Укажите общую сумму долгов.');
      return;
    }
    setStageError(null);
    const amount = Number(debt);
    if (amount < MFC_LOWER_BOUND || amount > MFC_UPPER_BOUND) {
      void submitEstimate();
      return;
    }
    setStage(2);
  };

  // Stage 2 hard gate: a bankruptcy that ended less than 5 years ago ends the flow.
  const continueFromPriorBankruptcy = () => {
    if (priorBankruptcy === '') {
      setStageError('Ответьте, признавались ли вы банкротом ранее.');
      return;
    }
    if (priorBankruptcy === 'yes' && priorEndedOn === '') {
      setStageError('Укажите, когда завершилась предыдущая процедура.');
      return;
    }
    setStageError(null);
    if (priorBankruptcy === 'yes' && insideFiveYearBar(priorEndedOn)) {
      void submitEstimate();
      return;
    }
    setStage(3);
  };

  // Stage 3 gate: "none" alone ends the flow; otherwise the follow-up blocks open.
  const continueFromGrounds = () => {
    if (grounds.length === 0) {
      setStageError('Отметьте хотя бы один вариант.');
      return;
    }
    setStageError(null);
    if (grounds.length === 1 && grounds[0] === 'none') {
      void submitEstimate();
      return;
    }
    setStage(4);
  };

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (hasGround('enforcement_ended') && bailiffsClosed === '') {
      setStageError('Ответьте на вопрос о закрытом производстве приставов.');
      return;
    }
    if (hasGround('child_benefit') && benefitConfirmed === '') {
      setStageError('Ответьте на вопрос о едином пособии.');
      return;
    }
    if (showsSharedBlock && (writOverYear === '' || sellableProperty === '')) {
      setStageError('Ответьте на оба вопроса об исполнительном документе и имуществе.');
      return;
    }
    if (hasGround('long_enforcement') && writOverSevenYears === '') {
      setStageError('Ответьте на вопрос о давности исполнительного документа.');
      return;
    }
    setStageError(null);
    void submitEstimate();
  };

  const resetFlow = () => {
    setStage(1);
    setDebt('');
    setPriorBankruptcy('');
    setPriorEndedOn('');
    setGrounds([]);
    setBailiffsClosed('');
    setBenefitConfirmed('');
    setWritOverYear('');
    setSellableProperty('');
    setWritOverSevenYears('');
    setStageError(null);
    setResult(null);
    setError(null);
  };

  const verdictText = result ? VERDICT_TEXT[result.verdict] : null;

  return (
    <div className="landing-page">
      <header className="home-header">
        <div className="home-container home-header-inner">
          <Link className="home-brand" to="/" aria-label="Адиафора — главная">
            <span className="home-brand-mark" aria-hidden="true">
              <svg fill="none" viewBox="0 0 38 38">
                <path d="M11 27.5 19 8l8 19.5" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" />
                <path d="M14.2 21h9.6" stroke="currentColor" strokeLinecap="round" strokeWidth="3" />
              </svg>
            </span>
            <span>
              <strong>Адиафора</strong>
              <small>Внесудебное банкротство через МФЦ</small>
            </span>
          </Link>

          <nav className="home-nav" aria-label="Основная навигация">
            <a href="#check">Проверка</a>
            <a href="#conditions">Условия</a>
            <a href="#process">Этапы</a>
            <a href="#important">Важно знать</a>
          </nav>

          <div className="home-header-actions">
            <Link className="home-login-link" to="/login">Войти</Link>
            <Link className="home-button home-button-small" to="/register">
              Начать
            </Link>
          </div>
        </div>
      </header>

      <main>
        <section className="home-hero">
          <div className="home-container home-hero-grid">
            <div className="home-hero-copy">
              <span className="home-eyebrow">
                <span className="home-eyebrow-dot" /> Бесплатная процедура через МФЦ
              </span>
              <h1>Проверьте, подходит ли вам внесудебное банкротство</h1>
              <p className="home-lead">
                Ответьте на несколько вопросов и подготовьте сведения для подачи
                заявления через МФЦ — спокойно, по шагам и без сложных формулировок.
              </p>

              <div className="home-hero-actions">
                <a className="home-button" href="#check">
                  Проверить условия <ArrowIcon />
                </a>
                <a className="home-button home-button-secondary" href="#process">
                  Посмотреть этапы
                </a>
              </div>

              <ul className="home-trust-list" aria-label="Основные свойства процедуры">
                <li><CheckIcon /> Подача через МФЦ бесплатна</li>
                <li><CheckIcon /> Процедура длится 6 месяцев</li>
                <li><CheckIcon /> Быстрая проверка не сохраняет ответы</li>
              </ul>
            </div>

            <div className="home-hero-visual" aria-label="Этапы подготовки к подаче через МФЦ">
              <div className="home-orbit home-orbit-one" />
              <div className="home-orbit home-orbit-two" />
              <div className="home-preview-card">
                <div className="home-preview-head">
                  <span className="home-preview-icon">
                    <Icon>
                      <path d="M4 20h16M6 17V9m4 8V9m4 8V9m4 8V9M3 7l9-4 9 4H3Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
                    </Icon>
                  </span>
                  <div>
                    <small>Подготовка к МФЦ</small>
                    <strong>Ваш прогресс</strong>
                  </div>
                </div>
                <div className="home-preview-progress">
                  <span style={{ width: '68%' }} />
                </div>
                <div className="home-preview-steps">
                  <div className="done"><span>1</span><p><strong>Сумма долга</strong><small>Проверка диапазона</small></p><CheckIcon /></div>
                  <div className="active"><span>2</span><p><strong>Условия МФЦ</strong><small>Предварительная оценка</small></p><span className="home-step-pulse" /></div>
                  <div><span>3</span><p><strong>Заявление</strong><small>Сведения и кредиторы</small></p></div>
                </div>
                <div className="home-preview-note">
                  <Icon size={20}>
                    <path d="M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z" stroke="currentColor" strokeWidth="1.8" />
                    <path d="M12 10v6m0-9v.1" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
                  </Icon>
                  MVP помогает подготовиться только к внесудебной процедуре через МФЦ.
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="home-check-section" id="check">
          <div className="home-container">
            <div className="home-section-heading home-section-heading-centered">
              <span className="home-kicker">Быстрая проверка</span>
              <h2>Можно ли продолжить подготовку к подаче через МФЦ?</h2>
              <p>
                Эта форма даёт только предварительный результат. Для окончательной
                проверки нужны сведения об исполнительных производствах, долгах,
                имуществе и вашем основании для обращения.
              </p>
            </div>

            <div className="home-check-grid">
              <form className="home-check-card" onSubmit={submit} ref={formRef}>
                {!result && (
                <div className="home-form-intro">
                  <span className="home-form-number">{`0${stage}`}</span>
                  <div>
                    <h3>
                      {stage === 1 && 'Сумма долга'}
                      {stage === 2 && 'Предыдущее банкротство'}
                      {stage === 3 && 'Ваша ситуация'}
                      {stage === 4 && 'Уточняющие вопросы'}
                    </h3>
                    <p>
                      {stage === 1 && 'Шаг 1 из 4. Первое условие закона: от 25 000 до 1 000 000 ₽.'}
                      {stage === 2 && 'Шаг 2 из 4. Повторная внесудебная процедура возможна не раньше чем через 5 лет.'}
                      {stage === 3 && 'Шаг 3 из 4. Достаточно подойти хотя бы под одну категорию — можно выбрать несколько.'}
                      {stage === 4 && 'Шаг 4 из 4. Вопросы только по выбранным категориям.'}
                    </p>
                  </div>
                </div>
                )}

                {!result && stage === 1 && (
                <div className="home-fields-grid">
                  <label className="home-field home-field-wide">
                    <span>Общая сумма долгов (₽)</span>
                    <input
                      inputMode="numeric"
                      min="0"
                      name="debt"
                      onChange={(event: ChangeEvent<HTMLInputElement>) => setDebt(event.target.value)}
                      placeholder="Например, 350 000"
                      step="1"
                      type="number"
                      value={debt}
                    />
                    <small>Размер долга определяется на дату подачи заявления в МФЦ</small>
                  </label>

                  <details className="home-debt-breakdown home-field-wide">
                    <summary>Из чего складывается общая сумма долга?</summary>
                    <ul>
                      <li>
                        займы и кредиты, включая проценты по ним — точную сумму долга можно запросить у кредитора;
                        <details className="home-bki-guide">
                          <summary>Как узнать, какие кредиты оформлены на ваше имя?</summary>
                          <ol>
                            <li>Зайдите на Госуслуги и запросите перечень бюро кредитных историй (БКИ), где хранятся ваши данные.</li>
                            <li>Откройте официальный сайт каждого указанного БКИ, войдите через Госуслуги и закажите кредитный отчёт.</li>
                          </ol>
                          <p>
                            В отчёте будут отражены действующие кредиты, займы, кредитные карты, остатки задолженности
                            и возможные просрочки. Важно проверить все БКИ из списка — информация может храниться
                            в разных бюро. Отчёт в каждом БКИ предоставляется бесплатно два раза за календарный год,
                            последующие запросы могут быть платными.
                          </p>
                        </details>
                      </li>
                      <li>налоги и сборы — задолженность можно проверить на сайте ФНС или на Госуслугах;</li>
                      <li>обязательства со сроком, который ещё не наступил;</li>
                      <li>платежи по договорам поручительства, включая суммы, по которым нет просрочки платежей;</li>
                      <li>судебная задолженность — её можно проверить в личном кабинете на Госуслугах или на сайте ФССП;</li>
                      <li>алименты — их запросят для учёта в общей сумме долга при подаче заявления; списать долги по алиментам нельзя.</li>
                    </ul>
                    <p>
                      Не включаются: неустойки, штрафы, пени и другие финансовые санкции,
                      начисленные до подачи заявления. Сумма фиксируется на дату подачи в МФЦ.
                    </p>
                  </details>
                </div>
                )}

                {!result && stage === 2 && (
                <div className="home-fields-grid">
                  <fieldset className="home-tristate home-field-wide">
                    <legend>Признавались ли вы банкротом ранее (через МФЦ или через суд)?</legend>
                    <div className="home-tristate-options">
                      {(['yes', 'no'] as const).map((option) => (
                        <label
                          className={`home-tristate-option${priorBankruptcy === option ? ' selected' : ''}`}
                          key={option}
                        >
                          <input
                            checked={priorBankruptcy === option}
                            name="priorBankruptcy"
                            onChange={() => {
                              setPriorBankruptcy(option);
                              setStageError(null);
                            }}
                            type="radio"
                            value={option}
                          />
                          <span>{option === 'yes' ? 'Да' : 'Нет'}</span>
                        </label>
                      ))}
                    </div>
                  </fieldset>

                  {priorBankruptcy === 'yes' && (
                    <label className="home-field home-field-wide">
                      <span>Когда та процедура завершилась или была прекращена?</span>
                      <input
                        onChange={(event: ChangeEvent<HTMLInputElement>) => {
                          setPriorEndedOn(event.target.value);
                          setStageError(null);
                        }}
                        type="date"
                        value={priorEndedOn}
                      />
                      <small>
                        Считается любая завершённая или прекращённая процедура — в том числе после
                        мирового соглашения, реструктуризации долгов или реализации имущества
                        (п. 8 ст. 223.2 Закона № 127-ФЗ).
                      </small>
                    </label>
                  )}
                </div>
                )}

                {!result && stage === 3 && (
                <div className="home-fields-grid">
                  <fieldset className="home-category-fieldset home-field-wide">
                    <legend>Какие из этих ситуаций к вам относятся?</legend>
                    <p className="home-category-hint">Можно выбрать несколько вариантов.</p>
                    <div className="home-category-list">
                      {CATEGORY_OPTIONS.map((option) => (
                        <label
                          className={`home-category-option${hasGround(option.value) ? ' selected' : ''}`}
                          key={option.value}
                        >
                          <input
                            checked={hasGround(option.value)}
                            name="statutoryGrounds"
                            onChange={() => toggleGround(option.value)}
                            type="checkbox"
                            value={option.value}
                          />
                          <span>
                            <strong>{option.title}</strong>
                            <small>{option.description}</small>
                          </span>
                        </label>
                      ))}
                    </div>
                  </fieldset>
                </div>
                )}

                {!result && stage === 4 && (
                <div className="home-fields-grid">
                  {hasGround('enforcement_ended') && (
                    <TriStateField
                      legend="Приставы уже пытались взыскать этот долг и закрыли дело, потому что не нашли ни вас, ни имущество, ни деньги на счетах? И новых дел о взыскании с тех пор не открывали?"
                      name="bailiffsClosed"
                      onChange={(value) => {
                        setBailiffsClosed(value);
                        setStageError(null);
                      }}
                      value={bailiffsClosed}
                      help={(
                        <details className="home-question-help">
                          <summary>Как это проверить</summary>
                          <p>
                            Зайдите на <strong>fssp.gov.ru</strong> → «Банк данных исполнительных
                            производств» → введите ФИО и дату рождения. Если дело было и его закрыли,
                            оно отобразится со статусом «окончено» и причиной окончания.
                          </p>
                        </details>
                      )}
                    />
                  )}

                  {hasGround('child_benefit') && (
                    <TriStateField
                      legend="Вам назначено именно единое пособие (через Социальный фонд, СФР)?"
                      name="benefitConfirmed"
                      onChange={(value) => {
                        setBenefitConfirmed(value);
                        setStageError(null);
                      }}
                      value={benefitConfirmed}
                      help={(
                        <details className="home-question-help">
                          <summary>Как это проверить</summary>
                          <p>
                            Посмотрите в личном кабинете на <strong>Госуслугах</strong> (раздел
                            «Выплаты» → «Единое пособие») или в кабинете СФР на <strong>sfr.gov.ru</strong>.
                          </p>
                        </details>
                      )}
                    />
                  )}

                  {showsSharedBlock && (
                    <>
                      <TriStateField
                        legend="Есть документ о долге (решение или приказ суда), по которому взыскание — приставами, банком или из зарплаты — началось год назад или раньше? И долг до сих пор не погашен полностью?"
                        name="writOverYear"
                        onChange={(value) => {
                          setWritOverYear(value);
                          setStageError(null);
                        }}
                        value={writOverYear}
                        help={(
                          <details className="home-question-help">
                            <summary>Что это за документ и где искать дату</summary>
                            <p>
                              Обычно это <strong>исполнительный лист</strong> или{' '}
                              <strong>судебный приказ</strong>: суд решил, что вы должны, и документ
                              передали приставам, в банк или работодателю — и с вас начали удерживать
                              деньги.
                            </p>
                            <p>
                              Дату смотрите на <strong>fssp.gov.ru</strong> (когда пристав возбудил
                              производство). Если там пусто — проверьте, не списывал ли деньги банк
                              (выписка по счёту) и не удерживала ли бухгалтерия на работе.
                            </p>
                          </details>
                        )}
                      />
                      <TriStateField
                        legend="Есть ли имущество, которое можно продать в счёт долга (недвижимость кроме единственного жилья, автомобиль, вклад и т. п.)?"
                        name="sellableProperty"
                        onChange={(value) => {
                          setSellableProperty(value);
                          setStageError(null);
                        }}
                        value={sellableProperty}
                      />
                    </>
                  )}

                  {hasGround('long_enforcement') && (
                    <TriStateField
                      legend="Документ о долге (решение или приказ суда) выдан 7 лет назад или раньше и уже передавался на взыскание — приставам, в банк или работодателю? Долг не погашен полностью?"
                      name="writOverSevenYears"
                      onChange={(value) => {
                        setWritOverSevenYears(value);
                        setStageError(null);
                      }}
                      value={writOverSevenYears}
                      help={(
                        <details className="home-question-help">
                          <summary>Где посмотреть дату</summary>
                          <p>
                            Смотрите дату в постановлении пристава о возбуждении производства
                            (не дату самого решения суда) или в выписке из банка, если деньги
                            списывали напрямую.
                          </p>
                        </details>
                      )}
                    />
                  )}
                </div>
                )}

                {!result && stageError && (
                  <div className="home-alert home-alert-error" role="alert">{stageError}</div>
                )}

                {!result && (
                <div className="home-form-footer">
                  {stage > 1 && (
                    <button
                      className="home-button home-button-secondary"
                      onClick={() => {
                        setStage((current) => (current > 1 ? (current - 1) as 1 | 2 | 3 : current));
                        setStageError(null);
                      }}
                      type="button"
                    >
                      Назад
                    </button>
                  )}
                  {stage === 1 && (
                    <button className="home-button" disabled={busy} onClick={continueFromDebt} type="button">
                      {busy ? 'Проверяем…' : 'Продолжить'} {!busy && <ArrowIcon />}
                    </button>
                  )}
                  {stage === 2 && (
                    <button className="home-button" disabled={busy} onClick={continueFromPriorBankruptcy} type="button">
                      {busy ? 'Проверяем…' : 'Продолжить'} {!busy && <ArrowIcon />}
                    </button>
                  )}
                  {stage === 3 && (
                    <button className="home-button" disabled={busy} onClick={continueFromGrounds} type="button">
                      {busy ? 'Проверяем…' : 'Продолжить'} {!busy && <ArrowIcon />}
                    </button>
                  )}
                  {stage === 4 && (
                    <button className="home-button" disabled={busy} type="submit">
                      {busy ? 'Проверяем…' : 'Проверить условия МФЦ'}
                      {!busy && <ArrowIcon />}
                    </button>
                  )}
                  <p>
                    <Icon size={18}>
                      <path d="M7 10V8a5 5 0 0 1 10 0v2M6 10h12v10H6V10Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
                    </Icon>
                    Ответы сохраняются обезличенно — без имени и контактов
                  </p>
                </div>
                )}

                {error && <div className="home-alert home-alert-error" role="alert">{error}</div>}

                {result && verdictText && (
                  <section className={`home-result home-result-${verdictText.tone}`} aria-live="polite">
                    <div className="home-result-icon">
                      <Icon size={26}>
                        {verdictText.tone === 'positive' ? (
                          <path d="m5 12 4 4L19 6" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.2" />
                        ) : (
                          <>
                            <path d="M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z" stroke="currentColor" strokeWidth="1.8" />
                            <path d="M12 8v5m0 3v.1" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
                          </>
                        )}
                      </Icon>
                    </div>
                    <div>
                      <span className="home-result-label">Предварительный результат</span>
                      <h3>{verdictText.title}</h3>
                      <p>{verdictText.body}</p>

                      {result.qualifyingGrounds.length > 0 && (
                        <ul className="home-result-grounds">
                          {result.qualifyingGrounds.map((ground) => (
                            <li key={ground.code}>
                              {ground.message}
                              {ground.legalBasis && (
                                <small className="home-result-basis">{ground.legalBasis}</small>
                              )}
                            </li>
                          ))}
                        </ul>
                      )}

                      {result.messages.length > 0 && (
                        <ul>
                          {result.messages.map((message) => <li key={message}>{message}</li>)}
                        </ul>
                      )}

                      {result.citations.length > 0 && (
                        <p className="home-result-citations">
                          Правовое основание: {result.citations.join('; ')}.
                        </p>
                      )}

                      <p className="home-result-disclaimer">
                        Результат не имеет юридической силы и не подтверждает право
                        на процедуру. Он не проверяет все основания и официальные
                        реестры (набор правил {result.rulesetVersion}).
                      </p>
                      {result.verdict === 'MFC_ELIGIBLE' && (
                        <Link className="home-result-link" to="/register">
                          Продолжить подготовку заявления <ArrowIcon />
                        </Link>
                      )}
                      <button
                        className="home-button home-button-secondary home-result-reset"
                        onClick={resetFlow}
                        type="button"
                      >
                        Пройти проверку заново
                      </button>
                    </div>
                  </section>
                )}
              </form>

              <aside className="home-check-aside">
                <span className="home-aside-badge">Только внесудебная процедура</span>
                <h3>Что будет в первом MVP</h3>
                <ul>
                  <li><span><CheckIcon /></span><p><strong>Проверка базовых условий</strong>Сумма долга и обстоятельства для дальнейшей проверки.</p></li>
                  <li><span><CheckIcon /></span><p><strong>Пошаговая анкета</strong>Сведения собираются небольшими понятными блоками.</p></li>
                  <li><span><CheckIcon /></span><p><strong>Список кредиторов</strong>Подготовка данных, которые важно указать в заявлении.</p></li>
                  <li><span><CheckIcon /></span><p><strong>Чек-лист для МФЦ</strong>Что проверить и взять с собой перед подачей.</p></li>
                </ul>
                <div className="home-aside-source">
                  <Icon size={22}>
                    <path d="M4 20h16M6 17V9m4 8V9m4 8V9m4 8V9M3 7l9-4 9 4H3Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.7" />
                  </Icon>
                  Информационная основа — официальные материалы Госуслуг и положения закона о внесудебном банкротстве.
                </div>
              </aside>
            </div>
          </div>
        </section>

        <section className="home-routes-section" id="conditions">
          <div className="home-container">
            <div className="home-section-heading home-section-heading-centered">
              <span className="home-kicker">Основные условия</span>
              <h2>Что нужно проверить перед обращением в МФЦ</h2>
              <p>
                Диапазон долга — только первое условие. Для подачи также нужно
                соответствовать одному из предусмотренных законом оснований.
              </p>
            </div>

            <div className="home-condition-grid">
              <article className="home-condition-card">
                <span className="home-condition-number">01</span>
                <div className="home-condition-icon"><Icon><path d="M5 7h14v12H5V7Zm3-3h8v3M8 11h8m-8 4h5" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" /></Icon></div>
                <h3>Сумма обязательств</h3>
                <p>Общий размер учитываемых долгов — от 25 000 до 1 000 000 ₽.</p>
              </article>
              <article className="home-condition-card">
                <span className="home-condition-number">02</span>
                <div className="home-condition-icon"><Icon><path d="M4 6h16v12H4V6Zm3 3h10M7 13h6" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" /></Icon></div>
                <h3>Законное основание</h3>
                <p>Нужно соответствовать хотя бы одному основанию, связанному с исполнительным документом и вашей ситуацией.</p>
              </article>
              <article className="home-condition-card">
                <span className="home-condition-number">03</span>
                <div className="home-condition-icon"><Icon><path d="M7 3h7l4 4v14H7V3Zm7 0v5h5M10 12h5m-5 4h5" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" /></Icon></div>
                <h3>Полный список кредиторов</h3>
                <p>В заявлении важно правильно указать кредиторов и суммы: неуказанные обязательства не прекращаются.</p>
              </article>
              <article className="home-condition-card">
                <span className="home-condition-number">04</span>
                <div className="home-condition-icon"><Icon><path d="M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Zm0-13v5l3 2" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" /></Icon></div>
                <h3>Готовность к 6 месяцам</h3>
                <p>После внесения сведений в реестр процедура обычно продолжается шесть месяцев.</p>
              </article>
            </div>

            <div className="home-mfc-summary">
              <div>
                <span className="home-route-tag">Через МФЦ</span>
                <h3>Подача бесплатна</h3>
                <p>Заявление подаётся в МФЦ по месту жительства или месту пребывания.</p>
              </div>
              <a
                className="home-source-link"
                href="https://www.gosuslugi.ru/life/details/bankruptcy_of_individuals"
                rel="noreferrer"
                target="_blank"
              >
                Открыть официальную инструкцию <ArrowIcon />
              </a>
            </div>
          </div>
        </section>

        <section className="home-process-section" id="process">
          <div className="home-container">
            <div className="home-section-heading home-section-heading-centered">
              <span className="home-kicker">Один понятный сценарий</span>
              <h2>От проверки до визита в МФЦ</h2>
              <p>Первый MVP ведёт только по подготовке к внесудебному банкротству.</p>
            </div>

            <ol className="home-process-list">
              <li>
                <span className="home-process-number">01</span>
                <div className="home-process-icon"><Icon><path d="M5 5h14v14H5V5Zm3 4h8M8 13h5" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" /></Icon></div>
                <h3>Проверьте условия</h3>
                <p>Уточните сумму долга и основание, которое позволяет обратиться во внесудебном порядке.</p>
              </li>
              <li>
                <span className="home-process-number">02</span>
                <div className="home-process-icon"><Icon><path d="M7 3h7l4 4v14H7V3Zm7 0v5h5M10 12h5m-5 4h5" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" /></Icon></div>
                <h3>Подготовьте сведения</h3>
                <p>Соберите персональные данные, список кредиторов, суммы и необходимые подтверждения.</p>
              </li>
              <li>
                <span className="home-process-number">03</span>
                <div className="home-process-icon"><Icon><path d="M4 20h16M6 17V9m4 8V9m4 8V9m4 8V9M3 7l9-4 9 4H3Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.7" /></Icon></div>
                <h3>Подайте заявление</h3>
                <p>Обратитесь в подходящий МФЦ с подготовленным заявлением и требуемыми документами.</p>
              </li>
            </ol>
          </div>
        </section>

        <section className="home-consequences-section" id="important">
          <div className="home-container home-consequences-grid">
            <div>
              <span className="home-kicker">Важно до подачи</span>
              <h2>Укажите все долги и учитывайте последствия</h2>
              <p className="home-consequences-lead">
                Внесудебное банкротство может освободить от части обязательств,
                но действует не на все требования и накладывает ограничения.
              </p>
              <a
                className="home-source-link"
                href="https://www.gosuslugi.ru/life/details/bankruptcy_of_individuals"
                rel="noreferrer"
                target="_blank"
              >
                Проверить информацию на Госуслугах <ArrowIcon />
              </a>
            </div>

            <div className="home-fact-list">
              <article>
                <span className="home-fact-icon home-fact-icon-warn"><Icon><path d="M12 4 3 20h18L12 4Zm0 6v4m0 3v.1" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" /></Icon></span>
                <div><h3>Неуказанные кредиторы сохраняют требования</h3><p>Освобождение не распространяется на обязательства перед кредиторами, которых нет в заявлении.</p></div>
              </article>
              <article>
                <span className="home-fact-icon"><Icon><path d="M4 12a8 8 0 1 0 3-6.2M4 4v5h5M12 8v5l3 2" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" /></Icon></span>
                <div><h3>Во время процедуры нельзя брать новые кредиты</h3><p>На срок внесудебного банкротства нельзя получать займы, кредиты и выдавать поручительства.</p></div>
              </article>
              <article>
                <span className="home-fact-icon"><Icon><path d="M5 20V8l7-4 7 4v12M9 20v-6h6v6M3 20h18" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" /></Icon></span>
                <div><h3>Не все обязательства прекращаются</h3><p>Например, сохраняются алименты и отдельные требования, связанные с личностью кредитора.</p></div>
              </article>
            </div>
          </div>
        </section>

        <section className="home-faq-section">
          <div className="home-container home-faq-grid">
            <div className="home-section-heading">
              <span className="home-kicker">Коротко о главном</span>
              <h2>Частые вопросы о МФЦ</h2>
              <p>Ответы дают общее представление и не заменяют проверку вашей конкретной ситуации.</p>
            </div>
            <div className="home-faq-list">
              <details>
                <summary>Достаточно ли долга до 1 000 000 ₽?</summary>
                <p>Нет. Диапазон суммы — только одно условие. Нужно также соответствовать одному из специальных оснований, предусмотренных законом.</p>
              </details>
              <details>
                <summary>Нужно ли платить за подачу в МФЦ?</summary>
                <p>Нет. Процедура внесудебного банкротства для гражданина бесплатна.</p>
              </details>
              <details>
                <summary>Сервис гарантирует принятие заявления?</summary>
                <p>Нет. Адиафора помогает проверить сведения и подготовиться, но решение зависит от официальной процедуры и данных государственных систем.</p>
              </details>
            </div>
          </div>
        </section>

        <section className="home-final-cta">
          <div className="home-container home-final-card">
            <div>
              <span className="home-kicker home-kicker-light">Начните с проверки</span>
              <h2>Подготовьтесь к обращению в МФЦ без хаоса</h2>
              <p>Создайте аккаунт, заполните анкету и соберите сведения в одном месте.</p>
            </div>
            <Link className="home-button home-button-light" to="/register">
              Начать подготовку <ArrowIcon />
            </Link>
          </div>
        </section>
      </main>

      <footer className="home-footer">
        <div className="home-container home-footer-inner">
          <div className="home-brand home-brand-footer">
            <span className="home-brand-mark" aria-hidden="true">
              <svg fill="none" viewBox="0 0 38 38">
                <path d="M11 27.5 19 8l8 19.5" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" />
                <path d="M14.2 21h9.6" stroke="currentColor" strokeLinecap="round" strokeWidth="3" />
              </svg>
            </span>
            <span><strong>Адиафора</strong><small>Внесудебное банкротство через МФЦ</small></span>
          </div>
          <p>Информационный сервис для подготовки к внесудебной процедуре. Не является юридической консультацией и не гарантирует принятие заявления.</p>
          <div className="home-footer-links">
            <Link to="/login">Войти</Link>
            <Link to="/register">Регистрация</Link>
            <a href="https://www.gosuslugi.ru/life/details/bankruptcy_of_individuals" rel="noreferrer" target="_blank">Официальный источник</a>
          </div>
        </div>
      </footer>
    </div>
  );
}
