import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ApplicationsPage from './ApplicationsPage';
import { applicationsApi } from '../api/endpoints';
import type { ApplicationResponse, PageResponse } from '../api/types';

vi.mock('../api/endpoints', () => ({
  applicationsApi: {
    list: vi.fn(),
    create: vi.fn(),
    get: vi.fn(),
  },
}));

const listMock = vi.mocked(applicationsApi.list);

function page(items: ApplicationResponse[]): PageResponse<ApplicationResponse> {
  return { items, page: 0, size: 20, totalElements: items.length, totalPages: 1 };
}

describe('ApplicationsPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('renders cases returned by the API', async () => {
    listMock.mockResolvedValue(
      page([
        {
          applicationId: '11111111-2222-3333-4444-555555555555',
          ownerId: 'owner-1',
          status: 'DRAFT',
          route: 'NOT_EVALUATED',
          submittedAt: null,
        },
      ]),
    );

    render(
      <MemoryRouter>
        <ApplicationsPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText('Черновик')).toBeInTheDocument();
    expect(screen.getByText('Не оценено')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Новое дело' })).toBeInTheDocument();
  });

  it('shows an empty state when there are no cases', async () => {
    listMock.mockResolvedValue(page([]));

    render(
      <MemoryRouter>
        <ApplicationsPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText(/Дел пока нет/i)).toBeInTheDocument();
  });
});
