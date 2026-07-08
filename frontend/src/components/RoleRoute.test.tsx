import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import RoleRoute from './RoleRoute';
import type { MeResponse, UserRole } from '../api/types';

let currentUser: MeResponse | null = null;
vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ user: currentUser, initializing: false }),
}));

function renderAt(role: UserRole) {
  currentUser = {
    userId: 'u-1',
    email: 'x@example.test',
    role,
    status: 'ACTIVE',
  } as MeResponse;
  render(
    <MemoryRouter initialEntries={['/reviews']}>
      <Routes>
        <Route path="/applications" element={<div>cases screen</div>} />
        <Route element={<RoleRoute roles={['OPERATOR', 'LAWYER', 'ADMIN', 'AUDITOR']} />}>
          <Route path="/reviews" element={<div>reviews screen</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('RoleRoute', () => {
  beforeEach(() => {
    currentUser = null;
  });

  it('renders the staff screen for an allowed role', () => {
    renderAt('LAWYER');
    expect(screen.getByText('reviews screen')).toBeInTheDocument();
  });

  it('redirects ordinary users back to their cases', () => {
    renderAt('USER');
    expect(screen.getByText('cases screen')).toBeInTheDocument();
    expect(screen.queryByText('reviews screen')).not.toBeInTheDocument();
  });
});
