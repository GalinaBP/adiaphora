import { useState } from 'react';
import { Link } from 'react-router-dom';
import { eligibilityApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import type { EligibilityEstimateResponse } from '../api/types';

const VERDICT_TEXT: Record<string, { title: string; body: string }> = {
  MFC_ELIGIBLE: {
    title: 'Вам может подойти внесудебное банкротство через МФЦ',
    body: 'Судя по ответам, ваша ситуация подходит под внесудебную процедуру: долг в пределах 25 000 – 1 000 000 ₽ и нет обстоятельств, требующих проверки юристом. Зарегистрируйтесь, чтобы начать оформление.',
  },
  AMOUNT_OUT_OF_RANGE: {
    title: 'Внесудебное банкротство (МФЦ) не подходит по сумме долга',
    body: 'Внесудебная процедура применяется при общей сумме долгов от 25 000 до 1 000 000 ₽. Вне этого диапазона обычно применяется судебная процедура — этот сервис пока готовит только внесудебные дела.',
  },
  MANUAL_REVIEW: {
    title: 'Вашу ситуацию должен посмотреть специалист',
    body: 'Некоторые ответы (например, жильё в ипотеке, предыдущее банкротство или недавняя сделка с имуществом) означают, что до выбора процедуры детали должен изучить юрист.',
  },
  NEEDS_INFORMATION: {
    title: 'Недостаточно данных для оценки',
    body: 'Ответьте на оставшиеся вопросы, чтобы получить оценку.',
  },
};

// Публичная главная страница: короткая анонимная оценка применимости внесудебного банкротства
// на том же движке правил, что и настоящая заявка, плюс краткая справка по данным Госуслуг.
// Введённые здесь данные никуда не сохраняются.
export default function HomePage() {
  const [debt, setDebt] = useState('');
  const [income, setIncome] = useState('');
  const [mortgage, setMortgage] = useState('');
  const [priorBankruptcy, setPriorBankruptcy] = useState('');
  const [propertyTx, setPropertyTx] = useState('');
  const [result, setResult] = useState<EligibilityEstimateResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      setResult(
        await eligibilityApi.estimate({
          totalDebtAmount: debt === '' ? null : Number(debt),
          hasRegularIncome: income === '' ? null : income === 'yes',
          ownsMortgagedHome: mortgage === '' ? null : mortgage === 'yes',
          previousBankruptcy: priorBankruptcy === '' ? null : priorBankruptcy === 'yes',
          recentPropertyTransaction:
            propertyTx === '' ? null : (propertyTx as 'none' | 'sold' | 'gifted'),
        }),
      );
    } catch (err) {
      setResult(null);
      setError(err instanceof ApiError ? err.message : 'Оценка сейчас недоступна. Попробуйте позже.');
    } finally {
      setBusy(false);
    }
  };

  const verdictText = result ? VERDICT_TEXT[result.verdict] : null;

  return (
    <div className="home">
      <section>
        <h2>Проверьте, подходит ли вам внесудебное банкротство</h2>
        <p className="muted">
          Бесплатно, анонимно, за минуту. Введённые данные никуда не сохраняются. Эта первая версия
          сервиса охватывает <strong>только внесудебную процедуру (через МФЦ)</strong>.
        </p>

        <form onSubmit={submit} className="home-form">
          <label htmlFor="est-debt">Общая сумма долгов (₽)</label>
          <input
            id="est-debt"
            type="number"
            min="0"
            step="0.01"
            value={debt}
            onChange={(e) => setDebt(e.target.value)}
            placeholder="например, 350000"
          />

          <label htmlFor="est-income">Есть ли у вас регулярный доход?</label>
          <select id="est-income" value={income} onChange={(e) => setIncome(e.target.value)}>
            <option value="">— выберите —</option>
            <option value="yes">Да</option>
            <option value="no">Нет</option>
          </select>

          <label htmlFor="est-mortgage">Есть ли у вас жильё в ипотеке?</label>
          <select id="est-mortgage" value={mortgage} onChange={(e) => setMortgage(e.target.value)}>
            <option value="">— выберите —</option>
            <option value="yes">Да</option>
            <option value="no">Нет</option>
          </select>

          <label htmlFor="est-prior">Признавались ли вы банкротом ранее?</label>
          <select
            id="est-prior"
            value={priorBankruptcy}
            onChange={(e) => setPriorBankruptcy(e.target.value)}
          >
            <option value="">— выберите —</option>
            <option value="yes">Да</option>
            <option value="no">Нет</option>
          </select>

          <label htmlFor="est-property">Продавали или дарили имущество за последние 3 года?</label>
          <select
            id="est-property"
            value={propertyTx}
            onChange={(e) => setPropertyTx(e.target.value)}
          >
            <option value="">— выберите —</option>
            <option value="none">Нет</option>
            <option value="sold">Да — продавал(а)</option>
            <option value="gifted">Да — дарил(а)</option>
          </select>

          <button type="submit" disabled={busy}>
            {busy ? 'Проверяем…' : 'Получить оценку'}
          </button>
        </form>

        {error && <p className="error" role="alert">{error}</p>}

        {result && verdictText && (
          <div className="home-result" role="status" data-verdict={result.verdict}>
            <h3>{verdictText.title}</h3>
            <p>{verdictText.body}</p>
            {result.messages.length > 0 && (
              <ul>
                {result.messages.map((m) => (
                  <li key={m}>{m}</li>
                ))}
              </ul>
            )}
            <p className="home-disclaimer">
              <strong>Это предварительная оценка, не имеющая юридической силы.</strong> Правила,
              на которых она основана, ожидают проверки юристом и могут измениться; подтвердить
              ваши возможности может только специалист (набор правил {result.rulesetVersion}).
            </p>
            <p>
              <Link to="/register" className="home-cta">
                Создать аккаунт и начать оформление →
              </Link>
            </p>
          </div>
        )}
      </section>

      <section className="home-context">
        <h3>О банкротстве физических лиц</h3>
        <p>
          Банкротство — законная процедура для человека, который не может расплатиться с долгами.
          Внесудебный вариант проходит через МФЦ, бесплатен и доступен, когда общая сумма долгов
          составляет от 25 000 до 1 000 000 ₽, а исполнительное производство окончено из-за
          отсутствия имущества.
        </p>
        <p>
          Банкротство может списать потребительские кредиты, долги за ЖКХ и налоги — но{' '}
          <strong>не списывает</strong> алименты, возмещение вреда жизни и здоровью и ряд других
          личных обязательств: они сохраняются после процедуры.
        </p>
        <p>
          У процедуры есть последствия: в течение 5 лет после банкротства нужно сообщать о нём при
          получении новых кредитов, повторное банкротство ограничено, а несколько лет нельзя
          управлять юридическим лицом.
        </p>
        <p className="muted">
          Источник:{' '}
          <a
            href="https://www.gosuslugi.ru/life/details/bankruptcy_of_individuals"
            target="_blank"
            rel="noopener noreferrer"
          >
            Госуслуги — Банкротство физических лиц
          </a>
          . Краткое изложение для ориентира, ожидает проверки юристом; актуальные правила — на
          официальной странице.
        </p>
      </section>
    </div>
  );
}
