import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render } from '@testing-library/react';
import { CharacterSheet } from './CharacterSheet';

vi.mock('../../api/client', () => ({
  apiClient: { get: vi.fn(), patch: vi.fn() },
}));

vi.mock('../../hooks/useSheet', () => ({
  useSheet: vi.fn(),
}));

vi.mock('../../hooks/useToast', () => ({
  useToast: () => ({ error: vi.fn() }),
}));

import { useSheet } from '../../hooks/useSheet';
import { apiClient } from '../../api/client';

const mockSheet = {
  entity: { id: 'e1', name: 'Held', entityType: 'PC' },
  experiencePoints: 1500,
  level: 3,
  attributes: [
    { name: 'staerke', value: 15, modifier: 2, min: 3, max: 20 },
    { name: 'geschick', value: 12, modifier: 1, min: 3, max: 20 },
  ],
  derivedValues: [
    { name: 'hp', value: 25 },
    { name: 'ac', value: 14 },
  ],
  skills: [
    { name: 'Athletik', total: 4, perCharacterValue: null },
    { name: 'Wahrnehmung', total: 2, perCharacterValue: null },
  ],
  conditionals: [
    { name: 'Stark', active: true, description: '+2 auf schaden' },
    { name: 'Zäh', active: false, description: '' },
  ],
  abilities: [
    { name: 'Angriff', type: 'active', apCost: 1, effect: 'Nahkampf', diceExpression: '1d20+staerke' },
    { name: 'Parade', type: 'active', apCost: 0, effect: 'Reaktion', diceExpression: '1d20+mut' },
  ],
};

beforeEach(() => {
  vi.clearAllMocks();
  (apiClient.get as any).mockResolvedValue({ data: { metadata_json: '{}' } });
});

describe('CharacterSheet', () => {
  it('shows loading state', () => {
    (useSheet as any).mockReturnValue({ data: null, loading: true, error: null });
    const { container } = render(<CharacterSheet entityId="e1" />);
    expect(container.textContent).toBeTruthy();
  });

  it('shows error state', () => {
    (useSheet as any).mockReturnValue({ data: null, loading: false, error: 'Fehler' });
    const { container } = render(<CharacterSheet entityId="e1" />);
    expect(container.textContent).toContain('Fehler');
  });

  it('renders entity name', () => {
    (useSheet as any).mockReturnValue({ data: mockSheet, loading: false, error: null, refetch: vi.fn() });
    const { container } = render(<CharacterSheet entityId="e1" />);
    expect(container.textContent).toContain('Held');
  });

  it('renders attributes with values', () => {
    (useSheet as any).mockReturnValue({ data: mockSheet, loading: false, error: null, refetch: vi.fn() });
    const { container } = render(<CharacterSheet entityId="e1" />);
    expect(container.textContent).toContain('15');
    expect(container.textContent).toContain('12');
    expect(container.textContent).toContain('+2');
  });

  it('renders derived values', () => {
    (useSheet as any).mockReturnValue({ data: mockSheet, loading: false, error: null, refetch: vi.fn() });
    const { container } = render(<CharacterSheet entityId="e1" />);
    expect(container.textContent).toContain('hp');
    expect(container.textContent).toContain('25');
  });

  it('renders skills', () => {
    (useSheet as any).mockReturnValue({ data: mockSheet, loading: false, error: null, refetch: vi.fn() });
    const { container } = render(<CharacterSheet entityId="e1" />);
    expect(container.textContent).toContain('Athletik');
  });

  it('renders active conditionals', () => {
    (useSheet as any).mockReturnValue({ data: mockSheet, loading: false, error: null, refetch: vi.fn() });
    const { container } = render(<CharacterSheet entityId="e1" />);
    expect(container.textContent).toContain('Stark');
  });

  it('shows XP', () => {
    (useSheet as any).mockReturnValue({ data: mockSheet, loading: false, error: null, refetch: vi.fn() });
    const { container } = render(<CharacterSheet entityId="e1" />);
    expect(container.textContent).toContain('1500');
  });

  it('renders abilities', () => {
    (useSheet as any).mockReturnValue({ data: mockSheet, loading: false, error: null, refetch: vi.fn() });
    const { container } = render(<CharacterSheet entityId="e1" />);
    expect(container.textContent).toContain('Angriff');
    expect(container.textContent).toContain('active');
    expect(container.textContent).toContain('AP: 1');
  });

  it('shows formula overrides section', () => {
    (useSheet as any).mockReturnValue({ data: mockSheet, loading: false, error: null, refetch: vi.fn() });
    const { container } = render(<CharacterSheet entityId="e1" />);
    // FormulaOverrides section should render with translated title
    expect(container.textContent).toContain('Formel-Overrides');
  });

  it('shows use button for active abilities', () => {
    (useSheet as any).mockReturnValue({ data: mockSheet, loading: false, error: null, refetch: vi.fn() });
    const { container } = render(<CharacterSheet entityId="e1" />);
    const buttons = container.querySelectorAll('button');
    // Should find buttons — active abilities get a use button
    expect(buttons.length).toBeGreaterThan(0);
  });
});
