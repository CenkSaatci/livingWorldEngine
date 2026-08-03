import { type ReactNode } from 'react';

export interface InventoryEntry {
  itemId: string;
  quantity: number;
  equipped: boolean;
  slot: string | null;
  name: string;
  type: string;
  weight: number;
}

interface Props {
  entry: InventoryEntry;
  onEquip?: (itemId: string, slot: string) => void;
  onUnequip?: (itemId: string) => void;
  children?: ReactNode;
}

export function ItemCard({ entry, onEquip, onUnequip, children }: Props) {
  const slotIcons: Record<string, string> = {
    weapon: '⚔️',
    armor: '🛡️',
    helmet: '⛑️',
    accessory: '💍',
  };

const ITEM_TYPE_TO_SLOT: Record<string, string> = {
    WEAPON: 'weapon',
    ARMOR: 'armor',
    HELMET: 'helmet',
    ACCESSORY: 'accessory',
  };

  return (
    <div
      className={`group relative rounded border px-3 py-2 text-sm transition ${
        entry.equipped ? 'border-accent/60 bg-accent/10' : 'border-bg-elevated bg-bg-surface'
      }`}
    >
      <div className="flex items-center gap-2">
        {entry.type === 'WEAPON' && <span>{slotIcons.weapon ?? '🗡️'}</span>}
        {entry.type === 'ARMOR' && <span>{slotIcons.armor ?? '🛡️'}</span>}

        <div className="flex-1">
          <span className="text-text-primary">{entry.name}</span>
          {entry.quantity > 1 && (
            <span className="ml-1.5 rounded bg-bg-elevated px-1.5 py-0.5 text-xs text-text-secondary">
              ×{entry.quantity}
            </span>
          )}
          <span className="ml-2 text-xs text-text-secondary">{entry.type}</span>
        </div>

        {entry.equipped && onUnequip ? (
          <button
            onClick={() => onUnequip(entry.itemId)}
            className="text-xs text-accent hover:text-accent/60"
          >
            unequip
          </button>
        ) : onEquip && !entry.equipped ? (
          <button
            onClick={() => onEquip(entry.itemId, ITEM_TYPE_TO_SLOT[entry.type] ?? 'weapon')}
            className="text-xs text-accent hover:text-accent/60"
          >
            equip
          </button>
        ) : null}
      </div>

      {/* Tooltip */}
      <div className="invisible absolute left-0 top-full z-10 mt-1 w-56 rounded-lg border border-bg-elevated bg-bg-elevated p-3 shadow-xl group-hover:visible">
        <p className="mb-1 font-heading text-text-primary">{entry.name}</p>
        <p className="mb-1 text-xs text-text-secondary">Type: {entry.type}</p>
        <p className="text-xs text-text-secondary">Weight: {Number(entry.weight).toFixed(1)} kg</p>
      </div>

      {children}
    </div>
  );
}
