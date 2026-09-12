import { useState } from 'react';
import { X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { apiClient } from '../../api/client';

const LOCATION_TYPES = [
  'village',
  'town',
  'city',
  'castle',
  'dungeon',
  'ruin',
  'temple',
  'tower',
  'inn',
  'camp',
  'shrine',
  'cave',
];

interface Props {
  regionId: string;
  regionName: string;
  onCreated: () => void;
  onClose: () => void;
}

export function LocationCreateModal({ regionId, regionName, onCreated, onClose }: Props) {
  const { t } = useTranslation('common');
  const [name, setName] = useState('');
  const [type, setType] = useState('village');
  const [wealth, setWealth] = useState(5);
  const [population, setPopulation] = useState(100);
  const [positionX, setPositionX] = useState(0);
  const [positionY, setPositionY] = useState(0);
  const [description, setDescription] = useState('');
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    if (!name.trim()) return;
    setSaving(true);
    try {
      await apiClient.post(`/regions/${regionId}/locations`, {
        type,
        name: name.trim(),
        wealth,
        population,
        positionJson: JSON.stringify({ x: positionX, y: positionY }),
        description: description || null,
      });
      onCreated();
      onClose();
    } catch {
      /* */
    } finally {
      setSaving(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
      onClick={onClose}
    >
      <div
        className="w-full max-w-md rounded-xl border border-bg-elevated bg-bg-surface p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-heading text-text-primary">
            {t('location.create')}{' '}
            <span className="text-text-secondary text-sm">in {regionName}</span>
          </h2>
          <button onClick={onClose} className="text-text-secondary hover:text-text-primary">
            <X size={20} />
          </button>
        </div>

        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="col-span-2">
              <label className="block text-xs text-text-secondary mb-1">{t('location.name')}</label>
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                autoFocus
                className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
              />
            </div>
            <div>
              <label className="block text-xs text-text-secondary mb-1">{t('location.type')}</label>
              <select
                value={type}
                onChange={(e) => setType(e.target.value)}
                className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
              >
                {LOCATION_TYPES.map((t) => (
                  <option key={t} value={t} className="capitalize">
                    {t}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs text-text-secondary mb-1">
                {t('location.wealth')}
              </label>
              <input
                type="number"
                min={1}
                max={10}
                value={wealth}
                onChange={(e) => setWealth(Number(e.target.value))}
                className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
              />
            </div>
            <div className="col-span-2">
              <label className="block text-xs text-text-secondary mb-1">
                {t('location.population')}
              </label>
              <input
                type="number"
                min={0}
                value={population}
                onChange={(e) => setPopulation(Number(e.target.value))}
                className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('location.mapX')}</label>
                <input
                  type="number"
                  value={positionX}
                  onChange={(e) => setPositionX(Number(e.target.value))}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('location.mapY')}</label>
                <input
                  type="number"
                  value={positionY}
                  onChange={(e) => setPositionY(Number(e.target.value))}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
            </div>
          </div>

          <div>
            <label className="block text-xs text-text-secondary mb-1">
              {t('location.description')}
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent resize-none"
            />
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button
              onClick={onClose}
              className="rounded border border-bg-elevated px-4 py-2 text-sm text-text-secondary hover:text-text-primary"
            >
              {t('actions.cancel')}
            </button>
            <button
              onClick={handleSave}
              disabled={saving || !name.trim()}
              className="rounded bg-accent px-4 py-2 text-sm text-white hover:bg-accent/80 disabled:opacity-40"
            >
              {saving ? 'Creating…' : t('location.create')}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
