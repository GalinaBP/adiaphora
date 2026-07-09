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

// Step 1 holds only the two gating questions (debt amount + statutory category); the
// remaining questions live on step 2 behind the "Продолжить" button.
async function fillAndSubmit() {
  await userEvent.type(screen.getByLabelText(/Общая сумма долгов/), '350000');
  await userEvent.click(screen.getByRole('radio', { name: /Обычный должник/ }));
  await userEvent.click(screen.getByRole('button', { name: /Продолжить/ }));
  await userEvent.selectOptions(screen.getByLabelText(/регулярный доход/), 'yes');
  await userEvent.selectOptions(screen.getByLabelText(/жильё в ипотеке/), 'no');
  await userEvent.selectOptions(screen.getByLabelText(/банкротом ранее/), 'no');
  await userEvent.selectOptions(screen.getByLabelText(/Продавали или дарили имущество/), 'none');
  await userEvent.click(screen.getByRole('button', { name: /Проверить условия МФЦ/ }));
}

async function skipToStepTwo() {
  await userEvent.click(screen.getByRole('button', { name: /Продолжить/ }));
}

describe('HomePage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('submits the two-step answers and shows an eligible estimate with disclaimer and CTA', async () => {
    estimateMock.mockResolvedValue(response());
    renderPage();

    await fillAndSubmit();

    expect(estimateMock).toHaveBeenCalledWith({
      totalDebtAmount: 350000,
      hasRegularIncome: true,
      ownsMortgagedHome: false,
      previousBankruptcy: false,
      recentPropertyTransaction: 'none',
      mfcStatutoryGround: 'enforcement_ended',
    });
    expect(
      await screen.findByText(/Есть основания продолжить проверку для подачи через МФЦ/i),
    ).toBeInTheDocument();
    expect(screen.getByText(/не имеет юридической силы/i)).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: /Продолжить проверку условий МФЦ/i }),
    ).toHaveAttribute('href', '/register');
  });

  it('keeps step-1 answers when navigating back from step 2', async () => {
    renderPage();

    await userEvent.click(screen.getByRole('radio', { name: /Пенсионер/ }));
    await userEvent.click(screen.getByRole('button', { name: /Продолжить/ }));
    expect(screen.getByLabelText(/регулярный доход/)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /Назад/ }));
    expect(screen.getByRole('radio', { name: /Пенсионер/ })).toBeChecked();
  });

  it('shows the out-of-range verdict', async () => {
    estimateMock.mockResolvedValue(
      response({ verdict: 'AMOUNT_OUT_OF_RANGE', route: 'COURT_PRELIMINARY' }),
    );
    renderPage();

    await fillAndSubmit();

    expect(
      await screen.findByText(/Условие по сумме долга для МФЦ не выполнено/i),
    ).toBeInTheDocument();
  });

  it('shows manual-review verdict with the rule messages', async () => {
    estimateMock.mockResolvedValue(
      response({
        verdict: 'MANUAL_REVIEW',
        route: 'MANUAL_REVIEW',
        messages: ['Ипотечное жильё требует проверки юристом.'],
      }),
    );
    renderPage();

    await fillAndSubmit();

    expect(
      await screen.findByText(/Нужна дополнительная проверка условий МФЦ/i),
    ).toBeInTheDocument();
    expect(
      screen.getByText('Ипотечное жильё требует проверки юристом.'),
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

    await skipToStepTwo();
    await userEvent.click(screen.getByRole('button', { name: /Проверить условия МФЦ/ }));

    expect(estimateMock).toHaveBeenCalledWith({
      totalDebtAmount: null,
      hasRegularIncome: null,
      ownsMortgagedHome: null,
      previousBankruptcy: null,
      recentPropertyTransaction: null,
      mfcStatutoryGround: null,
    });
    expect(await screen.findByText(/Недостаточно данных/i)).toBeInTheDocument();
  });

  it('shows an error banner when the API is unavailable', async () => {
    estimateMock.mockRejectedValue(new Error('down'));
    renderPage();

    await skipToStepTwo();
    await userEvent.click(screen.getByRole('button', { name: /Проверить условия МФЦ/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/недоступна/i);
  });

  it('links to the official Gosuslugi source', () => {
    renderPage();

    expect(
      screen.getByText(/Что нужно проверить перед обращением в МФЦ/),
    ).toBeInTheDocument();
    const source = screen.getByRole('link', { name: /Открыть официальную инструкцию/i });
    expect(source).toHaveAttribute(
      'href',
      'https://www.gosuslugi.ru/life/details/bankruptcy_of_individuals',
    );
  });
});
