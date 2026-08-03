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
        worldId: 'w1',
        status: 'ACTIVE',
        round: 1,
        currentTurnEntityId: 'e1',
        createdAt: '',
      },
      participants: [
        {
          id: 'p1',
          name: 'Hero',
          entityId: 'e1',
          initiative: 20,
          apCurrent: 1,
          apMax: 2,
          hpCurrent: 10,
          hpMax: 10,
          side: 'A',
        },
      ],
    });

    render(<ApBar />);
    expect(screen.getByText(/1 \/ 2/)).toBeInTheDocument();
  });
});
