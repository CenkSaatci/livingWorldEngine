import { useDraggable } from '@dnd-kit/core';
import { GripVertical } from 'lucide-react';
import { type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

interface Props {
  id: string;
  children: ReactNode;
}

/**
 * Drag-Wrapper für Inventar-Items.
 *
 * Nur der Griff (Grip-Icon) ist der Drag-Aktivator — der Rest der Karte ist
 * normales DOM. Das ist Absicht: Wären die Listener auf dem gesamten Wrapper,
 * würden Tastatur-Events (Enter/Space) und Klicks auf innere Buttons
 * (equip/unequip) mit dem Drag-Verhalten kollidieren (dnd-kit Keyboard-Sensor,
 * verschachtelte interaktive Elemente).
 */
export function DraggableItem({ id, children }: Props) {
  const { t } = useTranslation('character');
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({ id });

  const style = transform
    ? { transform: `translate(${transform.x}px, ${transform.y}px)`, zIndex: 50 }
    : undefined;

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`flex items-center gap-1 ${isDragging ? 'opacity-50' : ''}`}
    >
      <span
        {...listeners}
        {...attributes}
        className="cursor-grab touch-none text-text-secondary hover:text-accent active:cursor-grabbing"
        aria-label={t('sheet.dragToEquip')}
        title={t('sheet.dragToEquip')}
      >
        <GripVertical size={14} />
      </span>
      <div className="flex-1">{children}</div>
    </div>
  );
}
