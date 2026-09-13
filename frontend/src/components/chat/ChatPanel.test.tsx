import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ChatPanel } from './ChatPanel';
import { apiClient } from '../../api/client';
import { useWorldStore } from '../../store/worldStore';

vi.mock('../../api/client', () => ({
  apiClient: { get: vi.fn(() => Promise.resolve({ data: [] })), post: vi.fn(() => Promise.resolve({})) },
}));

const mockedGet = vi.mocked(apiClient.get);

// jsdom kennt scrollIntoView nicht
Element.prototype.scrollIntoView = vi.fn();

describe('ChatPanel (B5/R3)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useWorldStore.setState({ worldEvents: [] });
  });

  it('lädt die Historie beim Betreten', async () => {
    mockedGet.mockResolvedValue({
      data: [
        { sender: 'Lysander', text: 'Historien-Test', timestamp: '2026-09-12T10:00:00Z' },
        { sender: 'Brinja', text: 'Antwort', timestamp: '2026-09-12T10:01:00Z' },
      ],
    } as never);

    render(<ChatPanel worldId="w1" />);

    expect(await screen.findByText('Historien-Test')).toBeInTheDocument();
    expect(screen.getByText('Antwort')).toBeInTheDocument();
    expect(mockedGet).toHaveBeenCalledWith('/chat/w1');
  });

  it('Echo-vor-POST: Broadcast vor POST-Response erzeugt kein Duplikat (QA-Audit)', async () => {
    const post = vi.mocked(apiClient.post);
    let resolvePost!: (v: unknown) => void;
    post.mockImplementationOnce(() => new Promise((res) => { resolvePost = res; }));
    const { container } = render(<ChatPanel worldId="w1" />);
    const input = container.querySelector('[data-chat-input]') as HTMLInputElement;
    expect(input).not.toBeNull();
    fireEvent.change(input, { target: { value: 'Hallo QA' } });
    fireEvent.keyDown(input, { key: 'Enter', code: 'Enter' });
    // Echo ist sofort da (vor POST-Response)
    await screen.findByText('Hallo QA');
    // Broadcast gewinnt das Rennen (POST noch offen)
    useWorldStore.setState({
      worldEvents: [
        {
          event_type: 'CHAT_MESSAGE',
          payload: { sender: 'You', text: 'Hallo QA', timestamp: new Date().toISOString() },
          created_at: new Date().toISOString(),
        } as never,
      ],
    });
    resolvePost({});
    await waitFor(() => expect(post).toHaveBeenCalled());
    expect(screen.getAllByText('Hallo QA')).toHaveLength(1);
  });

  it('zeigt keine Roh-JSON-Systemevents im Chat (Playtest #11)', async () => {
    useWorldStore.setState({
      worldEvents: [
        {
          event_type: 'COMBAT_ACTION_EXECUTED',
          payload: { actionType: 'ACTION', damage: 5 },
          created_at: '2026-09-12T10:02:00Z',
        } as never,
      ],
    });

    render(<ChatPanel worldId="w1" />);

    // Footer (Eingabe) gerendert — kein Roh-JSON-Event sichtbar
    await screen.findByRole('log');
    expect(screen.queryByText(/ACTION.*damage/)).not.toBeInTheDocument();
  });
});
