import { useState, type ChangeEvent, type FormEvent, type ReactNode } from 'react';
import { Link } from 'react-router-dom';

import { ApiError } from '../api/client';
import { eligibilityApi } from '../api/endpoints';
import type {
  EligibilityEstimateRequest,
  EligibilityEstimateResponse,
} from '../api/types';
import './HomePage.css';

type VerdictCopy = {
  title: string;
  body: string;
  tone: 'positive' | 'attention' | 'neutral';
};

const VERDICT_TEXT: Record<EligibilityEstimateResponse['verdict'], VerdictCopy> = {
  MFC_ELIGIBLE: {
    title: 'Есть основания продолжить проверку для подачи через МФЦ',
    body: 'По указанным ответам внесудебное банкротство может вам подойти. Следующий шаг — проверить исполнительные производства, полный список долгов и другие обязательные условия.',
    tone: 'positive',
  },
  AMOUNT_OUT_OF_RANGE: {
    title: 'Условие по сумме долга для МФЦ не выполнено',
    body: 'Для внесудебного банкротства общая сумма учитываемых обязательств должна быть от 25 000 до 1 000 000 ₽. Этот MVP работает только с процедурой через МФЦ.',
    tone: 'attention',
  },
  MANUAL_REVIEW: {
    title: 'Нужна дополнительная проверка условий МФЦ',
    body: 'По этим ответам нельзя уверенно подтвердить возможность подачи. Продолжите анкету, чтобы проверить исполнительные производства, имущество и обязательные сведения.',
    tone: 'attention',
  },
  NEEDS_INFORMATION: {
    title: 'Недостаточно данных для предварительной проверки',
    body: 'Заполните известные вам поля. Итоговая возможность подачи через МФЦ всё равно подтверждается по официальным сведениям и документам.',
    tone: 'neutral',
  },
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

export default function HomePage() {
  const [debt, setDebt] = useState('');
  const [income, setIncome] = useState('');
  const [mortgage, setMortgage] = useState('');
  const [priorBankruptcy, setPriorBankruptcy] = useState('');
  const [propertyTx, setPropertyTx] = useState('');
  const [statutoryGround, setStatutoryGround] = useState('');
  const [result, setResult] = useState<EligibilityEstimateResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setBusy(true);
    setError(null);

    try {
      const estimate = await eligibilityApi.estimate({
        totalDebtAmount: debt === '' ? null : Number(debt),
        hasRegularIncome: income === '' ? null : income === 'yes',
        ownsMortgagedHome: mortgage === '' ? null : mortgage === 'yes',
        previousBankruptcy:
          priorBankruptcy === '' ? null : priorBankruptcy === 'yes',
        recentPropertyTransaction:
          propertyTx === ''
            ? null
            : (propertyTx as 'none' | 'sold' | 'gifted'),
        mfcStatutoryGround:
          statutoryGround === ''
            ? null
            : (statutoryGround as NonNullable<
                EligibilityEstimateRequest['mfcStatutoryGround']
              >),
      });
      setResult(estimate);
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
              <form className="home-check-card" onSubmit={submit}>
                <div className="home-form-intro">
                  <span className="home-form-number">01</span>
                  <div>
                    <h3>Расскажите о своей ситуации</h3>
                    <p>Можно пропустить поле, если пока не знаете точный ответ.</p>
                  </div>
                </div>

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
                      <li>займы и кредиты, включая проценты по ним — точную сумму долга можно запросить у кредитора;</li>
                      <li>налоги и сборы — задолженность можно проверить на сайте ФНС или на Госуслугах;</li>
                      <li>штрафы — автоштрафы отображаются на Госуслугах и сайте ГИБДД;</li>
                      <li>платежи по договорам поручительства, включая суммы, по которым нет просрочки платежей;</li>
                      <li>судебная задолженность — её можно проверить в личном кабинете на Госуслугах или на сайте ФССП;</li>
                      <li>алименты — их запросят для учёта в общей сумме долга при подаче заявления; списать долги по алиментам нельзя.</li>
                    </ul>
                  </details>

                  <label className="home-field">
                    <span>Есть ли у вас регулярный доход?</span>
                    <select value={income} onChange={(event: ChangeEvent<HTMLSelectElement>) => setIncome(event.target.value)}>
                      <option value="">— выберите —</option>
                      <option value="yes">Да</option>
                      <option value="no">Нет</option>
                    </select>
                  </label>

                  <label className="home-field">
                    <span>Есть ли у вас жильё в ипотеке?</span>
                    <select value={mortgage} onChange={(event: ChangeEvent<HTMLSelectElement>) => setMortgage(event.target.value)}>
                      <option value="">— выберите —</option>
                      <option value="yes">Да</option>
                      <option value="no">Нет</option>
                    </select>
                  </label>

                  <label className="home-field">
                    <span>Признавались ли вы банкротом ранее?</span>
                    <select value={priorBankruptcy} onChange={(event: ChangeEvent<HTMLSelectElement>) => setPriorBankruptcy(event.target.value)}>
                      <option value="">— выберите —</option>
                      <option value="yes">Да</option>
                      <option value="no">Нет</option>
                    </select>
                  </label>

                  <label className="home-field">
                    <span>Продавали или дарили имущество за последние 3 года?</span>
                    <select value={propertyTx} onChange={(event: ChangeEvent<HTMLSelectElement>) => setPropertyTx(event.target.value)}>
                      <option value="">— выберите —</option>
                      <option value="none">Нет</option>
                      <option value="sold">Да — продавал(а)</option>
                      <option value="gifted">Да — дарил(а)</option>
                    </select>
                  </label>

                  <label className="home-field home-field-wide">
                    <span>Подходите ли вы под одну из категорий для внесудебного банкротства?</span>
                    <select
                      value={statutoryGround}
                      onChange={(event: ChangeEvent<HTMLSelectElement>) => setStatutoryGround(event.target.value)}
                    >
                      <option value="">— выберите —</option>
                      <option value="enforcement_ended">
                        Исполнительное производство окончено: имущества для взыскания нет, документ вернули взыскателю
                      </option>
                      <option value="pensioner">
                        Я пенсионер, пенсия — основной доход, есть неисполненный исполнительный документ, имущества нет
                      </option>
                      <option value="child_benefit">
                        Получаю пособие в связи с рождением и воспитанием ребёнка, есть неисполненный исполнительный документ, имущества нет
                      </option>
                      <option value="long_enforcement">
                        Долг взыскивается с меня уже 7 лет и более, но документ полностью не исполнен
                      </option>
                      <option value="none">Ни одна из ситуаций не относится ко мне</option>
                    </select>
                    <small>Для внесудебного банкротства нужно подходить хотя бы под одну из категорий</small>
                  </label>
                </div>

                <div className="home-form-footer">
                  <button className="home-button" disabled={busy} type="submit">
                    {busy ? 'Проверяем…' : 'Проверить условия МФЦ'}
                    {!busy && <ArrowIcon />}
                  </button>
                  <p>
                    <Icon size={18}>
                      <path d="M7 10V8a5 5 0 0 1 10 0v2M6 10h12v10H6V10Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
                    </Icon>
                    Данные быстрой формы не сохраняются
                  </p>
                </div>

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

                      {result.messages.length > 0 && (
                        <ul>
                          {result.messages.map((message) => <li key={message}>{message}</li>)}
                        </ul>
                      )}

                      <p className="home-result-disclaimer">
                        Результат не имеет юридической силы и не подтверждает право
                        на процедуру. Он не проверяет все основания и официальные
                        реестры (набор правил {result.rulesetVersion}).
                      </p>
                      <Link className="home-result-link" to="/register">
                        Продолжить проверку условий МФЦ <ArrowIcon />
                      </Link>
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
