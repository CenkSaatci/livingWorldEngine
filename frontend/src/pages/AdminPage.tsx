import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Users, Globe, Activity } from 'lucide-react';
import { useApiGet } from '../hooks/useApiGet';
import { useAuthStore } from '../store/authStore';

interface AdminUser {
  id: string;
  username: string;
  email: string;
  role: string;
}

interface AdminStats {
  total_worlds: number;
  active_worlds: number;
}

export default function AdminPage() {
  const { t } = useTranslation('common');
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const { data: usersData } = useApiGet<AdminUser[]>('/admin/users', []);
  const { data: stats } = useApiGet<AdminStats>('/admin/worlds/stats', []);
  const users = usersData ?? [];

  if (user?.role !== 'ADMIN') {
    return <div className="p-6 text-text-secondary">{t('admin.accessDenied')}</div>;
  }

  return (
    <div className="min-h-screen bg-bg-primary">
      <header className="flex items-center gap-3 border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <button
          onClick={() => navigate('/dashboard')}
          className="text-text-secondary hover:text-accent"
        >
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-lg font-heading text-text-primary">{t('admin.title')}</h1>
      </header>

      <main className="mx-auto max-w-4xl space-y-6 p-6">
        {/* Stats */}
        {stats && (
          <section className="grid grid-cols-2 gap-4 sm:grid-cols-3">
            <div className="rounded-lg bg-bg-surface p-4">
              <Globe size={20} className="text-accent mb-1" />
              <p className="text-2xl font-heading text-text-primary">{stats.total_worlds}</p>
              <p className="text-xs text-text-secondary">{t('admin.totalWorlds')}</p>
            </div>
            <div className="rounded-lg bg-bg-surface p-4">
              <Activity size={20} className="text-accent mb-1" />
              <p className="text-2xl font-heading text-text-primary">{stats.active_worlds}</p>
              <p className="text-xs text-text-secondary">{t('admin.activeWorlds')}</p>
            </div>
            <div className="rounded-lg bg-bg-surface p-4">
              <Users size={20} className="text-accent mb-1" />
              <p className="text-2xl font-heading text-text-primary">{users.length}</p>
              <p className="text-xs text-text-secondary">{t('admin.users')}</p>
            </div>
          </section>
        )}

        {/* Users */}
        <section className="rounded-lg bg-bg-surface p-5">
          <h2 className="mb-4 font-heading text-text-primary">Users ({users.length})</h2>
          <div className="space-y-2">
            {users.map((u) => (
              <div
                key={u.id}
                className="flex items-center justify-between rounded bg-bg-primary/50 px-3 py-2"
              >
                <div>
                  <span className="text-sm text-text-primary">{u.username}</span>
                  <span className="ml-2 text-xs text-text-secondary">{u.email}</span>
                </div>
                <span className="rounded bg-bg-elevated px-2 py-0.5 text-xs text-text-secondary">
                  {u.role}
                </span>
              </div>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
}
