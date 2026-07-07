import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import QuestionField from './QuestionField';
import type { QuestionResponse } from '../api/types';

function question(overrides: Partial<QuestionResponse> = {}): QuestionResponse {
  return {
    code: 'q1',
    sectionCode: 's1',
    type: 'TEXT',
    label: 'Full name',
    helpText: null,
    required: true,
    displayOrder: 1,
    options: [],
    ...overrides,
  };
}

describe('QuestionField', () => {
  it('renders a text input and commits its value on blur', async () => {
    const onCommit = vi.fn();
    render(<QuestionField question={question()} value="" onCommit={onCommit} />);

    const input = screen.getByLabelText(/Full name/);
    await userEvent.type(input, 'Ivan');
    await userEvent.tab();

    expect(onCommit).toHaveBeenCalledWith('Ivan');
  });

  it('renders options for a single-choice question', () => {
    const q = question({
      type: 'SINGLE_CHOICE',
      options: [
        { value: 'YES', label: 'Yes', displayOrder: 1 },
        { value: 'NO', label: 'No', displayOrder: 2 },
      ],
    });
    render(<QuestionField question={q} value="" onCommit={vi.fn()} />);

    expect(screen.getByRole('option', { name: 'Yes' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'No' })).toBeInTheDocument();
  });

  it('renders a date input for a DATE question', () => {
    render(<QuestionField question={question({ type: 'DATE' })} value="" onCommit={vi.fn()} />);
    expect(screen.getByLabelText(/Full name/)).toHaveAttribute('type', 'date');
  });

  it('shows a validation error and marks the control invalid', () => {
    render(
      <QuestionField question={question()} value="" onCommit={vi.fn()} error="must be a whole number" />,
    );

    expect(screen.getByRole('alert')).toHaveTextContent('must be a whole number');
    expect(screen.getByLabelText(/Full name/)).toHaveAttribute('aria-invalid', 'true');
  });

  it('reflects an externally updated value (resume)', () => {
    const { rerender } = render(<QuestionField question={question()} value="" onCommit={vi.fn()} />);
    rerender(<QuestionField question={question()} value="Ivan" onCommit={vi.fn()} />);

    expect(screen.getByLabelText(/Full name/)).toHaveValue('Ivan');
  });
});
