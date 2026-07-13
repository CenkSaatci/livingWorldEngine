import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft } from 'lucide-react';
import { apiClient } from '../api/client';
import { ItemCard, type InventoryEntry } from '../components/inventory/ItemCard';

export default function InventoryPage() {
  const { t } = useTranslation('character');
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [items, setItems] = useState<InventoryEntry[]>([]);
  const [bonuses, setBonuses] = useState<Record<string, number>>({});

  useEffect(() => {
    if (!id) return;
    apiClient.get(`/entities/${id}/inventory`).then((res) => {
      setItems(res.data.items ?? []);
      setBonuses(res.data.computed_bonuses ?? {});
    });
  }, [id]);

  const handleEquip = async (itemId: string, slot: string) => {
    if (!id) return;
    const res = await apiClient.post(`/entities/${id}/inventory/equip`, { itemId, slot });
    setItems(res.data.items ?? []);
    setBonuses(res.data.computed_bonuses ?? {});
  };

  const handleUnequip = async (_itemId: string) => {
    if (!id) return;
    const res = await apiClient.post(`/entities/${id}/inventory/unequip`, { slot: 'weapon' });
    setItems(res.data.items ?? []);
    setBonuses(res.data.computed_bonuses ?? {});
  };

  const equipped = items.filter((i) => i.equipped);
  const backpack = items.filter((i) => !i.equipped);

  return (
    <div className="min-h-screen bg-bg-primary">
      <header className="flex items-center gap-3 border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <button onClick={() => navigate(-1)} className="text-text-secondary hover:text-accent">
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-lg font-heading text-text-primary">{t('sheet.inventory')}</h1>
      </header>

      <main className="mx-auto max-w-3xl space-y-6 p-6">
        <section className="rounded-lg bg-bg-surface p-5">
          <h2 className="mb-3 font-heading text-text-primary">Equipped</h2>
          {equipped.length === 0 ? (
            <p className="text-sm text-text-secondary">No items equipped</p>
          ) : (
            <div className="space-y-2">
              {equipped.map((e) => (
                <ItemCard key={e.itemId} entry={e} onUnequip={handleUnequip} />
              ))}
            </div>
          )}
        </section>

        {Object.keys(bonuses).length > 0 && (
          <section className="rounded-lg bg-bg-surface p-5">
            <h2 className="mb-3 font-heading text-text-primary">Active Bonuses</h2>
            <div className="flex flex-wrap gap-2">
              {Object.entries(bonuses).map(([key, val]) => (
                <span key={key} className="rounded bg-accent/15 px-2 py-1 text-xs text-accent">
                  {key}: +{val}
                </span>
              ))}
            </div>
          </section>
        )}

        <section className="rounded-lg bg-bg-surface p-5">
          <h2 className="mb-3 font-heading text-text-primary">
            Inventory ({backpack.length})
          </h2>
          {backpack.length === 0 ? (
            <p className="text-sm text-text-secondary">Empty</p>
          ) : (
            <div className="space-y-2">
              {backpack.map((item) => (
                <ItemCard key={item.itemId} entry={item} onEquip={handleEquip} />
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
