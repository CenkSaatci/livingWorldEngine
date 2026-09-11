import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from './LoginPage';
import { useToastStore } from '../store/toastStore';

vi.mock('../api/client', () => ({
  apiClient: { post: vi.fn() },
  getAccessToken: vi.fn(),
  setTokens: vi.fn(),
  clearTokens: vi.fn(),
}));

import { apiClient } from '../api/client';

const postMock = apiClient.post as ReturnType<typeof vi.fn>;

function renderLogin() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <LoginPage />
    </MemoryRouter>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  useToastStore.setState({ toasts: [] });
});

describe('LoginPage (auth smoke)', () => {
  it('rendert Login-Formular mit Validierungsattributen', () => {
    renderLogin();
    const email = screen.getByLabelText(/email/i) as HTMLInputElement;
    const password = document.getElementById('password') as HTMLInputElement;
    expect(email).toBeRequired();
    expect(email).toHaveAttribute('type', 'email');
    expect(password).toBeRequired();
    expect(password.minLength).toBeGreaterThanOrEqual(6);
    expect(screen.getByRole('button', { name: /sign in|anmelden/i })).toBeInTheDocument();
  });

  it('zeigt Fehlermeldung bei fehlgeschlagenem Login', async () => {
    postMock.mockRejectedValue({
      response: { data: { error: { code: 'AUTH_INVALID_CREDENTIALS' } } },
    });
    const user = userEvent.setup();
    renderLogin();

    await user.type(screen.getByLabelText(/email/i), 'a@b.c');
    await user.type(document.getElementById('password') as HTMLInputElement, 'falsch12');
    await user.click(screen.getByRole('button', { name: /sign in|anmelden/i }));

    await waitFor(() => expect(postMock).toHaveBeenCalledWith('/auth/login', expect.anything()));
    await waitFor(() =>
      expect(screen.getByText('AUTH_INVALID_CREDENTIALS')).toBeInTheDocument(),
    );
  });
});
