import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, MapPin } from 'lucide-react';
import { apiClient } from '../api/client';
import { LocationDetail, type LocationData } from '../components/world/LocationDetail';
import { EntityTimeline } from '../components/world/EntityTimeline';

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

export default function LocationViewPage() {
  const { id, locationId } = useParams<{ id: string; locationId: string }>();
  const navigate = useNavigate();
  const worldId = id ?? '';

  const [locationData, setLocationData] = useState<LocationData | null>(null);
  const [regionId, setRegionId] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [showDelete, setShowDelete] = useState(false);
  const [editName, setEditName] = useState('');
  const [editType, setEditType] = useState('village');
  const [editWealth, setEditWealth] = useState(5);
  const [editPopulation, setEditPopulation] = useState(100);
  const [saving, setSaving] = useState(false);

  const handleLocationLoad = (loc: LocationData) => {
    setLocationData(loc);
    if (loc.regionId) setRegionId(loc.regionId);
  };

  const openEdit = () => {
    if (!locationData) return;
    setEditName(locationData.name);
    setEditType(locationData.type);
    setEditWealth(locationData.wealth);
    setEditPopulation(locationData.population);
    setEditing(true);
  };

  const handleEdit = async () => {
    if (!regionId || !locationId || !editName.trim()) return;
    setSaving(true);
    try {
      await apiClient.patch(`/regions/${regionId}/locations/${locationId}`, {
        name: editName.trim(),
        type: editType,
        wealth: editWealth,
        population: editPopulation,
      });
      setEditing(false);
      window.location.reload();
    } catch {
      /* */
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!regionId || !locationId) return;
    try {
      await apiClient.delete(`/regions/${regionId}/locations/${locationId}`);
      navigate(-1);
    } catch {
      /* */
    }
  };

  const canEdit = !!regionId;

  return (
    <div className="min-h-screen bg-bg-primary">
      <header className="flex items-center gap-3 border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <button onClick={() => navigate(-1)} className="text-text-secondary hover:text-accent">
          <ArrowLeft size={20} />
        </button>
        <h1 className="flex items-center gap-2 text-lg font-heading text-text-primary">
          <MapPin size={20} className="text-accent" /> Location
        </h1>
        {canEdit && (
          <>
            <button onClick={openEdit} className="text-xs text-accent hover:text-accent/60 ml-auto">
              Edit
            </button>
            <button
              onClick={() => setShowDelete(true)}
              className="text-xs text-danger hover:text-danger/60"
            >
              Delete
            </button>
          </>
        )}
      </header>

      <main className="mx-auto max-w-4xl grid grid-cols-1 gap-6 p-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <LocationDetail
            locationId={locationId ?? ''}
            worldId={worldId}
            onLocationLoad={handleLocationLoad}
          />
        </div>
        <div className="space-y-6">
          <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
            <EntityTimeline entityType="location" entityId={locationId ?? ''} />
          </section>
        </div>
      </main>

      {/* Edit Modal */}
      {editing && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
          onClick={() => setEditing(false)}
        >
          <div
            className="w-80 rounded-xl border border-bg-elevated bg-bg-surface p-5 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="font-heading text-text-primary mb-4">Edit Location</h3>
            <div className="space-y-3">
              <div>
                <label className="block text-xs text-text-secondary mb-1">Name</label>
                <input
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">Type</label>
                <select
                  value={editType}
                  onChange={(e) => setEditType(e.target.value)}
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
                <label className="block text-xs text-text-secondary mb-1">Wealth (1-10)</label>
                <input
                  type="number"
                  min={1}
                  max={10}
                  value={editWealth}
                  onChange={(e) => setEditWealth(Number(e.target.value))}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">Population</label>
                <input
                  type="number"
                  min={0}
                  value={editPopulation}
                  onChange={(e) => setEditPopulation(Number(e.target.value))}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div className="flex gap-2 pt-2">
                <button
                  onClick={() => setEditing(false)}
                  className="flex-1 rounded border border-bg-elevated py-2 text-sm text-text-secondary hover:text-text-primary"
                >
                  Cancel
                </button>
                <button
                  onClick={handleEdit}
                  disabled={saving || !editName.trim()}
                  className="flex-1 rounded bg-accent py-2 text-sm text-white hover:bg-accent/80 disabled:opacity-40"
                >
                  {saving ? 'Saving…' : 'Save'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirm */}
      {showDelete && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
          onClick={() => setShowDelete(false)}
        >
          <div
            className="w-72 rounded-xl border border-bg-elevated bg-bg-surface p-5 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="font-heading text-text-primary mb-2">Delete Location?</h3>
            <p className="text-sm text-text-secondary mb-4">
              This will permanently delete this location and all associated data.
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => setShowDelete(false)}
                className="flex-1 rounded border border-bg-elevated py-2 text-sm text-text-secondary hover:text-text-primary"
              >
                Cancel
              </button>
              <button
                onClick={handleDelete}
                className="flex-1 rounded bg-danger py-2 text-sm text-white hover:bg-danger/80"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
