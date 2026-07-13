import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Plus, LogOut, Globe } from 'lucide-react';
import { apiClient } from '../api/client';
import { useAuthStore } from '../store/authStore';
import { useWorldStore, type WorldSummary } from '../store/worldStore';

export default function DashboardPage() {
  const { t, i18n } = useTranslation('common');
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const setCurrentWorld = useWorldStore((s) => s.setCurrentWorld);
  const worlds = useWorldStore((s) => s.worlds);
  const setWorlds = useWorldStore((s) => s.setWorlds);

  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    apiClient.get('/worlds').then((r) => setWorlds(r.data)).catch(() => {});
  }, [setWorlds]);

  const handleCreate = async () => {
    if (!name.trim()) return;
    setLoading(true);
    setError(null);
    try {
      const res = await apiClient.post('/worlds', { name: name.trim() });
      setWorlds([res.data, ...worlds]);
      setName('');
      setShowCreate(false);
    } catch (err: any) {
      setError(err.response?.data?.error?.message ?? 'Failed to create world');
    } finally {
      setLoading(false);
    }
  };

  const handleEnter = (world: WorldSummary) => {
    setCurrentWorld(world);
    navigate(`/worlds/${world.id}`);
  };

  const currentLocale = i18n.language;

  return (
    <div className="min-h-screen bg-bg-primary">
      {/* Top Bar */}
      <header className="flex items-center justify-between border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <h1 className="text-lg font-heading text-accent">{t('app.title')}</h1>
        <div className="flex items-center gap-4">
          {/* Language Switcher */}
          <button
            onClick={() => {
              const next = currentLocale === 'de' ? 'en' : 'de';
              i18n.changeLanguage(next);
              useAuthStore.getState().setLocale(next);
            }}
            className="flex items-center gap-1 text-sm text-text-secondary hover:text-accent"
            title={t('language.switch')}
          >
            <Globe size={16} />
            {currentLocale.toUpperCase()}
          </button>

          <span className="text-sm text-text-secondary">{user?.username}</span>
          <button
            onClick={() => { logout(); navigate('/login'); }}
            className="flex items-center gap-1 text-sm text-danger hover:text-danger/80"
          >
            <LogOut size={16} /> {t('actions.logout')}
          </button>
        </div>
      </header>

      {/* Content */}
      <main className="mx-auto max-w-4xl p-6">
        <div className="mb-6 flex items-center justify-between">
          <h2 className="text-xl font-heading text-text-primary">
            {/* No specific t key — inline OK */}
            My Worlds
          </h2>
          <button
            onClick={() => setShowCreate(true)}
            className="flex items-center gap-2 rounded bg-accent px-4 py-2 text-sm font-semibold text-white hover:bg-accent/80"
          >
            <Plus size={18} /> {t('actions.create')}
          </button>
        </div>

        {/* Create Modal */}
        {showCreate && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-bg-surface p-6 shadow-xl">
              <h3 className="mb-4 text-lg font-heading text-text-primary">
                {t('actions.create')} World
              </h3>
              {error && (
                <div className="mb-3 rounded bg-danger/20 p-2 text-sm text-danger">{error}</div>
              )}
              <input
                autoFocus
                value={name}
                onChange={(e) => setName(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleCreate()}
                placeholder="World name"
                className="mb-4 w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2
                           text-text-primary placeholder:text-text-secondary/50
                           focus:border-accent focus:outline-none"
              />
              <div className="flex justify-end gap-3">
                <button
                  onClick={() => { setShowCreate(false); setName(''); setError(null); }}
                  className="rounded px-4 py-2 text-sm text-text-secondary hover:text-text-primary"
                >
                  {t('actions.cancel')}
                </button>
                <button
                  onClick={handleCreate}
                  disabled={loading || !name.trim()}
                  className="rounded bg-accent px-4 py-2 text-sm font-semibold text-white
                             hover:bg-accent/80 disabled:opacity-50"
                >
                  {loading ? t('status.loading') : t('actions.create')}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* World List */}
        {worlds.length === 0 ? (
          <div className="rounded-lg border border-dashed border-bg-elevated p-12 text-center">
            <p className="text-text-secondary">{t('app.skeleton_note')}</p>
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            {worlds.map((w) => (
              <button
                key={w.id}
                onClick={() => handleEnter(w)}
                className="rounded-lg border border-bg-elevated bg-bg-surface p-4 text-left
                           transition hover:border-accent/50 hover:bg-bg-elevated/50"
              >
                <h3 className="font-heading text-text-primary">{w.name}</h3>
                <p className="mt-1 text-xs text-text-secondary">
                  {new Date(w.created_at).toLocaleDateString(currentLocale)}
                </p>
              </button>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
