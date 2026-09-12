import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import CombatPage from './CombatPage';
import { useCombatStore } from '../store/combatStore';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}));

vi.mock('../hooks/useWorldSocket', () => ({
  useWorldSocket: vi.fn(),
}));

vi.mock('../components/map/MapCanvas', () => ({
  MapCanvas: () => <div>map-canvas</div>,
}));
vi.mock('../components/chat/ChatPanel', () => ({
  ChatPanel: () => <div>chat-panel</div>,
}));
vi.mock('../components/chat/RollLog', () => ({
  RollLog: () => <div>roll-log</div>,
}));
vi.mock('../components/combat/InitiativeList', () => ({
  InitiativeList: () => <div>initiative-list</div>,
}));
vi.mock('../components/combat/ActionBar', () => ({
  ActionBar: () => <div>action-bar</div>,
}));
vi.mock('../components/combat/ApBar', () => ({
  ApBar: () => <div>ap-bar</div>,
}));

import { useWorldSocket } from '../hooks/useWorldSocket';

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/combat/w1']}>
      <Routes>
        <Route path="/combat/:id" element={<CombatPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  useCombatStore.setState({ session: null, participants: [], targetEntityId: null });
});

describe('CombatPage', () => {
  it('renders without crashing', () => {
    expect(() => renderPage()).not.toThrow();
  });

  it('renders combat title and children', () => {
    const { container } = renderPage();
    expect(container.textContent).toContain('gameView.combat'); // R4: Kampf-Titel statt "Karte"
    expect(container.textContent).toContain('map-canvas');
    expect(container.textContent).toContain('initiative-list');
    expect(container.textContent).toContain('action-bar');
  });

  it('wires world socket with worldId from route', () => {
    renderPage();
    expect(useWorldSocket).toHaveBeenCalledWith('w1');
  });

  it('renders with an active combat session in store', () => {
    useCombatStore.setState({
      session: {
        id: 's1',
        worldId: 'w1',
        status: 'ACTIVE',
        round: 2,
        currentTurnEntityId: 'e1',
        createdAt: '2026-01-01T00:00:00Z',
      },
      participants: [],
    });
    const { container } = renderPage();
    expect(container.textContent).toContain('ap-bar');
  });
});
