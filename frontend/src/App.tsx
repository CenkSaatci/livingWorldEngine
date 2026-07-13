import { useTranslation } from 'react-i18next';

/**
 * Skeleton-Root-Komponente. Vollständige Routing-Struktur folgt in Phase 3 (P3-T01 ff, siehe
 * docs/UI-UX.md Abschnitt 3 "Komponenten-Struktur").
 */
function App() {
  const { t } = useTranslation('common');
  return (
    <div className="min-h-screen bg-bg-primary text-text-primary font-body">
      <header className="p-4 bg-bg-surface border-b border-bg-elevated">
        <h1 className="text-2xl font-heading text-accent">{t('app.title')}</h1>
        <p className="text-text-secondary">{t('app.subtitle')}</p>
      </header>
      <main className="p-6 max-w-3xl">
        <p className="text-text-secondary">{t('app.skeleton_note')}</p>
      </main>
    </div>
  );
}

export default App;