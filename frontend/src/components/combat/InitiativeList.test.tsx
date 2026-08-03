import { render, screen } from '@testing-library/react';
import { describe, it, expect, beforeEach } from 'vitest';
import { InitiativeList } from './InitiativeList';
import { useCombatStore } from '../../store/combatStore';

beforeEach(() => {
  useCombatStore.setState({
    session: null,
    participants: [],
  });
});

describe('InitiativeList', () => {
  it('renders nothing when no session', () => {
    const { container } = render(<InitiativeList />);
    expect(container).toBeEmptyDOMElement();
  });

  it('shows round number and participants', () => {
    useCombatStore.setState({
      session: {
        id: 's1',
        worldId: 'w1',
        status: 'ACTIVE',
        round: 3,
        currentTurnEntityId: 'e1',
        createdAt: '',
      },
      participants: [
        {
          id: 'p1',
          name: "Test",
          entityId: 'e1',
          initiative: 20,
          apCurrent: 1,
          apMax: 2,
          hpCurrent: 10,
          hpMax: 10,
          side: 'A',
        },
        {
          id: 'p2',
          name: "Test",
          entityId: 'e2',
          initiative: 15,
          apCurrent: 2,
          apMax: 2,
          hpCurrent: 10,
          hpMax: 10,
          side: 'B',
        },
      ],
    });

    render(<InitiativeList />);
    expect(screen.getByText(/runde/i)).toBeInTheDocument();
    expect(screen.getByText(/AP 1\/2/)).toBeInTheDocument();
  });

  it('shows defeated status for zero HP', () => {
    useCombatStore.setState({
      session: {
        id: 's1',
        worldId: 'w1',
        status: 'ACTIVE',
        round: 1,
        currentTurnEntityId: 'e1',
        createdAt: '',
      },
      participants: [
        {
          id: 'p1',
          name: "Test",
          entityId: 'e1',
          initiative: 20,
          apCurrent: 0,
          apMax: 2,
          hpCurrent: 0,
          hpMax: 10,
          side: 'A',
        },
      ],
    });

    render(<InitiativeList />);
    expect(screen.getByText(/defeated/i)).toBeInTheDocument();
  });
});
