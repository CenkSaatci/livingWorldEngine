import { useDroppable } from '@dnd-kit/core';
import { type ReactNode } from 'react';

interface Props {
  id: string;
  children: ReactNode;
  isEmpty: boolean;
  isDragActive: boolean;
}

export function DroppableSlot({ id, children, isEmpty, isDragActive }: Props) {
  const { setNodeRef, isOver } = useDroppable({ id });

  return (
    <div
      ref={setNodeRef}
      className={`flex min-h-[60px] items-center gap-3 rounded border-2 px-3 py-2 transition
        ${isOver ? 'border-accent bg-accent/10' : ''}
        ${!isOver && isDragActive && isEmpty ? 'border-accent/50 bg-accent/5' : ''}
        ${!isOver && !isEmpty ? 'border-accent/60 bg-accent/10' : ''}
        ${!isOver && isEmpty && !isDragActive ? 'border-dashed border-bg-elevated' : ''}`}
    >
      {children}
    </div>
  );
}
