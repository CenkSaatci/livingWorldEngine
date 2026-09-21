import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, fireEvent, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import MapEditorPage from './MapEditorPage';
import { apiClient } from '../api/client';
import { useToastStore } from '../store/toastStore';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}));

vi.mock('../api/client', () => ({
  apiClient: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  BACKEND_ORIGIN: 'http://localhost:8080',
}));

vi.mock('../components/map/usePixiApp', () => ({
  usePixiApp: () => ({ getApp: () => null, getViewport: () => ({ x: 0, y: 0, zoom: 1 }) }),
}));

vi.mock('../components/map/Grid', () => ({ drawGrid: vi.fn() }));

const REGIONS = [{ id: 'r1', name: 'Waldmark', polygonPoints: '[{"x":0,"y":0},{"x":10,"y":0},{"x":0,"y":10}]' }];
const LOCATIONS = [{ id: 'l1', regionId: 'r1', name: 'Dorf', type: 'village', positionJson: '{"x":5,"y":5}' }];

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/worlds/w1/map']}>
      <Routes>
        <Route path="/worlds/:id/map" element={<MapEditorPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  useToastStore.setState({ toasts: [] });
  vi.mocked(apiClient.get).mockImplementation((url: string) => {
    if (url === '/worlds/w1/regions') return Promise.resolve({ data: REGIONS });
    if (url === '/worlds/w1/map') return Promise.resolve({ data: { imageUrl: null } });
    if (url === '/regions/r1/locations') return Promise.resolve({ data: LOCATIONS });
    return Promise.resolve({ data: [] });
  });
});

describe('MapEditorPage', () => {
  it('zeigt Regionen und POIs mit Icon', async () => {
    renderPage();

    expect(await screen.findByText('Waldmark')).toBeInTheDocument();
    expect(await screen.findByText('Dorf')).toBeInTheDocument();
    expect(screen.getByText('🏡')).toBeInTheDocument();
  });

  it('bearbeitet einen POI per PATCH', async () => {
    renderPage();
    await screen.findByText('Dorf');

    fireEvent.click(screen.getAllByLabelText('editor.edit')[0]);
    const nameInput = screen.getByDisplayValue('Dorf');
    fireEvent.change(nameInput, { target: { value: 'Großdorf' } });
    fireEvent.click(screen.getByText('editor.apply'));

    await vi.waitFor(() => {
      expect(apiClient.patch).toHaveBeenCalledWith('/regions/r1/locations/l1',
        expect.objectContaining({ name: 'Großdorf', type: 'village' }));
    });
  });

  it('löscht einen POI nach Bestätigung', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    renderPage();
    await screen.findByText('Dorf');

    // Regionen werden vor Orten gerendert -> index 1 ist der POI-Delete.
    fireEvent.click(screen.getAllByLabelText('editor.delete')[1]);

    await vi.waitFor(() => {
      expect(apiClient.delete).toHaveBeenCalledWith('/regions/r1/locations/l1');
    });
  });

  it('löscht eine Region nach Bestätigung', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    renderPage();
    await screen.findByText('Waldmark');

    fireEvent.click(screen.getAllByLabelText('editor.delete')[0]);

    await vi.waitFor(() => {
      expect(apiClient.delete).toHaveBeenCalledWith('/worlds/w1/regions/r1');
    });
  });

  it('benennt eine Region um', async () => {
    vi.spyOn(window, 'prompt').mockReturnValue('Düsterwald');
    renderPage();
    const row = (await screen.findByText('Waldmark')).closest('div')!;
    fireEvent.click(within(row.parentElement as HTMLElement).getByLabelText('editor.rename'));

    await vi.waitFor(() => {
      expect(apiClient.patch).toHaveBeenCalledWith('/worlds/w1/regions/r1', { name: 'Düsterwald' });
    });
  });

  it('lehnt Nicht-Bilder beim Karten-Upload ab (kein POST)', async () => {
    const { container } = renderPage();
    await screen.findByText('Waldmark');

    const input = container.querySelector('input[type="file"]') as HTMLInputElement;
    fireEvent.change(input, { target: { files: [new File(['x'], 'karte.txt', { type: 'text/plain' })] } });

    expect(apiClient.post).not.toHaveBeenCalled();
  });

  it('lädt ein gültiges Kartenbild hoch', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { imageUrl: '/files/map.png' } } as never);
    const { container } = renderPage();
    await screen.findByText('Waldmark');

    const input = container.querySelector('input[type="file"]') as HTMLInputElement;
    fireEvent.change(input, { target: { files: [new File(['img'], 'karte.png', { type: 'image/png' })] } });

    await vi.waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith('/worlds/w1/map/upload', expect.any(FormData));
    });
  });
});
