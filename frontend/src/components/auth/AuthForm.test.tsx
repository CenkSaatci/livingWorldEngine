import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { AuthForm } from './AuthForm';

describe('AuthForm', () => {
  it('renders login mode with email and password fields', () => {
    render(<AuthForm mode="login" onSubmit={vi.fn()} error={null} />);
    expect(screen.getByLabelText('E-Mail')).toBeInTheDocument();
    expect(screen.getByLabelText('Passwort')).toBeInTheDocument();
    expect(screen.queryByLabelText('Benutzername')).not.toBeInTheDocument();
  });

  it('renders register mode with username field', () => {
    render(<AuthForm mode="register" onSubmit={vi.fn()} error={null} />);
    expect(screen.getByLabelText('Benutzername')).toBeInTheDocument();
  });

  it('displays error message', () => {
    render(<AuthForm mode="login" onSubmit={vi.fn()} error="Invalid credentials" />);
    expect(screen.getByRole('alert')).toHaveTextContent('Invalid credentials');
  });

  it('calls onSubmit with field values', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    const user = userEvent.setup();
    render(<AuthForm mode="login" onSubmit={onSubmit} error={null} />);

    await user.type(screen.getByLabelText('E-Mail'), 'test@test.com');
    await user.type(screen.getByLabelText('Passwort'), 'secret123');
    await user.click(screen.getByRole('button', { name: 'Anmelden' }));

    expect(onSubmit).toHaveBeenCalledWith({
      email: 'test@test.com',
      username: '',
      password: 'secret123',
    });
  });
});
