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
    qualifyingGrounds: [],
    citations: [],
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

// The flow is staged with hard gates: 1 debt amount, 2 prior bankruptcy, 3 category
// multi-select, 4 per-category follow-up questions.
async function passDebtStage(debt = '350000') {
  await userEvent.type(screen.getByLabelText(/Общая сумма долгов/), debt);
  await userEvent.click(screen.getByRole('button', { name: /Продолжить/ }));
}

async function passPriorBankruptcyStage() {
  await userEvent.click(screen.getByRole('radio', { name: 'Нет' }));
  await userEvent.click(screen.getByRole('button', { name: /Продолжить/ }));
}

describe('HomePage eligibility flow', () => {
  beforeEach(() => vi.clearAllMocks());

  it('walks the four stages and shows qualifying grounds with legal citations', async () => {
    estimateMock.mockResolvedValue(response({
      qualifyingGrounds: [{
        code: 'MFC-GROUND-BAILIFFS-CLOSED',
        message: 'Основание подтверждено: пристав окончил исполнительное производство.',
        legalBasis: 'пп. 1 п. 1 ст. 223.2 Закона № 127-ФЗ',
      }],
      citations: ['пп. 1 п. 1 ст. 223.2 Закона № 127-ФЗ'],
    }));
    renderPage();

    await passDebtStage();
    await passPriorBankruptcyStage();
    await userEvent.click(screen.getByRole('checkbox', { name: /Приставы уже работали/ }));
    await userEvent.click(screen.getByRole('button', { name: /Продолжить/ }));
    await userEvent.click(screen.getByRole('radio', { name: 'Да' }));
    await userEvent.click(screen.getByRole('button', { name: /Проверить условия МФЦ/ }));

    expect(estimateMock).toHaveBeenCalledWith({
      totalDebtAmount: 350000,
      previousBankruptcy: false,
      previousBankruptcyEndedOn: null,
      mfcStatutoryGrounds: ['enforcement_ended'],
      bailiffsCaseClosedNoNew: 'yes',
      childBenefitConfirmed: null,
      writUnpaidOverOneYear: null,
      ownsSellableProperty: null,
      writIssuedOverSevenYears: null,
    });
    expect(await screen.findByText(/Вам доступно внесудебное банкротство/i)).toBeInTheDocument();
    expect(screen.getAllByText(/пп\. 1 п\. 1 ст\. 223\.2/).length).toBeGreaterThan(0);
    expect(screen.getByText(/не имеет юридической силы/i)).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: /Продолжить подготовку заявления/i }),
    ).toHaveAttribute('href', '/register');
  });

  it('hard-gates an out-of-range amount without showing later stages', async () => {
    estimateMock.mockResolvedValue(
      response({ verdict: 'AMOUNT_OUT_OF_RANGE', route: 'NOT_CURRENTLY_RECOMMENDED' }),
    );
    renderPage();

    await passDebtStage('10000');

    expect(estimateMock).toHaveBeenCalledWith(expect.objectContaining({
      totalDebtAmount: 10000,
      previousBankruptcy: null,
      mfcStatutoryGrounds: null,
    }));
    expect(
      await screen.findByText(/сумма долга вне диапазона/i),
    ).toBeInTheDocument();
    expect(screen.queryByText(/Признавались ли вы банкротом/)).not.toBeInTheDocument();
  });

  it('hard-gates a prior bankruptcy that ended less than 5 years ago', async () => {
    estimateMock.mockResolvedValue(
      response({ verdict: 'JUDICIAL_ROUTE', route: 'COURT_PRELIMINARY' }),
    );
    renderPage();

    await passDebtStage();
    await userEvent.click(screen.getByRole('radio', { name: 'Да' }));
    const recentDate = new Date();
    recentDate.setFullYear(recentDate.getFullYear() - 1);
    const isoDate = recentDate.toISOString().slice(0, 10);
    await userEvent.type(screen.getByLabelText(/Когда та процедура завершилась/), isoDate);
    await userEvent.click(screen.getByRole('button', { name: /Продолжить/ }));

    expect(estimateMock).toHaveBeenCalledWith(expect.objectContaining({
      previousBankruptcy: true,
      previousBankruptcyEndedOn: isoDate,
    }));
    expect(
      await screen.findByText(/рассмотрите судебную процедуру/i),
    ).toBeInTheDocument();
    expect(screen.queryByText(/Какие из этих ситуаций/)).not.toBeInTheDocument();
  });

  it('treats "none" as exclusive and routes it to the judicial result', async () => {
    estimateMock.mockResolvedValue(
      response({ verdict: 'JUDICIAL_ROUTE', route: 'COURT_PRELIMINARY' }),
    );
    renderPage();

    await passDebtStage();
    await passPriorBankruptcyStage();
    await userEvent.click(screen.getByRole('checkbox', { name: /Пенсионер/ }));
    await userEvent.click(screen.getByRole('checkbox', { name: /Ни одна ситуация не подходит/ }));
    expect(screen.getByRole('checkbox', { name: /Пенсионер/ })).not.toBeChecked();
    await userEvent.click(screen.getByRole('button', { name: /Продолжить/ }));

    expect(estimateMock).toHaveBeenCalledWith(expect.objectContaining({
      mfcStatutoryGrounds: ['none'],
    }));
    expect(
      await screen.findByText(/рассмотрите судебную процедуру/i),
    ).toBeInTheDocument();
  });

  it('shows one shared block for pensioner and the separate old-debt block without a property question', async () => {
    estimateMock.mockResolvedValue(response());
    renderPage();

    await passDebtStage();
    await passPriorBankruptcyStage();
    await userEvent.click(screen.getByRole('checkbox', { name: /Пенсионер/ }));
    await userEvent.click(screen.getByRole('checkbox', { name: /Долг взыскивают уже 7 лет/ }));
    await userEvent.click(screen.getByRole('button', { name: /Продолжить/ }));

    // Shared block (writ + property) + old-debt block = exactly three questions.
    expect(screen.queryByText(/едином пособии|единое пособие \(через Социальный фонд/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Приставы уже пытались взыскать/)).not.toBeInTheDocument();
    const yesRadios = screen.getAllByRole('radio', { name: 'Да' });
    expect(yesRadios).toHaveLength(3);

    await userEvent.click(yesRadios[0]); // writ presented over a year ago
    const noRadios = screen.getAllByRole('radio', { name: 'Нет' });
    await userEvent.click(noRadios[1]); // no sellable property
    await userEvent.click(noRadios[2]); // writ not older than 7 years
    await userEvent.click(screen.getByRole('button', { name: /Проверить условия МФЦ/ }));

    expect(estimateMock).toHaveBeenCalledWith(expect.objectContaining({
      mfcStatutoryGrounds: ['pensioner', 'long_enforcement'],
      writUnpaidOverOneYear: 'yes',
      ownsSellableProperty: 'no',
      writIssuedOverSevenYears: 'no',
      bailiffsCaseClosedNoNew: null,
      childBenefitConfirmed: null,
    }));
  });

  it('shows the check-your-documents result for a not-sure answer', async () => {
    estimateMock.mockResolvedValue(response({
      verdict: 'MANUAL_REVIEW',
      route: 'MANUAL_REVIEW',
      messages: ['Проверьте статус исполнительных производств: fssp.gov.ru.'],
    }));
    renderPage();

    await passDebtStage();
    await passPriorBankruptcyStage();
    await userEvent.click(screen.getByRole('checkbox', { name: /Приставы уже работали/ }));
    await userEvent.click(screen.getByRole('button', { name: /Продолжить/ }));
    await userEvent.click(screen.getByRole('radio', { name: /Не уверен/ }));
    await userEvent.click(screen.getByRole('button', { name: /Проверить условия МФЦ/ }));

    expect(await screen.findByText(/Проверьте документы/i)).toBeInTheDocument();
    expect(screen.getByText(/fssp\.gov\.ru/)).toBeInTheDocument();
  });

  it('does not submit stage 4 while a follow-up question is unanswered', async () => {
    renderPage();

    await passDebtStage();
    await passPriorBankruptcyStage();
    await userEvent.click(screen.getByRole('checkbox', { name: /Приставы уже работали/ }));
    await userEvent.click(screen.getByRole('button', { name: /Продолжить/ }));
    await userEvent.click(screen.getByRole('button', { name: /Проверить условия МФЦ/ }));

    expect(estimateMock).not.toHaveBeenCalled();
    expect(screen.getByRole('alert')).toHaveTextContent(/Ответьте/);
  });

  it('shows an error banner when the API is unavailable', async () => {
    estimateMock.mockRejectedValue(new Error('down'));
    renderPage();

    await passDebtStage('10000');

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
