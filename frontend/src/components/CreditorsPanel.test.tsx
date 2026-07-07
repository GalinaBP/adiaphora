import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CreditorsPanel from './CreditorsPanel';
import { creditorsApi } from '../api/endpoints';
import type { CreditorResponse } from '../api/types';

vi.mock('../api/endpoints', () => ({
  creditorsApi: {
    list: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    remove: vi.fn(),
  },
}));

const api = vi.mocked(creditorsApi);

function creditor(overrides: Partial<CreditorResponse> = {}): CreditorResponse {
  return {
    creditorId: 'c1',
    applicationId: 'app1',
    name: 'Sberbank',
    type: 'BANK',
    inn: null,
    claimBasis: null,
    currency: 'RUB',
    totalAmount: 100000,
    principalAmount: null,
    interestAmount: null,
    penaltyAmount: null,
    overdue: false,
    secured: false,
    duplicateWarning: false,
    ...overrides,
  };
}

describe('CreditorsPanel', () => {
  beforeEach(() => vi.clearAllMocks());

  it('lists creditors returned by the API', async () => {
    api.list.mockResolvedValue([creditor()]);
    render(<CreditorsPanel applicationId="app1" />);
    expect(await screen.findByText('Sberbank')).toBeInTheDocument();
  });

  it('adds a creditor through the form', async () => {
    api.list.mockResolvedValue([]);
    api.create.mockResolvedValue(creditor({ name: 'Tinkoff' }));

    render(<CreditorsPanel applicationId="app1" />);
    await screen.findByText(/No creditors yet/i);

    await userEvent.type(screen.getByLabelText('Creditor name'), 'Tinkoff');
    await userEvent.type(screen.getByLabelText('Total amount'), '50000');
    await userEvent.click(screen.getByRole('button', { name: 'Add creditor' }));

    await waitFor(() =>
      expect(api.create).toHaveBeenCalledWith('app1', expect.objectContaining({
        name: 'Tinkoff',
        type: 'BANK',
        totalAmount: 50000,
      })),
    );
  });

  it('shows a warning when the API flags a duplicate', async () => {
    api.list.mockResolvedValue([]);
    api.create.mockResolvedValue(creditor({ duplicateWarning: true }));

    render(<CreditorsPanel applicationId="app1" />);
    await screen.findByText(/No creditors yet/i);

    await userEvent.type(screen.getByLabelText('Creditor name'), 'Sberbank');
    await userEvent.type(screen.getByLabelText('Total amount'), '100000');
    await userEvent.click(screen.getByRole('button', { name: 'Add creditor' }));

    expect(await screen.findByText(/similar creditor already exists/i)).toBeInTheDocument();
  });

  it('deletes a creditor', async () => {
    api.list.mockResolvedValue([creditor()]);
    api.remove.mockResolvedValue(undefined);

    render(<CreditorsPanel applicationId="app1" />);
    await screen.findByText('Sberbank');
    await userEvent.click(screen.getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(api.remove).toHaveBeenCalledWith('app1', 'c1'));
  });
});
