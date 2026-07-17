import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { ItemCard } from './ItemCard';

describe('ItemCard', () => {
  const baseEntry = {
    itemId: '1',
    quantity: 1,
    equipped: false,
    slot: null,
    name: 'Short Sword',
    type: 'WEAPON',
    weight: 2,
  };

  it('renders item name and type', () => {
    const { container } = render(<ItemCard entry={baseEntry} />);
    expect(container.textContent).toContain('Short Sword');
    expect(screen.getByText('WEAPON')).toBeInTheDocument();
  });

  it('shows quantity when > 1', () => {
    render(<ItemCard entry={{ ...baseEntry, quantity: 3 }} />);
    expect(screen.getByText('×3')).toBeInTheDocument();
  });

  it('shows equip button when not equipped and onEquip provided', () => {
    render(<ItemCard entry={baseEntry} onEquip={vi.fn()} />);
    expect(screen.getByText('equip')).toBeInTheDocument();
  });

  it('shows unequip button when equipped', () => {
    render(
      <ItemCard entry={{ ...baseEntry, equipped: true, slot: 'weapon' }} onUnequip={vi.fn()} />,
    );
    expect(screen.getByText('unequip')).toBeInTheDocument();
  });

  it('calls onEquip on button click', async () => {
    const onEquip = vi.fn();
    const user = userEvent.setup();
    render(<ItemCard entry={baseEntry} onEquip={onEquip} />);
    await user.click(screen.getByText('equip'));
    expect(onEquip).toHaveBeenCalledWith('1', 'weapon');
  });
});
