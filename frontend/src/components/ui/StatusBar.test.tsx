import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { StatusBar } from './StatusBar';
import { useWorldStore } from '../../store/worldStore';
import { useSessionStore } from '../../store/sessionStore';
import { useCombatStore } from '../../store/combatStore';
import { useAuthStore } from '../../store/authStore';

const mockWorld = {
  id: 'w1',
  name: 'Testwelt',
  game_system_id: 'gs1',
  current_game_time: '2026-07-19T14:30:00Z',
  created_at: '2026-01-01T00:00:00Z',
};

const mockUser = { id: 'u1', email: 'dm@test.com', username: 'testdm', role: 'ADMIN', locale: 'de' };
const mockUserPlayer = { id: 'u2', email: 'player@test.com', username: 'player', role: 'USER', locale: 'de' };

beforeEach(() => {
  vi.restoreAllMocks();
  useWorldStore.setState({ currentWorld: null });
  useSessionStore.setState({ activeSession: null });
  useCombatStore.setState({ session: null, participants: [] });
  useAuthStore.setState({ user: null, isAuthenticated: false });
});

function renderStatusBar() {
  return render(<StatusBar />);
}

describe('StatusBar', () => {
  it('shows time placeholder when no world', () => {
    const { container } = renderStatusBar();
    expect(container.textContent).toContain('\u2014');
  });

  it('shows game time and phase when world is set', () => {
    useWorldStore.setState({ currentWorld: mockWorld });
    const { container } = renderStatusBar();
    expect(container.textContent).toContain('Day');
    expect(container.textContent).toContain('16:30');
  });

  it('shows "no session" by default', () => {
    useWorldStore.setState({ currentWorld: mockWorld });
    const { container } = renderStatusBar();
    expect(container.textContent).toContain('No session');
  });

  it('shows active session when set', () => {
    useWorldStore.setState({ currentWorld: mockWorld });
    useSessionStore.setState({
      activeSession: {
        id: 's1', world_id: 'w1', dm_id: 'u1', started_at: '2026-07-19T14:00:00Z',
        ended_at: null, status: 'ACTIVE' as const, created_at: '2026-07-19T14:00:00Z',
      },
    });
    const { container } = renderStatusBar();
    expect(container.textContent).toContain('Session active');
  });

  it('shows combat turn info when combat is active', () => {
    useWorldStore.setState({ currentWorld: mockWorld });
    useCombatStore.setState({
      session: { id: 'c1', world_id: 'w1', status: 'ACTIVE', round: 3, current_turn_entity_id: 'e2' } as any,
      participants: [
        { entity_id: 'e1', name: 'Hero', initiative: 20, entity_type: 'PC', is_active: true, hp_current: 10, hp_max: 10 } as any,
        { entity_id: 'e2', name: 'Goblin', initiative: 15, entity_type: 'NPC', is_active: true, hp_current: 5, hp_max: 5 } as any,
      ],
    });
    const { container } = renderStatusBar();
    expect(container.textContent).toContain('Rd. 3');
    expect(container.textContent).toContain('2/2');
  });

  it('shows DM controls for admin users', () => {
    useWorldStore.setState({ currentWorld: mockWorld });
    useAuthStore.setState({ user: mockUser, isAuthenticated: true });
    renderStatusBar();
    // DM buttons should be present
    expect(screen.getByLabelText('Toggle fog of war')).toBeTruthy();
    expect(screen.getByLabelText('Pause or resume time')).toBeTruthy();
    expect(screen.getByLabelText('Advance time')).toBeTruthy();
  });

  it('hides DM controls for non-admin users', () => {
    useWorldStore.setState({ currentWorld: mockWorld });
    useAuthStore.setState({ user: mockUserPlayer, isAuthenticated: true });
    renderStatusBar();
    expect(screen.queryByLabelText('Toggle fog of war')).toBeNull();
    expect(screen.queryByLabelText('Pause or resume time')).toBeNull();
    expect(screen.queryByLabelText('Advance time')).toBeNull();
  });

  it('shows advance time options on click', async () => {
    useWorldStore.setState({ currentWorld: mockWorld });
    useAuthStore.setState({ user: mockUser, isAuthenticated: true });
    renderStatusBar();
    const advanceBtn = screen.getByLabelText('Advance time');
    await userEvent.click(advanceBtn);
    expect(screen.getByText('+1h')).toBeTruthy();
    expect(screen.getByText('+6h')).toBeTruthy();
    expect(screen.getByText('+1d')).toBeTruthy();
    expect(screen.getByText('bis Dawn')).toBeTruthy();
    expect(screen.getByText('bis Dusk')).toBeTruthy();
  });

  it('hides controls when no worldId', () => {
    useAuthStore.setState({ user: mockUser, isAuthenticated: true });
    renderStatusBar();
    expect(screen.queryByLabelText('Toggle fog of war')).toBeNull();
  });
});
