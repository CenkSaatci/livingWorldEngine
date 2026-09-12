import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { TradeModal } from './TradeModal';
import { apiClient } from '../../api/client';

vi.mock('../../api/client', () => ({
  apiClient: { get: vi.fn(), post: vi.fn(() => Promise.resolve({ data: {} })) },
}));

const mockedGet = vi.mocked(apiClient.get);
const mockedPost = vi.mocked(apiClient.post);

const ME = 'me-1';
const PARTNER = 'partner-2';
const SWORD = 'item-sword';
const POTION = 'item-potion';

function baseGet(url: string) {
  if (url === `/entities/${ME}`) return Promise.resolve({ data: { worldId: 'w1' } } as never);
  if (url === '/worlds/w1/entities')
    return Promise.resolve({
      data: [
        { id: ME, name: 'Lysander', entityType: 'PC' },
        { id: PARTNER, name: 'Brinja', entityType: 'PC' },
      ],
    } as never);
  if (url === `/entities/${ME}/inventory`)
    return Promise.resolve({ data: { items: [{ itemId: SWORD, name: 'Kurzschwert', quantity: 1 }] } } as never);
  if (url === `/entities/${PARTNER}/inventory`)
    return Promise.resolve({ data: { items: [{ itemId: POTION, name: 'Heiltrank', quantity: 2 }] } } as never);
  return null;
}

describe('TradeModal (B4/R3)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGet.mockImplementation((url: string) => {
      const base = baseGet(url);
      if (base) return base;
      if (url.startsWith('/trades?')) return Promise.resolve({ data: [] } as never);
      return Promise.reject(new Error('unexpected ' + url));
    });
  });

  it('sendet Angebot mit Item-Listen an POST /trades', async () => {
    render(<TradeModal entityId={ME} entityName="Lysander" onClose={() => {}} />);

    fireEvent.change(await screen.findByRole('combobox'), { target: { value: PARTNER } });
    await screen.findByText('Kurzschwert (1)');
    await screen.findByText('Heiltrank (2)');

    const plus = screen.getAllByRole('button', { name: /\+$/ });
    fireEvent.click(plus[0]); // 1x Kurzschwert geben
    fireEvent.click(plus[1]); // 1x Heiltrank wollen
    fireEvent.click(screen.getByRole('button', { name: /Angebot senden/ }));

    await waitFor(() =>
      expect(mockedPost).toHaveBeenCalledWith('/trades', expect.objectContaining({
        worldId: 'w1',
        proposerEntityId: ME,
        partnerEntityId: PARTNER,
        offer: [{ itemId: SWORD, quantity: 1 }],
        request: [{ itemId: POTION, quantity: 1 }],
      })),
    );
  });

  it('zeigt annehmbares Angebot und akzeptiert es', async () => {
    mockedGet.mockImplementation((url: string) => {
      if (url.startsWith('/trades?'))
        return Promise.resolve({
          data: [{
            id: 't1',
            proposerEntityId: PARTNER,
            partnerEntityId: ME,
            offerJson: '[]',
            requestJson: '[]',
            status: 'proposed',
            lastEditorEntityId: PARTNER,
            offer: [{ itemId: SWORD, quantity: 1, name: 'Kurzschwert' }],
            request: [],
          }],
        } as never);
      const base = baseGet(url);
      if (base) return base;
      return Promise.reject(new Error('unexpected ' + url));
    });

    render(<TradeModal entityId={ME} entityName="Lysander" onClose={() => {}} />);
    await screen.findByText(/Annehmen/);
    fireEvent.click(screen.getByRole('button', { name: /Annehmen/ }));

    await waitFor(() =>
      expect(mockedPost).toHaveBeenCalledWith('/trades/t1/accept', { entityId: ME }),
    );
  });
});
