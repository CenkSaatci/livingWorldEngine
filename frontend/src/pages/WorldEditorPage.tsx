import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Copy, Users } from 'lucide-react';
import { apiClient } from '../api/client';
import { type WorldSummary } from '../store/worldStore';

interface WorldDetail extends WorldSummary {
  owner_id: string;
  settings_json: string;
  game_system_id: string | null;
}

interface Member {
  id: string;
  user_id: string;
  role: string;
  joined_at: string;
}

export default function WorldEditorPage() {
  const { t } = useTranslation('common');
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [world, setWorld] = useState<WorldDetail | null>(null);
  const [members, setMembers] = useState<Member[]>([]);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!id) return;
    apiClient.get(`/worlds/${id}`).then((r) => setWorld(r.data)).catch(() => navigate('/dashboard'));
    apiClient.get(`/worlds/${id}/members`).then((r) => setMembers(r.data)).catch(() => {});
  }, [id, navigate]);

  const inviteLink = `${window.location.origin}/worlds/${id}`;

  const handleCopyLink = () => {
    navigator.clipboard.writeText(inviteLink).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  };

  if (!world) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg-primary">
        <p className="text-text-secondary">{t('status.loading')}</p>
      </div>
    );
  }

  const currentLocale = useTranslation().i18n.language;

  return (
    <div className="min-h-screen bg-bg-primary">
      <header className="flex items-center gap-4 border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <button onClick={() => navigate('/dashboard')} className="text-text-secondary hover:text-accent">
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-lg font-heading text-text-primary">{world.name}</h1>
      </header>

      <main className="mx-auto max-w-3xl space-y-6 p-6">
        {/* World Info */}
        <section className="rounded-lg bg-bg-surface p-5">
          <h2 className="mb-3 font-heading text-text-primary">World Info</h2>
          <dl className="space-y-2 text-sm">
            <div className="flex justify-between">
              <dt className="text-text-secondary">Created</dt>
              <dd className="text-text-primary">
                {new Date(world.created_at).toLocaleDateString(currentLocale)}
              </dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-text-secondary">Game Time</dt>
              <dd className="text-text-primary">
                {world.current_game_time
                  ? new Date(world.current_game_time).toLocaleString(currentLocale)
                  : '—'}
              </dd>
            </div>
          </dl>
        </section>

        {/* Invite Link */}
        <section className="rounded-lg bg-bg-surface p-5">
          <h2 className="mb-3 flex items-center gap-2 font-heading text-text-primary">
            <Users size={18} /> Invite
          </h2>
          <div className="flex gap-2">
            <input
              readOnly
              value={inviteLink}
              className="flex-1 rounded border border-bg-elevated bg-bg-primary px-3 py-2
                         text-sm text-text-secondary"
            />
            <button
              onClick={handleCopyLink}
              className="flex items-center gap-1 rounded bg-accent px-3 py-2 text-sm font-semibold
                         text-white hover:bg-accent/80"
            >
              <Copy size={16} />
              {copied ? 'Copied!' : t('actions.copy') ?? 'Copy'}
            </button>
          </div>
        </section>

        {/* Members */}
        <section className="rounded-lg bg-bg-surface p-5">
          <h2 className="mb-3 font-heading text-text-primary">
            Members ({members.length})
          </h2>
          {members.length === 0 ? (
            <p className="text-sm text-text-secondary">No members yet</p>
          ) : (
            <ul className="space-y-2">
              {members.map((m) => (
                <li key={m.id} className="flex items-center justify-between text-sm">
                  <span className="text-text-primary">{m.user_id.slice(0, 8)}…</span>
                  <span className="rounded bg-bg-elevated px-2 py-0.5 text-xs text-text-secondary">
                    {m.role}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
    </div>
  );
}
