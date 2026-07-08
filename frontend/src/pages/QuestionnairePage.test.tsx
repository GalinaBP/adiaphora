import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import QuestionnairePage from './QuestionnairePage';
import { questionnaireApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import type { CompletionResponse, FormResponse } from '../api/types';

vi.mock('../api/endpoints', () => ({
  questionnaireApi: {
    form: vi.fn(),
    saveAnswer: vi.fn(),
    validate: vi.fn(),
  },
}));

const formMock = vi.mocked(questionnaireApi.form);
const saveMock = vi.mocked(questionnaireApi.saveAnswer);
const validateMock = vi.mocked(questionnaireApi.validate);

function form(overrides: Partial<FormResponse> = {}): FormResponse {
  return {
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
    ...overrides,
  };
}

function completion(answered: number): CompletionResponse {
  return {
    requiredTotal: 2,
    requiredAnswered: answered,
    missingRequired: [],
    complete: answered === 2,
  };
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/applications/app-1/questionnaire']}>
      <Routes>
        <Route path="/applications/:applicationId/questionnaire" element={<QuestionnairePage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('QuestionnairePage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('shows the first section, step position, and required progress', async () => {
    formMock.mockResolvedValue(form());
    renderPage();

    expect(await screen.findByLabelText(/Full name/)).toBeInTheDocument();
    expect(screen.getByText('Раздел 1 из 2')).toBeInTheDocument();
    expect(screen.getByTestId('required-progress')).toHaveTextContent('0/2 обязательных отвечено');
    // The second section's question is not rendered until we navigate to it.
    expect(screen.queryByLabelText(/Do you have debts/)).not.toBeInTheDocument();
  });

  it('navigates forward and back between sections', async () => {
    formMock.mockResolvedValue(form());
    renderPage();
    await screen.findByLabelText(/Full name/);

    await userEvent.click(screen.getByRole('button', { name: 'Далее' }));
    expect(screen.getByLabelText(/Do you have debts/)).toBeInTheDocument();
    expect(screen.getByText('Раздел 2 из 2')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Назад' }));
    expect(screen.getByLabelText(/Full name/)).toBeInTheDocument();
  });

  it('autosaves an answer on blur and updates progress', async () => {
    formMock.mockResolvedValue(form());
    saveMock.mockResolvedValue(completion(1));
    renderPage();

    const input = await screen.findByLabelText(/Full name/);
    await userEvent.type(input, 'Ivan Petrov');
    await userEvent.tab();

    await waitFor(() =>
      expect(saveMock).toHaveBeenCalledWith('app-1', 'full_name', 'Ivan Petrov'),
    );
    expect(await screen.findByText('Все изменения сохранены')).toBeInTheDocument();
    expect(screen.getByTestId('required-progress')).toHaveTextContent('1/2 обязательных отвечено');
  });

  it('surfaces a per-field validation error returned by save', async () => {
    formMock.mockResolvedValue(form());
    saveMock.mockRejectedValue(
      new ApiError(422, 'VALIDATION_ERROR', 'Invalid answer', {
        timestamp: '',
        status: 422,
        code: 'VALIDATION_ERROR',
        message: 'Invalid answer',
        path: '',
        correlationId: '',
        fieldErrors: [{ field: 'full_name', message: 'must not be blank' }],
      }),
    );
    renderPage();

    const input = await screen.findByLabelText(/Full name/);
    await userEvent.type(input, 'x');
    await userEvent.tab();

    expect(await screen.findByText('must not be blank')).toBeInTheDocument();
    expect(input).toHaveAttribute('aria-invalid', 'true');
  });

  it('keeps the typed value and shows the error when a save fails', async () => {
    formMock.mockResolvedValue(form());
    saveMock.mockRejectedValue(new ApiError(500, 'INTERNAL_ERROR', 'Server unavailable'));
    renderPage();

    const input = await screen.findByLabelText(/Full name/);
    await userEvent.type(input, 'Anna Petrova');
    await userEvent.tab();

    // The failure is visible, but nothing the user typed is lost.
    expect(await screen.findByText('Server unavailable')).toBeInTheDocument();
    expect(input).toHaveValue('Anna Petrova');

    // Navigating away and back still shows the locally kept value.
    await userEvent.click(screen.getByRole('button', { name: 'Далее' }));
    await userEvent.click(screen.getByRole('button', { name: 'Назад' }));
    expect(screen.getByLabelText(/Full name/)).toHaveValue('Anna Petrova');
  });

  it('validates on the last step and lets the user jump to a missing question', async () => {
    formMock.mockResolvedValue(form());
    validateMock.mockResolvedValue({
      complete: false,
      missingRequired: ['full_name'],
      fieldErrors: [],
    });
    renderPage();
    await screen.findByLabelText(/Full name/);

    await userEvent.click(screen.getByRole('button', { name: 'Далее' }));
    await userEvent.click(screen.getByRole('button', { name: 'Проверить ответы' }));

    expect(await screen.findByText(/вопросы без ответа/i)).toBeInTheDocument();
    // The missing question is listed by its label and jumps back to its section when clicked.
    await userEvent.click(screen.getByRole('button', { name: 'Full name' }));
    expect(screen.getByLabelText(/Full name/)).toBeInTheDocument();
    expect(screen.getByText('Раздел 1 из 2')).toBeInTheDocument();
  });

  it('shows a load error when the questionnaire cannot be fetched', async () => {
    formMock.mockRejectedValue(new ApiError(500, 'INTERNAL_ERROR', 'boom'));
    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent('boom');
  });
});
