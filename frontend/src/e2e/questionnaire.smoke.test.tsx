import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import App from '../App';
import { AuthProvider } from '../auth/AuthContext';
import { tokenStore } from '../api/client';
import type { CompletionResponse, FormResponse, MeResponse, ValidationResponse } from '../api/types';

// End-to-end smoke test for the questionnaire flow. Unlike the component tests, nothing in the app
// is mocked here except the network boundary (global fetch): the real router, auth gate, API client,
// endpoint wrappers, and components all run. It walks the happy path a user takes — resume the saved
// form, answer questions across two steps, and validate — asserting the app wires end to end.

const me: MeResponse = {
  userId: 'u-1',
  email: 'user@example.test',
  role: 'USER',
  status: 'ACTIVE',
};

const savedForm: FormResponse = {
  applicationId: 'app-1',
  versionCode: 'v1',
  label: 'Bankruptcy questionnaire',
  sections: [
    { code: 's1', title: 'Personal', displayOrder: 1 },
    { code: 's2', title: 'Debts', displayOrder: 2 },
  ],
  questions: [
    {
      code: 'full_name',
      sectionCode: 's1',
      type: 'TEXT',
      label: 'Full name',
      helpText: null,
      required: true,
      displayOrder: 1,
      options: [],
    },
    {
      code: 'has_debts',
      sectionCode: 's2',
      type: 'SINGLE_CHOICE',
      label: 'Do you have debts?',
      helpText: null,
      required: true,
      displayOrder: 1,
      options: [
        { value: 'YES', label: 'Yes', displayOrder: 1 },
        { value: 'NO', label: 'No', displayOrder: 2 },
      ],
    },
  ],
  answers: {},
  completion: { requiredTotal: 2, requiredAnswered: 0, missingRequired: ['full_name', 'has_debts'], complete: false },
};

function jsonResponse(status: number, body: unknown) {
  return {
    status,
    ok: status >= 200 && status < 300,
    text: async () => JSON.stringify(body),
  } as Response;
}

// A tiny in-memory fake of the backend for the endpoints this flow touches.
function fakeBackend() {
  let answered = 0;
  return (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
    const url = String(input);
    const method = init?.method ?? 'GET';

    if (url.endsWith('/auth/me')) return Promise.resolve(jsonResponse(200, me));

    if (method === 'POST' && url.endsWith('/questionnaire/validate')) {
      const result: ValidationResponse = { complete: true, missingRequired: [], fieldErrors: [] };
      return Promise.resolve(jsonResponse(200, result));
    }

    if (method === 'PUT' && url.includes('/answers/')) {
      answered = Math.min(2, answered + 1);
      const completion: CompletionResponse = {
        requiredTotal: 2,
        requiredAnswered: answered,
        missingRequired: [],
        complete: answered === 2,
      };
      return Promise.resolve(jsonResponse(200, completion));
    }

    if (method === 'GET' && url.endsWith('/questionnaire')) {
      return Promise.resolve(jsonResponse(200, savedForm));
    }

    return Promise.resolve(jsonResponse(404, { code: 'NOT_FOUND', message: `unmatched ${method} ${url}` }));
  };
}

describe('questionnaire flow (smoke)', () => {
  beforeEach(() => {
    tokenStore.set('test-token');
    vi.stubGlobal('fetch', vi.fn(fakeBackend()));
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    tokenStore.clear();
  });

  it('resumes, answers both sections, and validates as complete', async () => {
    render(
      <MemoryRouter initialEntries={['/applications/app-1/questionnaire']}>
        <AuthProvider>
          <App />
        </AuthProvider>
      </MemoryRouter>,
    );

    // Step 1: the saved form loads (resume) and the first section is shown.
    const nameField = await screen.findByLabelText(/Full name/);
    expect(screen.getByRole('heading', { name: 'Bankruptcy questionnaire' })).toBeInTheDocument();

    await userEvent.type(nameField, 'Ivan Petrov');
    await userEvent.tab();
    await waitFor(() =>
      expect(screen.getByTestId('required-progress')).toHaveTextContent('1/2 required answered'),
    );

    // Step 2: navigate and answer the choice question (commits on change).
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));
    await userEvent.selectOptions(screen.getByLabelText(/Do you have debts/), 'YES');
    await waitFor(() =>
      expect(screen.getByTestId('required-progress')).toHaveTextContent('2/2 required answered'),
    );

    // Validate the whole questionnaire from the last step.
    await userEvent.click(screen.getByRole('button', { name: 'Check answers' }));
    expect(await screen.findByText(/ready to submit/i)).toBeInTheDocument();
  });
});
