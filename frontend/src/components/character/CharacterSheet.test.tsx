import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render } from '@testing-library/react';
import { CharacterSheet } from './CharacterSheet';

vi.mock('../../api/client', () => ({
  apiClient: { get: vi.fn(), patch: vi.fn() },
}));

vi.mock('../../hooks/useSheet', () => ({
  useSheet: vi.fn(),
}));

import { useSheet } from '../../hooks/useSheet';

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
    { name: 'Athletik', total: 4 },
    { name: 'Wahrnehmung', total: 2 },
  ],
  conditionals: [
    { name: 'Stark', active: true, description: '+2 auf schaden' },
    { name: 'Zäh', active: false, description: '' },
  ],
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe('CharacterSheet', () => {
  it('shows loading state', () => {
    (useSheet as any).mockReturnValue({ data: null, loading: true, error: null });
    const { container } = render(<CharacterSheet entityId="e1" worldId="" />);
    expect(container.textContent).toBeTruthy();
  });

  it('shows error state', () => {
    (useSheet as any).mockReturnValue({ data: null, loading: false, error: 'Fehler' });
    const { container } = render(<CharacterSheet entityId="e1" worldId="" />);
    expect(container.textContent).toContain('Fehler');
  });

  it('renders entity name', () => {
    (useSheet as any).mockReturnValue({ data: mockSheet, loading: false, error: null, refetch: vi.fn() });
    const { container } = render(<CharacterSheet entityId="e1" worldId="" />);
    expect(container.textContent).toContain('Held');
  });

  it('renders attributes with values', () => {
    (useSheet as any).mockReturnValue({ data: mockSheet, loading: false, error: null, refetch: vi.fn() });
    const { container } = render(<CharacterSheet entityId="e1" worldId="" />);
    expect(container.textContent).toContain('15');
    expect(container.textContent).toContain('12');
    expect(container.textContent).toContain('+2');
  });

  it('renders derived values', () => {
    (useSheet as any).mockReturnValue({ data: mockSheet, loading: false, error: null, refetch: vi.fn() });
    const { container } = render(<CharacterSheet entityId="e1" worldId="" />);
    expect(container.textContent).toContain('hp');
    expect(container.textContent).toContain('25');
  });

  it('renders skills', () => {
    (useSheet as any).mockReturnValue({ data: mockSheet, loading: false, error: null, refetch: vi.fn() });
    const { container } = render(<CharacterSheet entityId="e1" worldId="" />);
    expect(container.textContent).toContain('Athletik');
  });

  it('renders active conditionals', () => {
    (useSheet as any).mockReturnValue({ data: mockSheet, loading: false, error: null, refetch: vi.fn() });
    const { container } = render(<CharacterSheet entityId="e1" worldId="" />);
    expect(container.textContent).toContain('Stark');
  });

  it('shows XP', () => {
    (useSheet as any).mockReturnValue({ data: mockSheet, loading: false, error: null, refetch: vi.fn() });
    const { container } = render(<CharacterSheet entityId="e1" worldId="" />);
    expect(container.textContent).toContain('1500');
  });
});
