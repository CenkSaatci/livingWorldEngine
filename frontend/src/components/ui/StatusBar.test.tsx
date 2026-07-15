import { render, screen } from '@testing-library/react';
import { describe, it, expect, beforeEach } from 'vitest';
import { StatusBar } from './StatusBar';
import { useAuthStore } from '../../store/authStore';
import { useWorldStore } from '../../store/worldStore';

beforeEach(() => {
  useAuthStore.setState({
    user: {
      id: '1',
      email: 'dm@test.com',
      username: 'DM',
      role: 'ADMIN',
      locale: 'de',
      emailVerified: true,
    },
    isAuthenticated: true,
  });
  useWorldStore.setState({
    currentWorld: {
      id: 'w1',
      name: 'Test',
      game_system_id: null,
      current_game_time: null,
      created_at: new Date().toISOString(),
    },
  });
});

describe('StatusBar', () => {
  it('shows DM controls for admin users', () => {
    render(<StatusBar />);
    expect(screen.getByLabelText('Toggle fog of war')).toBeInTheDocument();
  });
});
