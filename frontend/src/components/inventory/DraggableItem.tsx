import { useDraggable } from '@dnd-kit/core';
import { type ReactNode } from 'react';

interface Props {
  id: string;
  children: ReactNode;
}

export function DraggableItem({ id, children }: Props) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({ id });

  const style = transform
    ? { transform: `translate(${transform.x}px, ${transform.y}px)`, zIndex: 50 }
    : undefined;

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...listeners}
      {...attributes}
      className={`${isDragging ? 'opacity-50' : 'cursor-grab active:cursor-grabbing'}`}
    >
      {children}
    </div>
  );
}
