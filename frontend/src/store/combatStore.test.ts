import { describe, it, expect, beforeEach } from 'vitest';
import { useCombatStore } from './combatStore';

const mockSession = {
  id: 's1',
  world_id: 'w1',
  status: 'ACTIVE',
  round: 1,
  current_turn_entity_id: 'e1',
  created_at: '',
};
const mockParticipants = [
  {
    id: 'p1',
        entity_id: 'e1',
        name: 'Hero',
    initiative: 20,
    ap_current: 2,
    ap_max: 2,
    hp_current: 10,
    hp_max: 10,
    side: 'A',
  },
];

beforeEach(() =>
  useCombatStore.setState({ session: null, participants: [], targetEntityId: null }),
);

describe('combatStore', () => {
  it('sets session and participants', () => {
    useCombatStore.getState().setSession(mockSession, mockParticipants);
    expect(useCombatStore.getState().session?.round).toBe(1);
    expect(useCombatStore.getState().participants).toHaveLength(1);
  });

  it('updates participant AP', () => {
    useCombatStore.getState().setSession(mockSession, mockParticipants);
    useCombatStore.getState().updateParticipantAp('e1', 1);
    expect(useCombatStore.getState().participants[0].ap_current).toBe(1);
  });

  it('clears combat on end', () => {
    useCombatStore.getState().setSession(mockSession, mockParticipants);
    useCombatStore.getState().clearCombat();
    expect(useCombatStore.getState().session).toBeNull();
  });
});
