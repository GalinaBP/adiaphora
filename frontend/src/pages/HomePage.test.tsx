import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import HomePage from './HomePage';
import { eligibilityApi } from '../api/endpoints';
import type { EligibilityEstimateResponse } from '../api/types';

vi.mock('../api/endpoints', () => ({
  eligibilityApi: {
    estimate: vi.fn(),
  },
}));

const estimateMock = vi.mocked(eligibilityApi.estimate);

function response(overrides: Partial<EligibilityEstimateResponse> = {}): EligibilityEstimateResponse {
  return {
    verdict: 'MFC_ELIGIBLE',
    route: 'MFC_PRELIMINARY',
    messages: [],
    missingInformation: [],
    rulesetVersion: 'ruleset-test',
    ...overrides,
  };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <HomePage />
    </MemoryRouter>,
  );
}

async function fillAndSubmit() {
  await userEvent.type(screen.getByLabelText(/Total debt amount/), '350000');
  await userEvent.selectOptions(screen.getByLabelText(/regular income/), 'yes');
  await userEvent.selectOptions(screen.getByLabelText(/home under mortgage/), 'no');
  await userEvent.selectOptions(screen.getByLabelText(/bankrupt before/), 'no');
  await userEvent.selectOptions(screen.getByLabelText(/Property sold or gifted/), 'none');
  await userEvent.click(screen.getByRole('button', { name: /Get my estimate/ }));
}

describe('HomePage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('submits the five inputs and shows an eligible estimate with disclaimer and CTA', async () => {
    estimateMock.mockResolvedValue(response());
    renderPage();

    await fillAndSubmit();

    expect(estimateMock).toHaveBeenCalledWith({
      totalDebtAmount: 350000,
      hasRegularIncome: true,
      ownsMortgagedHome: false,
      previousBankruptcy: false,
      recentPropertyTransaction: 'none',
    });
    expect(
      await screen.findByText(/may qualify for out-of-court \(MFC\) bankruptcy/i),
    ).toBeInTheDocument();
    expect(screen.getByText(/preliminary, non-binding estimate/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Create an account/i })).toHaveAttribute(
      'href',
      '/register',
    );
  });

  it('shows the out-of-range verdict', async () => {
    estimateMock.mockResolvedValue(
      response({ verdict: 'AMOUNT_OUT_OF_RANGE', route: 'COURT_PRELIMINARY' }),
    );
    renderPage();

    await fillAndSubmit();

    expect(
      await screen.findByText(/does not fit this debt amount/i),
    ).toBeInTheDocument();
  });

  it('shows manual-review verdict with the rule messages', async () => {
    estimateMock.mockResolvedValue(
      response({
        verdict: 'MANUAL_REVIEW',
        route: 'MANUAL_REVIEW',
        messages: ['A mortgaged home requires manual legal review.'],
      }),
    );
    renderPage();

    await fillAndSubmit();

    expect(await screen.findByText(/needs a specialist/i)).toBeInTheDocument();
    expect(
      screen.getByText('A mortgaged home requires manual legal review.'),
    ).toBeInTheDocument();
  });

  it('allows submitting with unanswered questions and shows needs-information', async () => {
    estimateMock.mockResolvedValue(
      response({
        verdict: 'NEEDS_INFORMATION',
        route: 'INSUFFICIENT_INFORMATION',
        missingInformation: ['APPLICATION-DEBT-AMOUNT-MISSING'],
      }),
    );
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: /Get my estimate/ }));

    expect(estimateMock).toHaveBeenCalledWith({
      totalDebtAmount: null,
      hasRegularIncome: null,
      ownsMortgagedHome: null,
      previousBankruptcy: null,
      recentPropertyTransaction: null,
    });
    expect(await screen.findByText(/Not enough information/i)).toBeInTheDocument();
  });

  it('shows an error banner when the API is unavailable', async () => {
    estimateMock.mockRejectedValue(new Error('down'));
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: /Get my estimate/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/unavailable/i);
  });

  it('shows the Gosuslugi-sourced context with citation', () => {
    renderPage();

    expect(screen.getByText(/About personal bankruptcy in Russia/)).toBeInTheDocument();
    const source = screen.getByRole('link', { name: /Gosuslugi/i });
    expect(source).toHaveAttribute(
      'href',
      'https://www.gosuslugi.ru/life/details/bankruptcy_of_individuals',
    );
  });
});
