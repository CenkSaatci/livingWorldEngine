import { render, screen } from '@testing-library/react';
import { describe, it, expect, beforeEach } from 'vitest';
import { ApBar } from './ApBar';
import { useCombatStore } from '../../store/combatStore';

beforeEach(() => {
  useCombatStore.setState({ session: null, participants: [] });
});

describe('ApBar', () => {
  it('renders nothing when no active session', () => {
    const { container } = render(<ApBar />);
    expect(container).toBeEmptyDOMElement();
  });

  it('shows AP bar for current actor', () => {
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
          name: 'Hero',
          entity_id: 'e1',
          initiative: 20,
          ap_current: 1,
          ap_max: 2,
          hp_current: 10,
          hp_max: 10,
          side: 'A',
        },
      ],
    });

    render(<ApBar />);
    expect(screen.getByText(/1 \/ 2/)).toBeInTheDocument();
  });
});
