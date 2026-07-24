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
        world_id: 'w1',
        status: 'ACTIVE',
        round: 3,
        current_turn_entity_id: 'e1',
        created_at: '',
      },
      participants: [
        {
          id: 'p1',
          name: "Test",
          entity_id: 'e1',
          initiative: 20,
          ap_current: 1,
          ap_max: 2,
          hp_current: 10,
          hp_max: 10,
          side: 'A',
        },
        {
          id: 'p2',
          name: "Test",
          entity_id: 'e2',
          initiative: 15,
          ap_current: 2,
          ap_max: 2,
          hp_current: 10,
          hp_max: 10,
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
        world_id: 'w1',
        status: 'ACTIVE',
        round: 1,
        current_turn_entity_id: 'e1',
        created_at: '',
      },
      participants: [
        {
          id: 'p1',
          name: "Test",
          entity_id: 'e1',
          initiative: 20,
          ap_current: 0,
          ap_max: 2,
          hp_current: 0,
          hp_max: 10,
          side: 'A',
        },
      ],
    });

    render(<InitiativeList />);
    expect(screen.getByText(/defeated/i)).toBeInTheDocument();
  });
});
