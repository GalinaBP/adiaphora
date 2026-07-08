import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import ReviewsPage from './ReviewsPage';
import { reviewsApi } from '../api/endpoints';
import type { MeResponse, PageResponse, ReviewResponse, UserRole } from '../api/types';

vi.mock('../api/endpoints', () => ({
  reviewsApi: {
    list: vi.fn(),
    get: vi.fn(),
    assign: vi.fn(),
    requestInformation: vi.fn(),
    approve: vi.fn(),
    reject: vi.fn(),
  },
}));

let currentUser: MeResponse | null = null;
vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ user: currentUser, initializing: false }),
}));

const listMock = vi.mocked(reviewsApi.list);
const approveMock = vi.mocked(reviewsApi.approve);
const rejectMock = vi.mocked(reviewsApi.reject);
const assignMock = vi.mocked(reviewsApi.assign);

function staff(role: UserRole): MeResponse {
  return { userId: 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee', email: `${role.toLowerCase()}@example.test`, role, status: 'ACTIVE' } as MeResponse;
}

function review(overrides: Partial<ReviewResponse> = {}): ReviewResponse {
  return {
    reviewId: '11111111-2222-3333-4444-555555555555',
    applicationId: '99999999-8888-7777-6666-555555555555',
    status: 'ASSIGNED',
    assigneeId: null,
    route: 'MANUAL_REVIEW',
    rulesetVersion: 'ruleset-test',
    lastDecisionReason: null,
    ...overrides,
  };
}

function page(items: ReviewResponse[]): PageResponse<ReviewResponse> {
  return { items, page: 0, size: 20, totalElements: items.length, totalPages: 1 };
}

async function renderOpenReview(role: UserRole, item = review()) {
  currentUser = staff(role);
  listMock.mockResolvedValue(page([item]));
  render(
    <MemoryRouter>
      <ReviewsPage />
    </MemoryRouter>,
  );
  await userEvent.click(await screen.findByRole('button', { name: 'Open' }));
}

describe('ReviewsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    currentUser = null;
  });

  it('lists review tasks for staff', async () => {
    currentUser = staff('LAWYER');
    listMock.mockResolvedValue(page([review()]));

    render(
      <MemoryRouter>
        <ReviewsPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText('ASSIGNED')).toBeInTheDocument();
    expect(screen.getByText('MANUAL_REVIEW')).toBeInTheDocument();
  });

  it('blocks an approve with route override until a reason is given, then submits', async () => {
    await renderOpenReview('LAWYER');

    await userEvent.selectOptions(
      screen.getByLabelText('Confirmed route'),
      'MFC_PRELIMINARY',
    );
    await userEvent.click(screen.getByRole('button', { name: 'Approve' }));

    expect(
      await screen.findByText(/Overriding the recommended route requires a reason/i),
    ).toBeInTheDocument();
    expect(approveMock).not.toHaveBeenCalled();

    approveMock.mockResolvedValue(review({ status: 'APPROVED', route: 'MFC_PRELIMINARY' }));
    await userEvent.type(screen.getByLabelText('Reason'), 'Debt is within MFC bounds after check');
    await userEvent.click(screen.getByRole('button', { name: 'Approve' }));

    expect(approveMock).toHaveBeenCalledWith(
      '11111111-2222-3333-4444-555555555555',
      'MFC_PRELIMINARY',
      'Debt is within MFC bounds after check',
    );
    expect(await screen.findByText('APPROVED')).toBeInTheDocument();
  });

  it('approves without a reason when the route is kept', async () => {
    await renderOpenReview('LAWYER');
    approveMock.mockResolvedValue(review({ status: 'APPROVED' }));

    await userEvent.click(screen.getByRole('button', { name: 'Approve' }));

    expect(approveMock).toHaveBeenCalledWith('11111111-2222-3333-4444-555555555555', null, '');
  });

  it('requires a reason to reject', async () => {
    await renderOpenReview('LAWYER');

    await userEvent.click(screen.getByRole('button', { name: 'Reject' }));

    expect(await screen.findByText(/Rejecting a review requires a reason/i)).toBeInTheDocument();
    expect(rejectMock).not.toHaveBeenCalled();
  });

  it('lets an admin assign the review to themselves', async () => {
    await renderOpenReview('ADMIN');
    assignMock.mockResolvedValue(
      review({ status: 'ASSIGNED', assigneeId: 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee' }),
    );

    await userEvent.click(screen.getByRole('button', { name: 'Assign to me' }));

    expect(assignMock).toHaveBeenCalledWith(
      '11111111-2222-3333-4444-555555555555',
      'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee',
    );
  });

  it('hides all actions from auditors', async () => {
    await renderOpenReview('AUDITOR');

    expect(await screen.findByText(/Read-only access/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Approve' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reject' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Assign/ })).not.toBeInTheDocument();
  });

  it('hides decision buttons from operators but allows information requests', async () => {
    await renderOpenReview('OPERATOR');

    expect(screen.queryByRole('button', { name: 'Approve' })).not.toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Request information' }),
    ).toBeInTheDocument();
  });

  it('shows API failures in the error banner', async () => {
    currentUser = staff('LAWYER');
    listMock.mockRejectedValue(new Error('boom'));

    render(
      <MemoryRouter>
        <ReviewsPage />
      </MemoryRouter>,
    );

    expect(await screen.findByRole('alert')).toHaveTextContent('Failed to load reviews');
  });
});
