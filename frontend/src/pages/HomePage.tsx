import { useState } from 'react';
import { Link } from 'react-router-dom';
import { eligibilityApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import type { EligibilityEstimateResponse } from '../api/types';

const VERDICT_TEXT: Record<string, { title: string; body: string }> = {
  MFC_ELIGIBLE: {
    title: 'You may qualify for out-of-court (MFC) bankruptcy',
    body: 'Based on your answers, your situation fits the extrajudicial procedure: debt within the 25,000–1,000,000 RUB band and no answers that require a lawyer’s review. Register to start the full application.',
  },
  AMOUNT_OUT_OF_RANGE: {
    title: 'The out-of-court (MFC) route does not fit this debt amount',
    body: 'Extrajudicial bankruptcy applies to total debts between 25,000 and 1,000,000 RUB. Outside that band, the judicial route is the usual path — this service currently prepares out-of-court cases only.',
  },
  MANUAL_REVIEW: {
    title: 'Your case needs a specialist’s review',
    body: 'Some of your answers (for example a mortgaged home, a previous bankruptcy, or a recent property transaction) mean a lawyer should look at the details before a route can be suggested.',
  },
  NEEDS_INFORMATION: {
    title: 'Not enough information for an estimate',
    body: 'Answer the remaining questions to get an estimate.',
  },
};

// Public landing page: a short anonymous MFC-eligibility estimator over the same rule engine the
// real application uses, plus brief context sourced from Gosuslugi. Nothing entered here is stored.
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
      setError(err instanceof ApiError ? err.message : 'The estimate is unavailable right now.');
    } finally {
      setBusy(false);
    }
  };

  const verdictText = result ? VERDICT_TEXT[result.verdict] : null;

  return (
    <div className="home">
      <section>
        <h2>Check if out-of-court bankruptcy fits your situation</h2>
        <p className="muted">
          Free, anonymous, takes a minute. Nothing you enter here is saved. This first version
          covers the <strong>out-of-court (MFC) procedure only</strong>.
        </p>

        <form onSubmit={submit} className="home-form">
          <label htmlFor="est-debt">Total debt amount (RUB)</label>
          <input
            id="est-debt"
            type="number"
            min="0"
            step="0.01"
            value={debt}
            onChange={(e) => setDebt(e.target.value)}
            placeholder="e.g. 350000"
          />

          <label htmlFor="est-income">Do you have a regular income?</label>
          <select id="est-income" value={income} onChange={(e) => setIncome(e.target.value)}>
            <option value="">— select —</option>
            <option value="yes">Yes</option>
            <option value="no">No</option>
          </select>

          <label htmlFor="est-mortgage">Do you own a home under mortgage?</label>
          <select id="est-mortgage" value={mortgage} onChange={(e) => setMortgage(e.target.value)}>
            <option value="">— select —</option>
            <option value="yes">Yes</option>
            <option value="no">No</option>
          </select>

          <label htmlFor="est-prior">Have you been declared bankrupt before?</label>
          <select
            id="est-prior"
            value={priorBankruptcy}
            onChange={(e) => setPriorBankruptcy(e.target.value)}
          >
            <option value="">— select —</option>
            <option value="yes">Yes</option>
            <option value="no">No</option>
          </select>

          <label htmlFor="est-property">Property sold or gifted in the last 3 years?</label>
          <select
            id="est-property"
            value={propertyTx}
            onChange={(e) => setPropertyTx(e.target.value)}
          >
            <option value="">— select —</option>
            <option value="none">No</option>
            <option value="sold">Yes — sold property</option>
            <option value="gifted">Yes — gifted property</option>
          </select>

          <button type="submit" disabled={busy}>
            {busy ? 'Checking…' : 'Get my estimate'}
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
              <strong>This is a preliminary, non-binding estimate.</strong> The rules behind it are
              pending review by a lawyer and may change; only a specialist can confirm your options
              (ruleset {result.rulesetVersion}).
            </p>
            <p>
              <Link to="/register" className="home-cta">
                Create an account to start your application →
              </Link>
            </p>
          </div>
        )}
      </section>

      <section className="home-context">
        <h3>About personal bankruptcy in Russia</h3>
        <p>
          Personal bankruptcy is a legal procedure for a person who cannot repay their debts. The
          out-of-court (extrajudicial) variant runs through an MFC public-services centre, is free,
          and is available when total debts are between 25,000 and 1,000,000 RUB and enforcement
          proceedings have ended for lack of assets.
        </p>
        <p>
          Bankruptcy can write off consumer loans, utility arrears and tax debts — but{' '}
          <strong>not</strong> alimony, compensation for harm to life or health, or certain other
          personal obligations, which survive the procedure.
        </p>
        <p>
          It also has consequences: for 5 years after bankruptcy you must disclose it when applying
          for new loans, repeat bankruptcy is restricted, and for several years you may not manage a
          legal entity.
        </p>
        <p className="muted">
          Source:{' '}
          <a
            href="https://www.gosuslugi.ru/life/details/bankruptcy_of_individuals"
            target="_blank"
            rel="noopener noreferrer"
          >
            Gosuslugi — Bankruptcy of individuals
          </a>
          . Summary provided for orientation only and pending lawyer review; the official page has
          the current rules.
        </p>
      </section>
    </div>
  );
}
