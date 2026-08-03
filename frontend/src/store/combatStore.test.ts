import { describe, it, expect, beforeEach } from 'vitest';
import { useCombatStore } from './combatStore';

const mockSession = {
  id: 's1',
  worldId: 'w1',
  status: 'ACTIVE',
  round: 1,
  currentTurnEntityId: 'e1',
  mapId: 'm1',
  createdAt: '',
};
const mockParticipants = [
  {
    id: 'p1',
    entityId: 'e1',
    name: 'Hero',
    initiative: 20,
    apCurrent: 2,
    apMax: 2,
    hpCurrent: 10,
    hpMax: 10,
    side: 'A',
  },
];

beforeEach(() =>
  useCombatStore.setState({ session: null, participants: [], targetEntityId: null }),
);

describe('combatStore', () => {
  it('sets session and participants with camelCase fields', () => {
    useCombatStore.getState().setSession(mockSession, mockParticipants);
    expect(useCombatStore.getState().session?.worldId).toBe('w1');
    expect(useCombatStore.getState().session?.currentTurnEntityId).toBe('e1');
    expect(useCombatStore.getState().participants[0].entityId).toBe('e1');
    expect(useCombatStore.getState().participants[0].apCurrent).toBe(2);
  });

  it('updates participant AP', () => {
    useCombatStore.getState().setSession(mockSession, mockParticipants);
    useCombatStore.getState().updateParticipantAp('e1', 1);
    expect(useCombatStore.getState().participants[0].apCurrent).toBe(1);
  });

  it('updates turn via handleTurnChanged', () => {
    useCombatStore.getState().setSession(mockSession, mockParticipants);
    useCombatStore.getState().handleTurnChanged('e2', 2);
    expect(useCombatStore.getState().session?.currentTurnEntityId).toBe('e2');
    expect(useCombatStore.getState().session?.round).toBe(2);
  });

  it('clears combat on end', () => {
    useCombatStore.getState().setSession(mockSession, mockParticipants);
    useCombatStore.getState().clearCombat();
    expect(useCombatStore.getState().session).toBeNull();
  });
});
