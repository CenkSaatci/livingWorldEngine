import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { WorldMapView } from './WorldMapView';
import { apiClient } from '../../api/client';

vi.mock('../../api/client', () => ({
  apiClient: { get: vi.fn() },
  BACKEND_ORIGIN: 'http://localhost:8080',
}));

vi.mock('../../hooks/useApiGet', () => ({
  useApiGet: () => ({ data: [
    { id: 'r1', name: 'Waldmark', climate: 'temperate', danger_level: 3 },
    { id: 'r2', name: 'Düsterburg', climate: 'cold', danger_level: 8 },
  ] }),
}));

const mockLocations = [
  { id: 'l1', name: 'Dorf', type: 'village', positionJson: '{"x":100,"y":200}' },
  { id: 'l2', name: 'Turm', type: 'tower', positionJson: '{"x":300,"y":400}' },
  { id: 'l3', name: 'OhnePos', type: 'ruin' },
];

const mockWeather = { weatherType: 'CLEAR', temperature: 22, description: 'Clear sky' };

beforeEach(async () => {
  vi.clearAllMocks();
  (apiClient.get as any).mockImplementation((url: string) => {
    if (url.includes('/map')) return Promise.resolve({ data: { imageUrl: null } });
    if (url.includes('/weather')) return Promise.resolve({ data: mockWeather });
    if (url.includes('/locations')) return Promise.resolve({ data: mockLocations });
    return Promise.resolve({ data: [] });
  });
});

describe('WorldMapView', () => {
  it('renders zoom controls', () => {
    render(<WorldMapView worldId="w1" />);
    expect(screen.getByText('+')).toBeTruthy();
    expect(screen.getByText('\u2212')).toBeTruthy();
    expect(screen.getByText('\u27F2')).toBeTruthy();
  });

  it('renders region legend', async () => {
    render(<WorldMapView worldId="w1" />);
    const regions = await screen.findAllByText(/Waldmark|Düsterburg/);
    expect(regions.length).toBe(2);
  });

  it('renders location markers with position', async () => {
    render(<WorldMapView worldId="w1" />);
    await vi.waitFor(() => {
      expect(apiClient.get).toHaveBeenCalled();
    });
    const markers = await screen.findAllByTitle(/Dorf|Turm/);
    expect(markers.length).toBeGreaterThanOrEqual(2);
  });

  it('does not render location without positionJson', () => {
    render(<WorldMapView worldId="w1" />);
    expect(screen.queryByTitle(/OhnePos/)).toBeNull();
  });

  it('calls onSelectLocation when marker clicked', async () => {
    const onSelect = vi.fn();
    const { container } = render(<WorldMapView worldId="w1" onSelectLocation={onSelect} />);
    await vi.waitFor(() => {
      expect(container.textContent).toContain('Dorf');
    });
    const buttons = Array.from(container.querySelectorAll('button'));
    const dorfBtn = buttons.find(b => b.title === 'Dorf' || b.textContent?.includes('Dorf'));
    expect(dorfBtn).toBeTruthy();
    if (dorfBtn) {
      await userEvent.click(dorfBtn);
      expect(onSelect).toHaveBeenCalledWith('l1');
    }
  });
});
