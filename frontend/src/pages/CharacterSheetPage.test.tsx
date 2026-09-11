import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import CharacterSheetPage from './CharacterSheetPage';
import { useToastStore } from '../store/toastStore';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}));

vi.mock('../api/client', () => ({
  apiClient: { get: vi.fn(), post: vi.fn() },
}));

vi.mock('../components/character/CharacterSheet', () => ({
  CharacterSheet: () => <div>character-sheet</div>,
}));

import { apiClient } from '../api/client';

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/characters/e1']}>
      <Routes>
        <Route path="/characters/:id" element={<CharacterSheetPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  useToastStore.setState({ toasts: [] });
});

describe('CharacterSheetPage', () => {
  it('shows error toast when export fails', async () => {
    (apiClient.get as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('boom'));
    const { getByText } = renderPage();

    fireEvent.click(getByText('sheet.export'));
    await Promise.resolve();

    expect(useToastStore.getState().toasts.some((t) => t.message === 'Export failed')).toBe(true);
  });

  it('shows error toast when import file is invalid JSON', async () => {
    const { container } = renderPage();
    const input = container.querySelector('input[type="file"]') as HTMLInputElement;
    const file = { text: vi.fn().mockResolvedValue('not json') };

    fireEvent.change(input, { target: { files: [file] } });
    await Promise.resolve();
    await Promise.resolve();

    expect(useToastStore.getState().toasts.some((t) => t.message === 'Import failed')).toBe(true);
    expect(apiClient.post).not.toHaveBeenCalled();
  });

  it('shows error toast when import file has no worldId', async () => {
    const { container } = renderPage();
    const input = container.querySelector('input[type="file"]') as HTMLInputElement;
    const file = { text: vi.fn().mockResolvedValue(JSON.stringify({ name: 'Hero' })) };

    fireEvent.change(input, { target: { files: [file] } });
    await Promise.resolve();
    await Promise.resolve();

    expect(
      useToastStore.getState().toasts.some((t) => t.message === 'No worldId in import file'),
    ).toBe(true);
    expect(apiClient.post).not.toHaveBeenCalled();
  });
});
