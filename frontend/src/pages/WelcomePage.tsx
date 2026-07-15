import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { CheckCircle, Circle, Globe, Plus, Swords } from 'lucide-react';
import { apiClient } from '../api/client';

interface Step {
  key: string;
  icon: typeof Globe;
  title: string;
  desc: string;
  action: () => void | Promise<void>;
}

export default function WelcomePage() {
  const { t } = useTranslation('common');
  const navigate = useNavigate();
  const [worldCreated, setWorldCreated] = useState(false);
  const [worldName, setWorldName] = useState('');
  const [creating, setCreating] = useState(false);

  const steps: Step[] = [
    {
      key: 'world',
      icon: Globe,
      title: t('welcome.stepWorld'),
      desc: t('welcome.stepWorldDesc'),
      action: async () => {
        if (!worldName.trim()) return;
        setCreating(true);
        try {
          await apiClient.post('/worlds', { name: worldName.trim() });
          setWorldCreated(true);
          setWorldName('');
        } catch {
          /* ignore */
        } finally {
          setCreating(false);
        }
      },
    },
    {
      key: 'npc',
      icon: Plus,
      title: t('welcome.stepNpc'),
      desc: t('welcome.stepNpcDesc'),
      action: () => navigate('/dashboard'),
    },
    {
      key: 'combat',
      icon: Swords,
      title: t('welcome.stepCombat'),
      desc: t('welcome.stepCombatDesc'),
      action: () => navigate('/dashboard'),
    },
  ];

  return (
    <div className="min-h-screen bg-bg-primary">
      <div className="mx-auto max-w-2xl px-4 py-16">
        <div className="mb-12 text-center">
          <h1 className="mb-2 text-3xl font-heading text-text-primary">{t('welcome.title')}</h1>
          <p className="text-text-secondary">{t('welcome.subtitle')}</p>
        </div>

        <div className="space-y-6">
          {steps.map((step, idx) => {
            const isActive = idx === 0 ? !worldCreated : worldCreated;
            const done = idx === 0 ? worldCreated : false;

            return (
              <div
                key={step.key}
                className={`rounded-lg border p-5 transition ${
                  done
                    ? 'border-success/30 bg-success/5'
                    : isActive
                      ? 'border-accent/30 bg-accent/5'
                      : 'border-bg-elevated bg-bg-surface opacity-50'
                }`}
              >
                <div className="flex items-start gap-4">
                  <div className="mt-0.5">
                    {done ? (
                      <CheckCircle size={20} className="text-success" />
                    ) : (
                      <Circle
                        size={20}
                        className={isActive ? 'text-accent' : 'text-text-secondary'}
                      />
                    )}
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <step.icon size={16} className="text-text-secondary" />
                      <h3 className="font-heading text-text-primary">{step.title}</h3>
                    </div>
                    <p className="mt-1 text-sm text-text-secondary">{step.desc}</p>

                    {isActive && !done && idx === 0 && (
                      <div className="mt-3 flex gap-2">
                        <input
                          value={worldName}
                          onChange={(e) => setWorldName(e.target.value)}
                          onKeyDown={(e) => e.key === 'Enter' && step.action()}
                          placeholder={t('welcome.worldNamePlaceholder')}
                          className="flex-1 rounded border border-bg-elevated bg-bg-primary px-3 py-1.5 text-sm text-text-primary outline-none focus:border-accent"
                          autoFocus
                        />
                        <button
                          onClick={step.action}
                          disabled={creating || !worldName.trim()}
                          className="rounded bg-accent px-4 py-1.5 text-sm text-white hover:bg-accent/80 disabled:opacity-40"
                        >
                          {creating ? t('status.loading') : t('welcome.create')}
                        </button>
                      </div>
                    )}

                    {done && <p className="mt-1 text-xs text-success">{t('welcome.done')}</p>}
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        <div className="mt-10 text-center">
          <button
            onClick={() => navigate('/dashboard')}
            className="rounded bg-accent px-6 py-2 text-sm text-white hover:bg-accent/80"
          >
            {t('welcome.start')}
          </button>
        </div>
      </div>
    </div>
  );
}
