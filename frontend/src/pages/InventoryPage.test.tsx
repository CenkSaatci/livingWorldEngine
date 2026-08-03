import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import InventoryPage from './InventoryPage';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}));

vi.mock('../api/client', () => ({
  apiClient: { get: vi.fn(), post: vi.fn() },
}));

vi.mock('../hooks/useApiGet', () => ({
  useApiGet: vi.fn(),
}));

vi.mock('../hooks/useToast', () => ({
  useToast: () => ({ error: vi.fn() }),
}));

vi.mock('@dnd-kit/core', () => ({
  DndContext: ({ children }: any) => children,
  DragOverlay: ({ children }: any) => children,
  useDraggable: () => ({ attributes: {}, listeners: {}, setNodeRef: () => {}, transform: null, isDragging: false }),
  useDroppable: () => ({ setNodeRef: () => {}, isOver: false }),
}));

import { useApiGet } from '../hooks/useApiGet';

const defaultData = {
  items: [],
  computedBonuses: {},
};

beforeEach(() => {
  vi.clearAllMocks();
  (useApiGet as any).mockReturnValue({ data: defaultData, loading: false, refetch: vi.fn() });
});

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/inventory/e1']}>
      <Routes>
        <Route path="/inventory/:id" element={<InventoryPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('InventoryPage', () => {
  it('renders without crashing in loading state', () => {
    (useApiGet as any).mockReturnValue({ data: null, loading: true, refetch: vi.fn() });
    expect(() => renderPage()).not.toThrow();
  });

  it('shows empty state', () => {
    const { container } = renderPage();
    expect(container.textContent).toContain('sheet.inventory');
  });

  it('renders equipped section', () => {
    (useApiGet as any).mockReturnValue({
      data: {
        items: [{ itemId: 'i1', name: 'Schwert', type: 'WEAPON', quantity: 1, equipped: true, slot: 'weapon', weight: 2 }],
        computedBonuses: { staerke: 2 },
      },
      loading: false,
      refetch: vi.fn(),
    });
    const { container } = renderPage();
    expect(container.textContent).toContain('Schwert');
    expect(container.textContent).toContain('staerke: +2');
  });
});
