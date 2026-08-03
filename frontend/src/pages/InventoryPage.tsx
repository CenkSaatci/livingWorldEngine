import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft } from 'lucide-react';
import { DndContext, DragOverlay, type DragEndEvent } from '@dnd-kit/core';
import { apiClient } from '../api/client';
import { ItemCard, type InventoryEntry } from '../components/inventory/ItemCard';
import { DraggableItem } from '../components/inventory/DraggableItem';
import { DroppableSlot } from '../components/inventory/DroppableSlot';
import { useApiGet } from '../hooks/useApiGet';
import { useToast } from '../hooks/useToast';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';

const SLOTS = ['weapon', 'armor', 'helmet', 'accessory'];

export default function InventoryPage() {
  const { t } = useTranslation('character');
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [dragTarget, setDragTarget] = useState<string | null>(null);
  const toast = useToast();

  const { data, loading, refetch } = useApiGet<{
    items: InventoryEntry[];
    computedBonuses: Record<string, number>;
  }>(`/entities/${id}/inventory`, [id]);

  const items = data?.items ?? [];
  const bonuses = data?.computedBonuses ?? {};

  const handleEquip = async (itemId: string, slot: string) => {
    if (!id) return;
    try {
      await apiClient.post(`/entities/${id}/inventory/equip`, { itemId, slot });
      refetch();
    } catch {
      toast.error('Equip failed');
    }
  };

  const handleUnequip = async (itemId: string) => {
    if (!id) return;
    try {
      await apiClient.post(`/entities/${id}/inventory/unequip`, { itemId });
      refetch();
    } catch {
      toast.error('Unequip failed');
    }
  };

  const handleDragEnd = (event: DragEndEvent) => {
    setDragTarget(null);
    const { active, over } = event;
    if (!active || !over) return;
    const itemId = active.id as string;
    const slot = over.id as string;
    if (SLOTS.includes(slot)) handleEquip(itemId, slot);
  };

  if (loading)
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg-primary">
        <LoadingSpinner />
      </div>
    );

  const equipped = items.filter((i) => i.equipped);
  const backpack = items.filter((i) => !i.equipped);
  const equippedBySlot = Object.fromEntries(
    SLOTS.map((s) => [s, equipped.find((i) => i.slot === s)]),
  );

  return (
    <DndContext onDragEnd={handleDragEnd} onDragStart={(e) => setDragTarget(e.active.id as string)}>
      <div className="min-h-screen bg-bg-primary">
        <header className="flex items-center gap-3 border-b border-bg-elevated bg-bg-surface px-6 py-3">
          <button onClick={() => navigate(-1)} className="text-text-secondary hover:text-accent">
            <ArrowLeft size={20} />
          </button>
          <h1 className="text-lg font-heading text-text-primary">{t('sheet.inventory')}</h1>
        </header>

        <main className="mx-auto max-w-3xl space-y-6 p-6">
          {/* Equip Slots */}
          <section className="rounded-lg bg-bg-surface p-5">
            <h2 className="mb-3 font-heading text-text-primary">{t("sheet.equipped")}</h2>
            <div className="grid grid-cols-2 gap-3">
              {SLOTS.map((slot) => {
                const item = equippedBySlot[slot];
                return (
                  <DroppableSlot key={slot} id={slot} isEmpty={!item} isDragActive={!!dragTarget}>
                    {item ? (
                      <DraggableItem id={item.itemId}>
                        <ItemCard entry={item} onUnequip={() => handleUnequip(item.itemId)} />
                      </DraggableItem>
                    ) : (
                      <span className="text-xs text-text-secondary capitalize italic">{slot}</span>
                    )}
                  </DroppableSlot>
                );
              })}
            </div>
          </section>

          {/* Bonuses */}
          {Object.keys(bonuses).length > 0 && (
            <section className="rounded-lg bg-bg-surface p-5">
              <h2 className="mb-3 font-heading text-text-primary">{t("sheet.activeBonuses")}</h2>
              <div className="flex flex-wrap gap-2">
                {Object.entries(bonuses).map(([key, val]) => (
                  <span key={key} className="rounded bg-accent/15 px-2 py-1 text-xs text-accent">
                    {key}: +{val}
                  </span>
                ))}
              </div>
            </section>
          )}

          {/* Backpack */}
          <section className="rounded-lg bg-bg-surface p-5">
            <h2 className="mb-3 font-heading text-text-primary">{t("sheet.inventoryCount", { count: backpack.length })}</h2>
            {backpack.length === 0 ? (
              <p className="text-sm text-text-secondary">{t("sheet.empty")}</p>
            ) : (
              <ul className="space-y-2" role="list" aria-label="Backpack items">
                {backpack.map((item) => (
                  <li key={item.itemId}>
                    <DraggableItem id={item.itemId}>
                      <ItemCard entry={item} />
                    </DraggableItem>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </main>
      </div>

      <DragOverlay>
        {dragTarget && items.find((i) => i.itemId === dragTarget) && (
          <div className="w-64 rounded border border-accent bg-bg-surface p-3 shadow-xl">
            <p className="text-sm text-text-primary">
              {items.find((i) => i.itemId === dragTarget)!.name}
            </p>
          </div>
        )}
      </DragOverlay>
    </DndContext>
  );
}
